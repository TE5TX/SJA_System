package com.sja.system.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterCookieTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUsingHttpOnlyAccessCookieValue() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, userDetailsService);
        SjaUserPrincipal principal = new SjaUserPrincipal(7L, "student", "", 13, 2,
                List.of(new SimpleGrantedAuthority("ROLE_13")));

        when(jwtUtils.validateAccessToken("cookie-token")).thenReturn(true);
        when(jwtUtils.getUsername("cookie-token")).thenReturn("student");
        when(jwtUtils.getUserId("cookie-token")).thenReturn(7L);
        when(jwtUtils.getTokenVersion("cookie-token")).thenReturn(2);
        when(userDetailsService.loadUserByUsername("student")).thenReturn(principal);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("sja_access_token", "cookie-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // 202609201535 - 验证认证过滤器只需读取浏览器自动携带的Access Token Cookie。
        assertEquals("student", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }
}
