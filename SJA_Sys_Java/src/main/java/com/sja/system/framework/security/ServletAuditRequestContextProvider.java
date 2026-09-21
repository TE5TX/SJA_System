package com.sja.system.framework.security;

import com.sja.system.application.port.AuditRequestContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Adapts the active servlet request to the audit context required by the application layer.
 */
@Component
public class ServletAuditRequestContextProvider implements AuditRequestContextProvider {

    @Override
    public Optional<AuditRequestContext> currentContext() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        Object requestId = request.getAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE);
        Object start = request.getAttribute(RequestTraceFilter.REQUEST_START_ATTRIBUTE);
        Long durationMs = start instanceof Long
                ? Math.max(0, System.currentTimeMillis() - (Long) start)
                : null;
        return Optional.of(new AuditRequestContext(
                requestId == null ? null : requestId.toString(),
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader("User-Agent"),
                request.getRemoteAddr(),
                durationMs));
    }
}
