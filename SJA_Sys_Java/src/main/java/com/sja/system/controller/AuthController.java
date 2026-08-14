package com.sja.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.common.Result;
import com.sja.system.dto.LoginDTO;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.security.SecurityUtils;
import com.sja.system.security.TokenPair;
import com.sja.system.service.AuthTokenService;
import com.sja.system.service.LoginCaptchaService;
import com.sja.system.service.LoginThrottleService;
import com.sja.system.service.SystemService;
import com.sja.system.service.UserService;
import com.sja.system.vo.LoginVO;
import com.sja.system.vo.MenuTreeVO;
import com.sja.system.vo.SessionUserVO;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "sja_refresh_token";

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SystemService systemService;
    private final UserService userService;
    private final LoginCaptchaService loginCaptchaService;
    private final LoginThrottleService loginThrottleService;
    private final AuthTokenService authTokenService;
    private final SecurityUtils securityUtils;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${sja.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request,
                                 HttpServletResponse response) {
        String loginIdentifier = dto.getUsername().trim();
        String clientIp = request.getRemoteAddr();
        // 202608140858 - 在验证码与密码认证之外增加了账号/IP 登录节流和短期锁定。
        loginThrottleService.assertAllowed(loginIdentifier, clientIp);
        loginThrottleService.recordIpAttempt(clientIp);
        if (!loginCaptchaService.consume(dto.getCaptchaToken().trim())) {
            throw ApiException.badRequest("滑块验证已失效，请重新验证");
        }
        loginThrottleService.recordAccountAttempt(loginIdentifier, clientIp);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, dto.getPassword()));

            SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                    .eq("username", authentication.getName())
                    .eq("is_valid", 1));
            if (user == null) {
                loginThrottleService.recordFailure(loginIdentifier, clientIp);
                throw ApiException.unauthorized("用户名或密码错误");
            }

            loginThrottleService.recordSuccess(loginIdentifier, clientIp);

            List<Integer> roleCodes = loadRoleCodes(user.getId());
            TokenPair tokenPair = authTokenService.issue(user);
            writeRefreshCookie(response, tokenPair.getRefreshToken());

            UserVO profile = userService.getUserProfile(user.getId());
            List<MenuTreeVO> menus = roleCodes.isEmpty()
                    ? Collections.emptyList()
                    : systemService.getMenuTreeByRoleCodes(roleCodes);

            LoginVO loginVO = new LoginVO();
            loginVO.setToken(tokenPair.getAccessToken());
            loginVO.setUserInfo(toSessionUser(profile));
            loginVO.setMenus(menus);
            loginVO.setMustChangePassword(Integer.valueOf(1).equals(user.getMustChangePassword()));

            log.info("User logged in: {}", user.getUsername());
            return Result.success(loginVO);
        } catch (BadCredentialsException e) {
            log.warn("Login failed - bad credentials from client: {}", clientIp);
            loginThrottleService.recordFailure(loginIdentifier, clientIp);
            throw ApiException.unauthorized("用户名或密码错误");
        } catch (AuthenticationException e) {
            log.warn("Authentication failed from client {}: {}", clientIp, e.getClass().getSimpleName());
            loginThrottleService.recordFailure(loginIdentifier, clientIp);
            throw ApiException.unauthorized("用户名或密码错误");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login error", e);
            throw new RuntimeException("登录失败，请稍后重试", e);
        }
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

    private List<Integer> loadRoleCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(new QueryWrapper<SysUserRole>()
                        .eq("user_id", userId)).stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        return sysRoleMapper.selectList(new QueryWrapper<SysRole>()
                        .in("id", roleIds)
                        .eq("is_valid", 1)).stream()
                .map(SysRole::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
    }

    private SessionUserVO toSessionUser(UserVO profile) {
        SessionUserVO session = new SessionUserVO();
        session.setId(profile.getId());
        session.setUsername(profile.getUsername());
        session.setRealName(profile.getRealName());
        session.setPhoto(profile.getPhoto());
        session.setIsValid(profile.getIsValid());
        session.setRoleNames(profile.getRoleNames());
        session.setRoleCodes(profile.getRoleCodes());
        session.setDeptNames(profile.getDeptNames());
        session.setDeptCodes(profile.getDeptCodes());
        return session;
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
