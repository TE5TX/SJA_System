package com.sja.system.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 [{}]: {}", request.getRequestURI(), message);
        return response(Constants.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<?>> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 [{}]: {}", request.getRequestURI(), message);
        return response(Constants.BAD_REQUEST, message);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<?>> handleApiException(ApiException e, HttpServletRequest request) {
        log.warn("业务请求失败 [{}]: {}", request.getRequestURI(), e.getMessage());
        return response(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<?>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问被拒绝 [{}]: {}", request.getRequestURI(), e.getMessage());
        return response(Constants.FORBIDDEN, "没有权限访问该资源");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<?>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return response(Constants.ERROR, "系统内部错误，请联系管理员");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return response(Constants.ERROR, "系统内部错误，请联系管理员");
    }

    private ResponseEntity<Result<?>> response(int status, String message) {
        return ResponseEntity.status(status).body(Result.error(status, message));
    }

}
