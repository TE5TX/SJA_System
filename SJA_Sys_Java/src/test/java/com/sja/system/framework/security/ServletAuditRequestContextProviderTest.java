package com.sja.system.framework.security;

import com.sja.system.application.port.AuditRequestContextProvider.AuditRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServletAuditRequestContextProviderTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void adaptsServletMetadataWithoutLeakingServletTypesToTheServiceLayer() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/users/8");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("User-Agent", "audit-test");
        request.setAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE, "request-12345678");
        request.setAttribute(RequestTraceFilter.REQUEST_START_ATTRIBUTE, System.currentTimeMillis());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuditRequestContext context = new ServletAuditRequestContextProvider()
                .currentContext()
                .orElseThrow();

        assertEquals("request-12345678", context.requestId());
        assertEquals("PUT", context.requestMethod());
        assertEquals("/api/v1/users/8", context.requestUri());
        assertEquals("audit-test", context.userAgent());
        assertEquals("192.0.2.10", context.ipAddress());
        assertTrue(context.durationMs() >= 0);
    }
}
