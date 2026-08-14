package com.sja.system.service;

import com.sja.system.common.ApiException;
import com.sja.system.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginThrottleServiceTest {

    private StringRedisTemplate redisTemplate;
    private LoginThrottleService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        service = new LoginThrottleService(redisTemplate, "sja:test:login",
                10, 30, 300, 5, 20, 900, 900);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void allowsRequestAndDoesNotExposeAccountOrIpInRedisKeys() {
        doReturn(0L).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));

        service.assertAllowed("2026000000001", "203.0.113.8");

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass((Class) List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(), any(Object[].class));
        assertEquals(2, keys.getValue().size());
        assertTrue(keys.getValue().stream().allMatch(key -> key.startsWith("sja:test:login:lock:")));
        assertFalse(keys.getValue().stream().anyMatch(key ->
                key.contains("2026000000001") || key.contains("203.0.113.8")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returns429AndRetryAfterWhenLocked() {
        doReturn(73L).when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.assertAllowed("user", "203.0.113.8"));

        assertEquals(Constants.TOO_MANY_REQUESTS, exception.getStatus());
        assertEquals(73L, exception.getRetryAfterSeconds());
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsClosedWhenRedisIsUnavailable() {
        doThrow(new RedisConnectionFailureException("down"))
                .when(redisTemplate).execute(any(RedisScript.class), anyList(), any(Object[].class));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.recordIpAttempt("203.0.113.8"));

        assertEquals(Constants.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void successfulLoginClearsOnlyTheAccountFailureCounter() {
        service.recordSuccess("USER", "203.0.113.8");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(key.capture());
        assertTrue(key.getValue().startsWith("sja:test:login:failure:account:"));
        assertFalse(key.getValue().contains("USER"));
    }
}
