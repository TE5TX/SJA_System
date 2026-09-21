package com.sja.system.service.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.service.async.model.AsyncTaskEnvelope;
import com.sja.system.service.async.model.AsyncTaskConsumerIdentity;
import com.sja.system.service.async.model.AsyncTaskStatus;
import com.sja.system.service.async.model.AsyncTaskType;
import com.sja.system.common.ApiException;
import com.sja.system.framework.config.AsyncTaskProperties;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.application.dto.user.ProfileUpdateDTO;
import com.sja.system.application.vo.async.AsyncTaskAcceptedVO;
import com.sja.system.application.vo.async.AsyncTaskStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Redis Stream 写入队列。
 *
 * 202608192148 - 修复了高并发写入直接占满数据库连接的问题：请求只负责校验和入队，
 * 数据库写入由固定大小的后台线程池处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskQueueService {

    private static final String KEY_PREFIX = "sja:async:task:";
    private static final String DEDUPE_PREFIX = "sja:async:dedupe:";
    private static final String ACTIVE_PROFILE_PREFIX = "sja:async:active:profile:";
    private static final String PROCESSING_LOCK_PREFIX = "sja:async:processing:";
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AsyncTaskPayloadCipher payloadCipher;
    private final AsyncTaskProperties properties;
    private final AsyncTaskConsumerIdentity consumerIdentity;

    public AsyncTaskAcceptedVO enqueueProfile(Long userId,
                                               ProfileUpdateDTO dto,
                                               String idempotencyKey) {
        return enqueue(AsyncTaskType.PROFILE_UPDATE, userId, dto, idempotencyKey, true);
    }

    public AsyncTaskAcceptedVO enqueueFee(Long userId,
                                           FeeSubmitDTO dto,
                                           String idempotencyKey) {
        return enqueue(AsyncTaskType.FEE_SUBMIT, userId, dto, idempotencyKey, false);
    }

    private AsyncTaskAcceptedVO enqueue(AsyncTaskType type,
                                        Long ownerId,
                                        Object body,
                                        String requestedIdempotencyKey,
                                        boolean singleActivePerOwner) {
        String idempotencyKey = normalizeIdempotencyKey(requestedIdempotencyKey);
        String dedupeKey = dedupeKey(type, ownerId, idempotencyKey);

        try {
            String duplicateTaskId = redisTemplate.opsForValue().get(dedupeKey);
            if (StringUtils.hasText(duplicateTaskId) && statusExists(duplicateTaskId)) {
                return accepted(duplicateTaskId);
            }

            Long queueSize = redisTemplate.opsForStream().size(properties.getStreamKey());
            if (queueSize != null && queueSize >= properties.getQueueCapacity()) {
                throw ApiException.serviceUnavailable("系统写入队列已满，请稍后重试");
            }

            String taskId = UUID.randomUUID().toString();
            String activeKey = singleActivePerOwner ? activeProfileKey(ownerId) : null;
            if (activeKey != null) {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(activeKey, taskId, properties.getStatusTtl());
                if (!Boolean.TRUE.equals(acquired)) {
                    String activeTaskId = redisTemplate.opsForValue().get(activeKey);
                    if (StringUtils.hasText(activeTaskId) && statusExists(activeTaskId)) {
                        return accepted(activeTaskId);
                    }
                    redisTemplate.delete(activeKey);
                    acquired = redisTemplate.opsForValue()
                            .setIfAbsent(activeKey, taskId, properties.getStatusTtl());
                    if (!Boolean.TRUE.equals(acquired)) {
                        throw ApiException.conflict("已有个人资料修改任务正在处理，请勿重复提交");
                    }
                }
            }

            Boolean dedupeAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(dedupeKey, taskId, properties.getStatusTtl());
            if (!Boolean.TRUE.equals(dedupeAcquired)) {
                cleanupActiveKey(activeKey, taskId);
                String existingTaskId = redisTemplate.opsForValue().get(dedupeKey);
                if (StringUtils.hasText(existingTaskId) && statusExists(existingTaskId)) {
                    return accepted(existingTaskId);
                }
                throw ApiException.conflict("相同请求正在处理，请勿重复提交");
            }

            String now = Instant.now().toString();
            try {
                String plaintext = objectMapper.writeValueAsString(body);
                AsyncTaskEnvelope envelope = AsyncTaskEnvelope.builder()
                        .taskId(taskId)
                        .type(type)
                        .ownerId(ownerId)
                        .payload(payloadCipher.encrypt(plaintext, cipherContext(taskId, type)))
                        .idempotencyKey(idempotencyKey)
                        .attempt(0)
                        .createdAt(now)
                        .build();

                writeStatus(envelope, AsyncTaskStatus.PENDING, "任务已进入处理队列");
                append(envelope);
                log.info("Async task accepted: taskId={}, type={}, ownerId={}", taskId, type, ownerId);
                return accepted(taskId);
            } catch (JsonProcessingException e) {
                redisTemplate.delete(taskKey(taskId));
                redisTemplate.delete(dedupeKey);
                cleanupActiveKey(activeKey, taskId);
                throw new IllegalStateException("异步任务序列化失败", e);
            } catch (RuntimeException e) {
                redisTemplate.delete(taskKey(taskId));
                redisTemplate.delete(dedupeKey);
                cleanupActiveKey(activeKey, taskId);
                throw e;
            }
        } catch (RedisConnectionFailureException e) {
            throw ApiException.serviceUnavailable("异步任务队列暂不可用，请稍后重试");
        } catch (DataAccessException e) {
            log.error("Async task enqueue failed: type={}, ownerId={}", type, ownerId, e);
            throw ApiException.serviceUnavailable("异步任务队列暂不可用，请稍后重试");
        }
    }

    public AsyncTaskStatusVO getStatus(String taskId, String statusToken) {
        validateTaskId(taskId);
        String expectedToken = payloadCipher.deriveStatusToken(taskId);
        if (!StringUtils.hasText(statusToken) || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                statusToken.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.forbidden("任务状态令牌无效");
        }
        Map<Object, Object> values = redisTemplate.opsForHash().entries(taskKey(taskId));
        if (values.isEmpty()) {
            throw ApiException.notFound("任务不存在或状态已过期");
        }
        return toStatusVO(values);
    }

    public void initializeConsumerGroup() {
        String bootstrapId = null;
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(properties.getStreamKey()))) {
                RecordId bootstrap = redisTemplate.opsForStream().add(properties.getStreamKey(),
                        Map.of("bootstrap", "1"));
                bootstrapId = bootstrap != null ? bootstrap.getValue() : null;
            }
            redisTemplate.opsForStream().createGroup(properties.getStreamKey(),
                    ReadOffset.from("0-0"), properties.getConsumerGroup());
            log.info("Created Redis Stream consumer group: {}", properties.getConsumerGroup());
        } catch (RedisSystemException e) {
            if (!containsCauseMessage(e, "BUSYGROUP")) {
                throw e;
            }
        } finally {
            if (bootstrapId != null) {
                redisTemplate.opsForStream().delete(properties.getStreamKey(), bootstrapId);
            }
        }
    }

    public AsyncTaskEnvelope fromRecord(MapRecord<String, String, String> record) {
        Map<String, String> values = record.getValue();
        return AsyncTaskEnvelope.builder()
                .taskId(values.get("taskId"))
                .type(AsyncTaskType.valueOf(values.get("type")))
                .ownerId(Long.valueOf(values.get("ownerId")))
                .payload(values.get("payload"))
                .idempotencyKey(values.get("idempotencyKey"))
                .attempt(Integer.valueOf(values.getOrDefault("attempt", "0")))
                .createdAt(values.get("createdAt"))
                .build();
    }

    public <T> T decryptPayload(AsyncTaskEnvelope envelope, Class<T> payloadType) {
        try {
            String json = payloadCipher.decrypt(envelope.getPayload(),
                    cipherContext(envelope.getTaskId(), envelope.getType()));
            return objectMapper.readValue(json, payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("异步任务正文格式不正确", e);
        }
    }

    public String acquireProcessingLock(String taskId) {
        String lockToken = properties.getConsumerName() + ":" + UUID.randomUUID();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                PROCESSING_LOCK_PREFIX + taskId,
                lockToken,
                properties.getProcessingLockTtl());
        return Boolean.TRUE.equals(acquired) ? lockToken : null;
    }

    public void releaseProcessingLock(String taskId, String lockToken) {
        // 仅锁持有者可以删除，避免旧消费者超时后误删新消费者持有的锁。
        redisTemplate.execute(RELEASE_LOCK_SCRIPT,
                List.of(PROCESSING_LOCK_PREFIX + taskId), lockToken);
    }

    public void markProcessing(AsyncTaskEnvelope envelope) {
        updateStatus(envelope.getTaskId(), AsyncTaskStatus.PROCESSING,
                "任务正在处理", envelope.getAttempt());
    }

    public void markSuccess(AsyncTaskEnvelope envelope) {
        updateStatus(envelope.getTaskId(), AsyncTaskStatus.SUCCESS,
                "任务处理成功", envelope.getAttempt());
        cleanupActiveTask(envelope);
    }

    public void markFailed(AsyncTaskEnvelope envelope, String message) {
        updateStatus(envelope.getTaskId(), AsyncTaskStatus.FAILED,
                safeFailureMessage(message), envelope.getAttempt());
        cleanupActiveTask(envelope);
    }

    public void requeue(AsyncTaskEnvelope envelope) {
        envelope.setAttempt(envelope.getAttempt() + 1);
        updateStatus(envelope.getTaskId(), AsyncTaskStatus.PENDING,
                "任务处理暂时失败，正在自动重试", envelope.getAttempt());
        append(envelope);
    }

    public void acknowledgeAndDelete(MapRecord<String, String, String> record) {
        redisTemplate.opsForStream().acknowledge(
                properties.getStreamKey(), properties.getConsumerGroup(), record.getId());
        redisTemplate.opsForStream().delete(properties.getStreamKey(), record.getId());
    }

    public List<MapRecord<String, String, String>> claimIdleTasks() {
        PendingMessages pending = redisTemplate.opsForStream().pending(
                properties.getStreamKey(), properties.getConsumerGroup(), Range.unbounded(), 100);
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }

        List<RecordId> staleIds = new ArrayList<>();
        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(properties.getVisibilityTimeout()) >= 0) {
                staleIds.add(message.getId());
            }
        }
        if (staleIds.isEmpty()) {
            return List.of();
        }
        List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream().claim(
                properties.getStreamKey(),
                properties.getConsumerGroup(),
                consumerIdentity.name(),
                properties.getVisibilityTimeout(),
                staleIds.toArray(new RecordId[0]));
        List<MapRecord<String, String, String>> result = new ArrayList<>(claimed.size());
        for (MapRecord<String, Object, Object> record : claimed) {
            Map<String, String> values = new HashMap<>();
            record.getValue().forEach((key, value) -> values.put(String.valueOf(key), String.valueOf(value)));
            result.add(MapRecord.create(record.getStream(), values).withId(record.getId()));
        }
        return result;
    }

    public boolean isTerminal(String taskId) {
        Object status = redisTemplate.opsForHash().get(taskKey(taskId), "status");
        return AsyncTaskStatus.SUCCESS.name().equals(status) || AsyncTaskStatus.FAILED.name().equals(status);
    }

    private void append(AsyncTaskEnvelope envelope) {
        Map<String, String> values = new HashMap<>();
        values.put("taskId", envelope.getTaskId());
        values.put("type", envelope.getType().name());
        values.put("ownerId", String.valueOf(envelope.getOwnerId()));
        values.put("payload", envelope.getPayload());
        values.put("idempotencyKey", envelope.getIdempotencyKey());
        values.put("attempt", String.valueOf(envelope.getAttempt()));
        values.put("createdAt", envelope.getCreatedAt());
        redisTemplate.opsForStream().add(StreamRecords.mapBacked(values)
                .withStreamKey(properties.getStreamKey()));
    }

    private void writeStatus(AsyncTaskEnvelope envelope, AsyncTaskStatus status, String message) {
        Map<String, String> values = new HashMap<>();
        values.put("taskId", envelope.getTaskId());
        values.put("type", envelope.getType().name());
        values.put("ownerId", String.valueOf(envelope.getOwnerId()));
        values.put("status", status.name());
        values.put("message", message);
        values.put("attempt", String.valueOf(envelope.getAttempt()));
        values.put("createdAt", envelope.getCreatedAt());
        values.put("updatedAt", Instant.now().toString());
        redisTemplate.opsForHash().putAll(taskKey(envelope.getTaskId()), values);
        redisTemplate.expire(taskKey(envelope.getTaskId()), properties.getStatusTtl());
    }

    private void updateStatus(String taskId, AsyncTaskStatus status, String message, Integer attempt) {
        String key = taskKey(taskId);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return;
        }
        redisTemplate.opsForHash().put(key, "status", status.name());
        redisTemplate.opsForHash().put(key, "message", message);
        redisTemplate.opsForHash().put(key, "attempt", String.valueOf(attempt));
        redisTemplate.opsForHash().put(key, "updatedAt", Instant.now().toString());
        redisTemplate.expire(key, properties.getStatusTtl());
    }

    private AsyncTaskAcceptedVO accepted(String taskId) {
        Object status = redisTemplate.opsForHash().get(taskKey(taskId), "status");
        return new AsyncTaskAcceptedVO(taskId,
                status != null ? status.toString() : AsyncTaskStatus.PENDING.name(),
                "/api/v1/async-tasks/" + taskId,
                payloadCipher.deriveStatusToken(taskId));
    }

    private AsyncTaskStatusVO toStatusVO(Map<Object, Object> values) {
        AsyncTaskStatusVO vo = new AsyncTaskStatusVO();
        vo.setTaskId(stringValue(values.get("taskId")));
        vo.setType(stringValue(values.get("type")));
        vo.setStatus(stringValue(values.get("status")));
        vo.setMessage(stringValue(values.get("message")));
        vo.setAttempt(parseNullableInteger(stringValue(values.get("attempt"))));
        vo.setCreatedAt(stringValue(values.get("createdAt")));
        vo.setUpdatedAt(stringValue(values.get("updatedAt")));
        return vo;
    }

    private void cleanupActiveTask(AsyncTaskEnvelope envelope) {
        if (envelope.getType() == AsyncTaskType.PROFILE_UPDATE) {
            cleanupActiveKey(activeProfileKey(envelope.getOwnerId()), envelope.getTaskId());
        }
    }

    private void cleanupActiveKey(String activeKey, String taskId) {
        if (activeKey == null) {
            return;
        }
        String activeTaskId = redisTemplate.opsForValue().get(activeKey);
        if (taskId.equals(activeTaskId)) {
            redisTemplate.delete(activeKey);
        }
    }

    private String normalizeIdempotencyKey(String value) {
        if (!StringUtils.hasText(value)) {
            return UUID.randomUUID().toString();
        }
        String normalized = value.trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(normalized).matches()) {
            throw ApiException.badRequest("幂等键格式不正确");
        }
        return normalized;
    }

    private void validateTaskId(String taskId) {
        try {
            UUID.fromString(taskId);
        } catch (Exception e) {
            throw ApiException.badRequest("任务ID格式不正确");
        }
    }

    private boolean statusExists(String taskId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(taskKey(taskId)));
    }

    private String dedupeKey(AsyncTaskType type, Long ownerId, String idempotencyKey) {
        return DEDUPE_PREFIX + type.name() + ":" + ownerId + ":" + idempotencyKey;
    }

    private String activeProfileKey(Long ownerId) {
        return ACTIVE_PROFILE_PREFIX + ownerId;
    }

    private String taskKey(String taskId) {
        return KEY_PREFIX + taskId;
    }

    private String cipherContext(String taskId, AsyncTaskType type) {
        return taskId + "|" + type.name();
    }

    private String safeFailureMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "任务处理失败，请重新提交";
        }
        String normalized = message.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }

    private Integer parseNullableInteger(String value) {
        return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean containsCauseMessage(Throwable error, String expectedText) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expectedText)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
