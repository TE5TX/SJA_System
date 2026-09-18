package com.sja.system.controller;

import com.sja.system.application.model.LoginResult;
import com.sja.system.application.model.TokenPair;
import com.sja.system.common.ApiException;
import com.sja.system.common.Result;
import com.sja.system.dto.LoginDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.AuthTokenService;
import com.sja.system.service.LoginApplicationService;
import com.sja.system.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "sja_refresh_token";

    private final LoginApplicationService loginApplicationService;
    private final AuthTokenService authTokenService;
    private final SecurityUtils securityUtils;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${sja.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request,
                                 HttpServletResponse response) {
        // 202609101605 - 完成了登录Controller只负责请求与Cookie响应转换的任务。
        LoginResult loginResult = loginApplicationService.login(dto, request.getRemoteAddr());
        writeRefreshCookie(response, loginResult.refreshToken());
        return Result.success(loginResult.response());
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        Long userId = securityUtils.getCurrentUserId(request);
        if (userId != null) {
            authTokenService.revokeAll(userId);
        }
        clearRefreshCookie(response);
        return Result.success("登出成功");
    }

    @PostMapping("/refresh")
    public Result<String> refresh(
            @RequestHeader("X-SJA-Refresh") String refreshRequest,
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (!"1".equals(refreshRequest)) {
            throw ApiException.badRequest("刷新请求标识无效");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            clearRefreshCookie(response);
            throw ApiException.unauthorized("未提供刷新令牌，请重新登录");
        }
        try {
            TokenPair tokenPair = authTokenService.rotate(refreshToken);
            writeRefreshCookie(response, tokenPair.getRefreshToken());
            return Result.success(tokenPair.getAccessToken());
        } catch (RuntimeException e) {
            clearRefreshCookie(response);
            throw e;
        }
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(refreshExpiration))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
