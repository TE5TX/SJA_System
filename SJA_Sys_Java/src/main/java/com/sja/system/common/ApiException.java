package com.sja.system.common;

// 202608101042 - 修复了业务错误始终返回 HTTP 200 的问题。
public class ApiException extends RuntimeException {

    private final int status;
    private final Long retryAfterSeconds;

    public ApiException(int status, String message) {
        this(status, message, null);
    }

    public ApiException(int status, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getStatus() {
        return status;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(Constants.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(Constants.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(Constants.FORBIDDEN, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(Constants.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(Constants.CONFLICT, message);
    }

    public static ApiException tooManyRequests(String message, long retryAfterSeconds) {
        // 202608140858 - 增加了登录限流所需的 429 状态与 Retry-After 秒数。
        return new ApiException(Constants.TOO_MANY_REQUESTS, message, Math.max(1L, retryAfterSeconds));
    }

    public static ApiException serviceUnavailable(String message) {
        return new ApiException(Constants.SERVICE_UNAVAILABLE, message);
    }
}
