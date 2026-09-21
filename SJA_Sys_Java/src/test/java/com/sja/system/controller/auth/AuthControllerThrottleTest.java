package com.sja.system.controller.auth;

import com.sja.system.application.model.LoginResult;
import com.sja.system.application.model.TokenPair;
import com.sja.system.common.ApiException;
import com.sja.system.common.GlobalExceptionHandler;
import com.sja.system.application.dto.auth.LoginDTO;
import com.sja.system.framework.security.SecurityUtils;
import com.sja.system.framework.web.support.AuthenticationCookieWriter;
import com.sja.system.service.auth.LoginApplicationService;
import com.sja.system.application.vo.auth.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 202609101605 - 完成了登录Controller委托应用服务并保留HTTP响应行为的测试。
@ExtendWith(MockitoExtension.class)
class AuthControllerThrottleTest {

    @Mock LoginApplicationService loginApplicationService;
    @Mock SecurityUtils securityUtils;
    @Mock AuthenticationCookieWriter authenticationCookieWriter;

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
                .when(loginApplicationService).login(any(LoginDTO.class), eq("127.0.0.1"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "81"))
                .andExpect(jsonPath("$.code").value(429));

        verify(loginApplicationService).login(any(LoginDTO.class), eq("127.0.0.1"));
    }

    @Test
    void passesTheDirectClientAddressToTheLoginUseCase() throws Exception {
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");
        when(loginApplicationService.login(any(LoginDTO.class), eq("203.0.113.9")))
                .thenReturn(new LoginResult(new LoginVO(), tokenPair));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.250")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        })
                        .content(loginJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist());

        verify(loginApplicationService).login(any(LoginDTO.class), eq("203.0.113.9"));
        verify(authenticationCookieWriter).write(any(), eq(tokenPair));
    }

    private String loginJson() {
        return """
                {"username":"student","password":"password123","captchaToken":"captcha-token"}
                """;
    }
}
