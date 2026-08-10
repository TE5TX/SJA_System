package com.sja.system.common;

// 202608101042 - 修复了业务错误始终返回 HTTP 200 的问题。
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
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
}
