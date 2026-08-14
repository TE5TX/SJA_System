package com.sja.system.service;

import com.sja.system.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Distributed login throttling backed by Redis. Account identifiers and IP
 * addresses are hashed before being used in Redis keys.
 */
// 202608140858 - 增加了账号/IP 双维度登录限流、失败计数和短期锁定。
@Slf4j
@Service
public class LoginThrottleService {

    private static final DefaultRedisScript<Long> CHECK_LOCK_SCRIPT = new DefaultRedisScript<>("""
            local accountTtl = redis.call('TTL', KEYS[1])
            local ipTtl = redis.call('TTL', KEYS[2])
            if accountTtl < 0 then accountTtl = 0 end
            if ipTtl < 0 then ipTtl = 0 end
            if accountTtl > ipTtl then return accountTtl end
            return ipTtl
            """, Long.class);

    private static final DefaultRedisScript<Long> RECORD_ATTEMPT_SCRIPT = new DefaultRedisScript<>("""
            local lockTtl = redis.call('TTL', KEYS[2])
            if lockTtl > 0 then return lockTtl end

            local attempts = redis.call('INCR', KEYS[1])
            if attempts == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            if attempts > tonumber(ARGV[1]) then
                redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])
                return tonumber(ARGV[3])
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>("""
            local accountFailures = redis.call('INCR', KEYS[1])
            if accountFailures == 1 then redis.call('EXPIRE', KEYS[1], ARGV[3]) end
            local ipFailures = redis.call('INCR', KEYS[2])
            if ipFailures == 1 then redis.call('EXPIRE', KEYS[2], ARGV[3]) end

            if accountFailures >= tonumber(ARGV[1]) then
                redis.call('SET', KEYS[3], '1', 'EX', ARGV[4])
            end
            if ipFailures >= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[4], '1', 'EX', ARGV[4])
            end

            local accountLockTtl = redis.call('TTL', KEYS[3])
            local ipLockTtl = redis.call('TTL', KEYS[4])
            if accountLockTtl < 0 then accountLockTtl = 0 end
            if ipLockTtl < 0 then ipLockTtl = 0 end
            if accountLockTtl > ipLockTtl then return accountLockTtl end
            return ipLockTtl
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final int accountAttemptLimit;
    private final int ipAttemptLimit;
    private final int attemptWindowSeconds;
    private final int accountFailureLimit;
    private final int ipFailureLimit;
    private final int failureWindowSeconds;
    private final int lockSeconds;

