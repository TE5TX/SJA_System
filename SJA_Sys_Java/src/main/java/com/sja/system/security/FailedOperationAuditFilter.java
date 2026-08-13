package com.sja.system.security;

import com.sja.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedOperationAuditFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final OperationLogService operationLogService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !MUTATING_METHODS.contains(request.getMethod().toUpperCase())
                || request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean unhandledFailure = false;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException e) {
            unhandledFailure = true;
            throw e;
        } finally {
            int status = unhandledFailure ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();
            if (status >= 400) {
                auditFailure(request, status);
            }
        }
    }

    private void auditFailure(HttpServletRequest request, int status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SjaUserPrincipal)) {
            return;
        }
        SjaUserPrincipal principal = (SjaUserPrincipal) authentication.getPrincipal();
        try {
            operationLogService.logFailure(principal.getUserId(), principal.getUsername(),
                    String.valueOf(principal.getHighestRoleCode()), request.getMethod(),
                    targetModule(request.getRequestURI()), "请求失败，HTTP状态码=" + status);
        } catch (RuntimeException logFailure) {
            log.error("Failed to persist failure audit log, requestId={}",
                    request.getAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE), logFailure);
        }
    }

    private String targetModule(String uri) {
        if (uri.startsWith("/api/v1/fee")) return "FEE";
        if (uri.startsWith("/api/v1/user") || uri.startsWith("/api/v1/users")) return "USER";
        if (uri.startsWith("/api/v1/system")) return "SYSTEM";
        if (uri.startsWith("/api/v1/upload")) return "UPLOAD";
        return "API";
    }
}
