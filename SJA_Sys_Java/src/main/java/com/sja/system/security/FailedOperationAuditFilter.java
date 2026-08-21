package com.sja.system.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedOperationAuditFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_AUDIT_REASON_LENGTH = 300;
    private static final int MAX_JSON_RESPONSE_BYTES = 64 * 1024;

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !MUTATING_METHODS.contains(request.getMethod().toUpperCase())
                || request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 202608210907 - 从失败JSON响应中提取经过限长的业务原因，替代仅记录HTTP状态码。
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        boolean unhandledFailure = false;
        try {
            filterChain.doFilter(request, responseWrapper);
        } catch (ServletException | IOException | RuntimeException e) {
            unhandledFailure = true;
            throw e;
        } finally {
            int status = unhandledFailure
                    ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    : responseWrapper.getStatus();
            if (status >= 400) {
                auditFailure(request, status, extractFailureReason(responseWrapper));
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private void auditFailure(HttpServletRequest request, int status, String failureReason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SjaUserPrincipal)) {
            return;
        }
        SjaUserPrincipal principal = (SjaUserPrincipal) authentication.getPrincipal();
        try {
            operationLogService.logFailure(principal.getUserId(), principal.getUsername(),
                    String.valueOf(principal.getHighestRoleCode()), request.getMethod(),
                    targetModule(request.getRequestURI()), failureDetail(status, failureReason));
        } catch (RuntimeException logFailure) {
            log.error("Failed to persist failure audit log, requestId={}",
                    request.getAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE), logFailure);
        }
    }

    private String extractFailureReason(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0 || content.length > MAX_JSON_RESPONSE_BYTES) return null;
        String contentType = response.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("json")) return null;
        try {
            Charset charset = response.getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(response.getCharacterEncoding());
            JsonNode messageNode = objectMapper.readTree(new String(content, charset)).get("message");
            return messageNode != null && messageNode.isTextual()
                    ? normalizeReason(messageNode.asText())
                    : null;
        } catch (RuntimeException | IOException parseFailure) {
            log.debug("Cannot extract failure reason from JSON response", parseFailure);
            return null;
        }
    }

    private String failureDetail(int status, String failureReason) {
        String detail = "请求失败，HTTP状态码=" + status;
        return failureReason == null || failureReason.isBlank()
                ? detail
                : detail + "，原因=" + failureReason;
    }

    private String normalizeReason(String reason) {
        String normalized = reason.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .replaceAll(
                        "(?i)((?:password|currentPassword|token|importToken|secret|authorization|密码|令牌)\\s*[=:：]\\s*)[^,，;；\\s]+",
                        "$1[已脱敏]")
                .trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() <= MAX_AUDIT_REASON_LENGTH
                ? normalized
                : normalized.substring(0, MAX_AUDIT_REASON_LENGTH) + "…";
    }

    private String targetModule(String uri) {
        if (uri.startsWith("/api/v1/fee")) return "FEE";
        if (uri.startsWith("/api/v1/user") || uri.startsWith("/api/v1/users")) return "USER";
        if (uri.startsWith("/api/v1/system")) return "SYSTEM";
        if (uri.startsWith("/api/v1/upload")) return "UPLOAD";
        return "API";
    }
}
