package com.sja.system.controller;

import com.sja.system.application.model.LoginResult;
import com.sja.system.application.model.TokenPair;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.Result;
import com.sja.system.controller.support.AuthenticationCookieWriter;
import com.sja.system.dto.LoginDTO;
import com.sja.system.service.LoginApplicationService;
import com.sja.system.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import static com.sja.system.security.AuthenticationCookieNames.REFRESH;

/**
 * 认证协议适配器，仅负责登录参数接收、认证Cookie写入和CSRF令牌响应。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
// 202609211059 - 完成了认证Controller当前操作人解析下沉。
public class AuthController {

    private final LoginApplicationService loginApplicationService;
    private final AuthenticationCookieWriter authenticationCookieWriter;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request,
                                 HttpServletResponse response) {
        // 202609101605 - 完成了登录Controller只负责请求与Cookie响应转换的任务。
        LoginResult loginResult = loginApplicationService.login(dto, request.getRemoteAddr());
        authenticationCookieWriter.write(response, loginResult.tokenPair());
        return Result.success(loginResult.response());
    }

    @PostMapping("/logout")
    public Result<String> logout(CurrentActor actor, HttpServletResponse response) {
        // 202609211059 - 完成了登出接口当前操作人解析从Controller下沉至统一参数解析器。
        loginApplicationService.logout(actor.userId());
        authenticationCookieWriter.clear(response);
        return Result.success("登出成功");
    }

    @PostMapping("/refresh")
    public Result<String> refresh(
            @RequestHeader("X-SJA-Refresh") String refreshRequest,
            @CookieValue(value = REFRESH, required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            // 202609201535 - 完成了刷新业务下沉并禁止向前端返回Access Token的任务。
            TokenPair tokenPair = loginApplicationService.refresh(refreshRequest, refreshToken);
            authenticationCookieWriter.write(response, tokenPair);
            return Result.success("令牌刷新成功");
        } catch (RuntimeException e) {
            authenticationCookieWriter.clear(response);
            throw e;
        }
    }

    /**
     * 生成供前端随状态变更请求回传的 CSRF Token。
     */
    @GetMapping("/csrf")
    public Result<String> csrf(CsrfToken csrfToken) {
        return Result.success(csrfToken.getToken());
    }
}
