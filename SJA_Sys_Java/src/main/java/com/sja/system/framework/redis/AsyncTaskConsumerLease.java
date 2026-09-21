package com.sja.system.framework.redis;

import com.sja.system.service.async.model.AsyncTaskConsumerIdentity;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用 Redis 租约保证同一消费者组中不会同时运行两个同名实例。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sja.async-task", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTaskConsumerLease implements InitializingBean {

    private static final String LEASE_PREFIX = "sja:async:consumer-lease:";
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
            Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AsyncTaskConsumerIdentity identity;
    private final ConfigurableApplicationContext applicationContext;
    private final String consumerGroup;
    private final Duration leaseTtl;
    private final Duration leaseRenewInterval;
    private final AtomicBoolean active = new AtomicBoolean(false);

    public AsyncTaskConsumerLease(StringRedisTemplate redisTemplate,
                                  AsyncTaskConsumerIdentity identity,
                                  ConfigurableApplicationContext applicationContext,
                                  @Value("${sja.async-task.consumer-group:sja-write-workers}") String consumerGroup,
                                  @Value("${sja.async-task.consumer-lease-ttl:90s}") Duration leaseTtl,
                                  @Value("${sja.async-task.consumer-lease-renew-interval:30s}")
                                  Duration leaseRenewInterval) {
        this.redisTemplate = redisTemplate;
        this.identity = identity;
        this.applicationContext = applicationContext;
        this.consumerGroup = consumerGroup;
        this.leaseTtl = leaseTtl;
        this.leaseRenewInterval = leaseRenewInterval;
    }

    @Override
    public void afterPropertiesSet() {
        validateIntervals();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                leaseKey(), identity.instanceId(), leaseTtl);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalStateException("Redis Stream消费者名称已被其他实例占用: " + identity.name());
        }
        active.set(true);
        // 202609201556 - 增加了Redis租约，防止多实例误用相同消费者名称。
        log.info("Acquired Redis Stream consumer lease: group={}, consumer={}",
                consumerGroup, identity.name());
    }

    @Scheduled(
            fixedDelayString = "${sja.async-task.consumer-lease-renew-interval:30s}",
            initialDelayString = "${sja.async-task.consumer-lease-renew-interval:30s}")
    public void renew() {
        if (!active.get()) {
            return;
        }
        try {
            Long renewed = redisTemplate.execute(RENEW_SCRIPT,
                    Collections.singletonList(leaseKey()), identity.instanceId(),
                    String.valueOf(leaseTtl.toMillis()));
            if (Long.valueOf(1L).equals(renewed)) {
                return;
            }
            Boolean reacquired = redisTemplate.opsForValue().setIfAbsent(
                    leaseKey(), identity.instanceId(), leaseTtl);
            if (!Boolean.TRUE.equals(reacquired)) {
                stopForLeaseConflict();
            } else {
                log.warn("Redis Stream consumer lease expired and was reacquired: consumer={}", identity.name());
            }
        } catch (Exception error) {
            // Redis 故障期间 Stream 本身也不可用；恢复连接后再次校验租约所有权。
            log.error("Failed to renew Redis Stream consumer lease: consumer={}", identity.name(), error);
        }
    }

    @PreDestroy
    public void release() {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        try {
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(leaseKey()), identity.instanceId());
            log.info("Released Redis Stream consumer lease: consumer={}", identity.name());
        } catch (Exception error) {
            log.warn("Failed to release Redis Stream consumer lease; it will expire automatically: consumer={}",
                    identity.name(), error);
        }
    }

    private void validateIntervals() {
        if (leaseTtl == null || leaseTtl.isZero() || leaseTtl.isNegative()) {
            throw new IllegalStateException("消费者租约有效期必须大于0");
        }
        if (leaseRenewInterval == null || leaseRenewInterval.isZero() || leaseRenewInterval.isNegative()
                || leaseRenewInterval.compareTo(leaseTtl) >= 0) {
            throw new IllegalStateException("消费者租约续期间隔必须大于0且小于租约有效期");
        }
    }

    private String leaseKey() {
        return LEASE_PREFIX + consumerGroup + ":" + identity.name();
    }

    private void stopForLeaseConflict() {
        if (!active.compareAndSet(true, false)) {
            return;
        }
        log.error("Redis Stream consumer lease is owned by another instance; stopping application: consumer={}",
                identity.name());
        applicationContext.close();
    }
}
