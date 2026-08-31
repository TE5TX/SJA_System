package com.sja.system.service;

import com.sja.system.async.AsyncTaskEnvelope;
import com.sja.system.common.ApiException;
import com.sja.system.config.AsyncTaskProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 固定并发数的 Redis Stream 消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sja.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTaskWorker {

    private final AsyncTaskQueueService queueService;
    private final AsyncTaskExecutionService executionService;
    private final AsyncTaskProperties properties;

    @Qualifier("asyncTaskWorkerExecutor")
    private final ThreadPoolTaskExecutor workerExecutor;

    public void accept(MapRecord<String, String, String> record) {
        // CallerRunsPolicy 会在工作队列已满时阻塞 Redis 拉取线程，从源头形成背压。
        workerExecutor.execute(() -> process(record));
    }

    private void process(MapRecord<String, String, String> record) {
        AsyncTaskEnvelope envelope;
        try {
            envelope = queueService.fromRecord(record);
        } catch (Exception e) {
            log.error("Discarding malformed async task record: recordId={}", record.getId(), e);
            queueService.acknowledgeAndDelete(record);
            return;
        }

        if (queueService.isTerminal(envelope.getTaskId())) {
            queueService.acknowledgeAndDelete(record);
            return;
        }
        String lockToken = queueService.acquireProcessingLock(envelope.getTaskId());
        if (lockToken == null) {
            return;
        }

        boolean businessCommitted = false;
        try {
            queueService.markProcessing(envelope);
            executionService.execute(envelope);
            businessCommitted = true;
            queueService.markSuccess(envelope);
            queueService.acknowledgeAndDelete(record);
            log.info("Async task completed: taskId={}, type={}, attempt={}",
                    envelope.getTaskId(), envelope.getType(), envelope.getAttempt());
        } catch (ApiException e) {
            // 业务校验错误重试不会成功，直接返回可理解的失败原因。
            queueService.markFailed(envelope, e.getMessage());
            queueService.acknowledgeAndDelete(record);
            log.warn("Async task rejected: taskId={}, type={}, reason={}",
                    envelope.getTaskId(), envelope.getType(), e.getMessage());
        } catch (Exception e) {
            handleInfrastructureFailure(record, envelope, businessCommitted, e);
        } finally {
            try {
                queueService.releaseProcessingLock(envelope.getTaskId(), lockToken);
            } catch (Exception e) {
                log.warn("Failed to release async task lock: taskId={}", envelope.getTaskId(), e);
            }
        }
    }

    private void handleInfrastructureFailure(MapRecord<String, String, String> record,
                                             AsyncTaskEnvelope envelope,
                                             boolean businessCommitted,
                                             Exception error) {
        log.error("Async task processing failed: taskId={}, type={}, attempt={}",
                envelope.getTaskId(), envelope.getType(), envelope.getAttempt(), error);
        try {
            if (businessCommitted) {
                // 数据库已提交但 Redis 状态更新失败时保留 pending，稍后通过数据库幂等标记恢复。
                return;
            }
            if (envelope.getAttempt() < properties.getMaxRetries()) {
                queueService.requeue(envelope);
                queueService.acknowledgeAndDelete(record);
                return;
            }
            queueService.markFailed(envelope, "系统繁忙，任务处理失败，请重新提交");
            queueService.acknowledgeAndDelete(record);
        } catch (Exception recoveryError) {
            // 不确认原消息，让定时 pending 恢复机制在 Redis/数据库恢复后继续处理。
            log.error("Async task recovery failed: taskId={}", envelope.getTaskId(), recoveryError);
        }
    }

    @Scheduled(
            fixedDelayString = "${sja.async-task.recovery-interval-ms:30000}",
            initialDelayString = "${sja.async-task.recovery-initial-delay-ms:30000}")
    public void recoverIdleTasks() {
        try {
            List<MapRecord<String, String, String>> records = queueService.claimIdleTasks();
            if (!records.isEmpty()) {
                log.warn("Recovering {} idle async task(s)", records.size());
                records.forEach(this::accept);
            }
        } catch (Exception e) {
            log.error("Failed to recover idle async tasks", e);
        }
    }
}