    public LoginThrottleService(
            StringRedisTemplate redisTemplate,
            @Value("${sja.security.login-throttle.key-prefix:sja:security:login}") String keyPrefix,
            @Value("${sja.security.login-throttle.account-attempt-limit:10}") int accountAttemptLimit,
            @Value("${sja.security.login-throttle.ip-attempt-limit:30}") int ipAttemptLimit,
            @Value("${sja.security.login-throttle.attempt-window-seconds:300}") int attemptWindowSeconds,
            @Value("${sja.security.login-throttle.account-failure-limit:5}") int accountFailureLimit,
            @Value("${sja.security.login-throttle.ip-failure-limit:20}") int ipFailureLimit,
            @Value("${sja.security.login-throttle.failure-window-seconds:900}") int failureWindowSeconds,
            @Value("${sja.security.login-throttle.lock-seconds:900}") int lockSeconds) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = requireText(keyPrefix, "登录限流 Redis 前缀不能为空");
        this.accountAttemptLimit = requirePositive(accountAttemptLimit, "账号尝试上限");
        this.ipAttemptLimit = requirePositive(ipAttemptLimit, "IP 尝试上限");
        this.attemptWindowSeconds = requirePositive(attemptWindowSeconds, "尝试统计窗口");
        this.accountFailureLimit = requirePositive(accountFailureLimit, "账号失败上限");
        this.ipFailureLimit = requirePositive(ipFailureLimit, "IP 失败上限");
        this.failureWindowSeconds = requirePositive(failureWindowSeconds, "失败统计窗口");
        this.lockSeconds = requirePositive(lockSeconds, "登录锁定时长");
    }

    /** Rejects an already locked account or IP without consuming a CAPTCHA token. */
    public void assertAllowed(String loginIdentifier, String clientIp) {
        LoginKeys keys = keys(loginIdentifier, clientIp);
        long retryAfter = execute(CHECK_LOCK_SCRIPT, List.of(keys.accountLock(), keys.ipLock()));
        rejectWhenLocked(retryAfter);
    }

    /** Records every login request by IP, including requests with an invalid CAPTCHA. */
    public void recordIpAttempt(String clientIp) {
        String ipFingerprint = sha256(normalizeIp(clientIp));
        long retryAfter = execute(RECORD_ATTEMPT_SCRIPT,
                List.of(keyPrefix + ":attempt:ip:" + ipFingerprint,
                        keyPrefix + ":lock:ip:" + ipFingerprint),
                ipAttemptLimit, attemptWindowSeconds, lockSeconds);
        rejectWhenLocked(retryAfter);
    }

    /** Records an account attempt only after CAPTCHA verification to reduce lockout abuse. */
    public void recordAccountAttempt(String loginIdentifier, String clientIp) {
        LoginKeys keys = keys(loginIdentifier, clientIp);
        long retryAfter = execute(RECORD_ATTEMPT_SCRIPT,
                List.of(keys.accountAttempts(), keys.accountLock()),
                accountAttemptLimit, attemptWindowSeconds, lockSeconds);
        rejectWhenLocked(retryAfter);
    }

    /** Records an authentication failure and activates short-term locks at the configured thresholds. */
    public void recordFailure(String loginIdentifier, String clientIp) {
        LoginKeys keys = keys(loginIdentifier, clientIp);
        long retryAfter = execute(RECORD_FAILURE_SCRIPT,
                List.of(keys.accountFailures(), keys.ipFailures(), keys.accountLock(), keys.ipLock()),
                accountFailureLimit, ipFailureLimit, failureWindowSeconds, lockSeconds);
        if (retryAfter > 0) {
            log.warn("Login protection activated for account fingerprint {} and client fingerprint {}",
                    keys.accountFingerprint().substring(0, 12), keys.ipFingerprint().substring(0, 12));
        }
        rejectWhenLocked(retryAfter);
    }

    /** A successful login resets only the account's consecutive-failure counter. */
    public void recordSuccess(String loginIdentifier, String clientIp) {
        LoginKeys keys = keys(loginIdentifier, clientIp);
        try {
            redisTemplate.delete(keys.accountFailures());
        } catch (DataAccessException e) {
            log.error("Cannot clear login failure counter", e);
            throw ApiException.serviceUnavailable("登录保护服务暂不可用，请稍后重试");
        }
    }

    private long execute(DefaultRedisScript<Long> script, List<String> keys, Object... arguments) {
        try {
            Object[] serializedArguments = java.util.Arrays.stream(arguments)
                    .map(String::valueOf)
                    .toArray(String[]::new);
            Long result = redisTemplate.execute(script, keys, serializedArguments);
            if (result == null) {
                throw ApiException.serviceUnavailable("登录保护服务暂不可用，请稍后重试");
            }
            return result;
        } catch (DataAccessException e) {
            log.error("Login protection Redis operation failed", e);
            throw ApiException.serviceUnavailable("登录保护服务暂不可用，请稍后重试");
        }
    }

    private void rejectWhenLocked(long retryAfter) {
        if (retryAfter > 0) {
            throw ApiException.tooManyRequests(
                    "登录尝试过于频繁，请在 " + retryAfter + " 秒后重试", retryAfter);
        }
    }

    private LoginKeys keys(String loginIdentifier, String clientIp) {
        String accountFingerprint = sha256(normalizeAccount(loginIdentifier));
        String ipFingerprint = sha256(normalizeIp(clientIp));
        return new LoginKeys(
                keyPrefix + ":attempt:account:" + accountFingerprint,
                keyPrefix + ":failure:account:" + accountFingerprint,
                keyPrefix + ":failure:ip:" + ipFingerprint,
                keyPrefix + ":lock:account:" + accountFingerprint,
                keyPrefix + ":lock:ip:" + ipFingerprint,
                accountFingerprint,
                ipFingerprint);
    }

    private String normalizeAccount(String loginIdentifier) {
        return loginIdentifier == null ? "" : loginIdentifier.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static int requirePositive(int value, String label) {
        if (value <= 0) throw new IllegalArgumentException(label + "必须大于 0");
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private record LoginKeys(
            String accountAttempts,
            String accountFailures,
            String ipFailures,
            String accountLock,
            String ipLock,
            String accountFingerprint,
            String ipFingerprint) {
    }
}
