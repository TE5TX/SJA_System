package com.sja.system.service.async.model;

import java.util.Locale;

/**
 * 创建并校验 Redis Stream 消费者实例身份。
 */
public final class AsyncTaskConsumerIdentityFactory {

    private static final int MAX_NAME_LENGTH = 128;
    private static final String AUTO_PREFIX = "sja-write-worker-";

    private AsyncTaskConsumerIdentityFactory() {
    }

    /**
     * 优先使用显式配置；未配置时根据主机、进程和随机后缀生成唯一名称。
     */
    public static AsyncTaskConsumerIdentity create(String configuredName,
                                                   String runtimeHost,
                                                   long processId,
                                                   String randomSuffix,
                                                   String instanceId) {
        String normalizedInstanceId = requireText(instanceId, "消费者实例标识不能为空");
        if (hasText(configuredName)) {
            String normalized = normalize(configuredName);
            validateName(normalized);
            return new AsyncTaskConsumerIdentity(normalized, normalizedInstanceId, "configuration");
        }

        String host = normalize(hasText(runtimeHost) ? runtimeHost : "unknown-host");
        if (!hasText(host)) {
            host = "unknown-host";
        }
        String suffix = normalize(requireText(randomSuffix, "消费者随机后缀不能为空"));
        if (!hasText(suffix)) {
            throw new IllegalArgumentException("消费者随机后缀不能仅包含特殊字符");
        }
        String generated = AUTO_PREFIX + host + "-" + processId + "-" + suffix;
        if (generated.length() > MAX_NAME_LENGTH) {
            int hostLength = Math.max(1,
                    MAX_NAME_LENGTH - AUTO_PREFIX.length() - String.valueOf(processId).length() - suffix.length() - 2);
            generated = AUTO_PREFIX + host.substring(0, Math.min(host.length(), hostLength))
                    + "-" + processId + "-" + suffix;
        }
        validateName(generated);
        return new AsyncTaskConsumerIdentity(generated, normalizedInstanceId, "runtime");
    }

    private static String normalize(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
    }

    private static void validateName(String name) {
        if (!hasText(name)) {
            throw new IllegalArgumentException("Redis Stream消费者名称不能为空或仅包含特殊字符");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Redis Stream消费者名称不能超过" + MAX_NAME_LENGTH + "个字符");
        }
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
