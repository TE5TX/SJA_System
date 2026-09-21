package com.sja.system.infrastructure.redis;

import com.sja.system.async.AsyncTaskConsumerIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncTaskConsumerLeaseTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ConfigurableApplicationContext applicationContext;
    private AsyncTaskConsumerIdentity identity;
    private Duration leaseTtl;
    private Duration renewInterval;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        applicationContext = mock(ConfigurableApplicationContext.class);
        leaseTtl = Duration.ofSeconds(90);
        renewInterval = Duration.ofSeconds(30);
        identity = new AsyncTaskConsumerIdentity("worker-1", "instance-1", "configuration");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void acquiresLeaseBeforeConsumerStarts() {
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
        AsyncTaskConsumerLease lease = lease();

        lease.afterPropertiesSet();

        // 202609201556 - 验证同名消费者检测在Stream监听器启动前完成。
        verify(valueOperations).setIfAbsent(
                "sja:async:consumer-lease:test-group:worker-1", "instance-1", Duration.ofSeconds(90));
    }

    @Test
    void rejectsConsumerNameAlreadyOwnedByAnotherInstance() {
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> lease().afterPropertiesSet());
    }

    @Test
    void rejectsRenewIntervalNotShorterThanLeaseTtl() {
        renewInterval = Duration.ofSeconds(90);

        assertThrows(IllegalStateException.class, () -> lease().afterPropertiesSet());
    }

    @Test
    void closesApplicationWhenLeaseIsOwnedByAnotherInstance() {
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class)))
                .thenReturn(true, false);
        when(redisTemplate.execute(any(), anyList(), any(), any())).thenReturn(0L);
        AsyncTaskConsumerLease lease = lease();
        lease.afterPropertiesSet();

        lease.renew();

        verify(applicationContext).close();
    }

    private AsyncTaskConsumerLease lease() {
        return new AsyncTaskConsumerLease(redisTemplate, identity, applicationContext,
                "test-group", leaseTtl, renewInterval);
    }
}
