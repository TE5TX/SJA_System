package com.sja.system.controller;

import com.sja.system.common.ApiException;
import com.sja.system.common.GlobalExceptionHandler;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.AuthTokenService;
import com.sja.system.service.LoginCaptchaService;
import com.sja.system.service.LoginThrottleService;
import com.sja.system.service.SystemService;
import com.sja.system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 202608140858 - 覆盖了登录锁定响应和验证码失败时的账号防拒绝服务边界。
@ExtendWith(MockitoExtension.class)
class AuthControllerThrottleTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock SysUserMapper sysUserMapper;
    @Mock SysUserRoleMapper sysUserRoleMapper;
    @Mock SysRoleMapper sysRoleMapper;
    @Mock SystemService systemService;
    @Mock UserService userService;
    @Mock LoginCaptchaService loginCaptchaService;
    @Mock LoginThrottleService loginThrottleService;
    @Mock AuthTokenService authTokenService;
    @Mock SecurityUtils securityUtils;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returns429AndRetryAfterBeforeConsumingCaptchaWhenLocked() throws Exception {
        doThrow(ApiException.tooManyRequests("登录尝试过于频繁，请稍后重试", 81))
                .when(loginThrottleService).assertAllowed("student", "127.0.0.1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "81"))
                .andExpect(jsonPath("$.code").value(429));

        verifyNoInteractions(loginCaptchaService);
    }

    @Test
    void invalidCaptchaCountsIpRequestButDoesNotCountAccountAttempt() throws Exception {
        when(loginCaptchaService.consume("captcha-token")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.250")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        })
                        .content(loginJson()))
                .andExpect(status().isBadRequest());

        verify(loginThrottleService).recordIpAttempt("203.0.113.9");
        verify(loginThrottleService, never()).recordAccountAttempt("student", "203.0.113.9");
    }

    private String loginJson() {
        return """
                {"username":"student","password":"password123","captchaToken":"captcha-token"}
                """;
    }
}
