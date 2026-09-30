package com.sja.system.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.service.system.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// 202608210907 - 验证失败审计日志提取业务原因、忽略响应数据并保持原响应内容。
class FailedOperationAuditFilterTest {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void recordsJsonMessageWithoutRecordingResponseData() throws Exception {
        OperationLogService operationLogService = mock(OperationLogService.class);
        FailedOperationAuditFilter filter = new FailedOperationAuditFilter(
                operationLogService, new ObjectMapper());
        SjaUserPrincipal principal = new SjaUserPrincipal(7L, "2023030103052", "", 3, 0,
                List.of(new SimpleGrantedAuthority("ROLE_3")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/import/preview");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String responseJson = "{\"code\":400,\"message\":\"Excel第一行必须依次为：姓名、学号；token=message-secret\","
                + "\"data\":{\"password\":\"never-log-this\"}}";

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            servletResponse.setContentType("application/json;charset=UTF-8");
            ((jakarta.servlet.http.HttpServletResponse) servletResponse).setStatus(400);
            servletResponse.getWriter().write(responseJson);
        });

        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        verify(operationLogService).logFailure(eq(7L), eq("2023030103052"), eq("3"),
                eq("POST"), eq("USER"), detailCaptor.capture());
        assertTrue(detailCaptor.getValue().contains("HTTP状态码=400"));
        assertTrue(detailCaptor.getValue().contains("Excel第一行必须依次为：姓名、学号"));
        assertTrue(detailCaptor.getValue().contains("token=[已脱敏]"));
        assertFalse(detailCaptor.getValue().contains("message-secret"));
        assertFalse(detailCaptor.getValue().contains("never-log-this"));
        assertEquals(responseJson, response.getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void recordsUtf8JsonMessageWhenResponseOmitsCharset() throws Exception {
        // 202609291633 - 完成了无charset响应仍以UTF-8正确记录中文失败原因的回归测试。
        OperationLogService operationLogService = mock(OperationLogService.class);
        FailedOperationAuditFilter filter = new FailedOperationAuditFilter(
                operationLogService, new ObjectMapper());
        SjaUserPrincipal principal = new SjaUserPrincipal(7L, "2023030103052", "", 3, 0,
                List.of(new SimpleGrantedAuthority("ROLE_3")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/user/profile/password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String responseJson = "{\"code\":400,\"message\":\"新密码不能与原密码相同\"}";

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletResponse.setContentType("application/json");
            ((jakarta.servlet.http.HttpServletResponse) servletResponse).setStatus(400);
            servletResponse.getOutputStream().write(responseJson.getBytes(StandardCharsets.UTF_8));
        });

        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        verify(operationLogService).logFailure(eq(7L), eq("2023030103052"), eq("3"),
                eq("PUT"), eq("USER"), detailCaptor.capture());
        assertEquals("请求失败，HTTP状态码=400，原因=新密码不能与原密码相同", detailCaptor.getValue());
        assertEquals(responseJson, response.getContentAsString(StandardCharsets.UTF_8));
    }
}
