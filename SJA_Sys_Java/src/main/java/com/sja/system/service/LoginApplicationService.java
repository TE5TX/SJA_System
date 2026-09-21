package com.sja.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.application.model.LoginResult;
import com.sja.system.application.model.TokenPair;
import com.sja.system.application.port.CredentialAuthenticationPort;
import com.sja.system.common.ApiException;
import com.sja.system.dto.LoginDTO;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.vo.LoginVO;
import com.sja.system.vo.MenuTreeVO;
import com.sja.system.vo.SessionUserVO;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Coordinates credential validation, login throttling, session data and token issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginApplicationService {

    private final CredentialAuthenticationPort credentialAuthenticationPort;
    private final SysUserMapper sysUserMapper;
    private final SystemService systemService;
    private final UserService userService;
    private final LoginCaptchaService loginCaptchaService;
    private final LoginThrottleService loginThrottleService;
    private final AuthTokenService authTokenService;

    public LoginResult login(LoginDTO dto, String clientIp) {
        String loginIdentifier = dto.getUsername().trim();
        loginThrottleService.assertAllowed(loginIdentifier, clientIp);
        loginThrottleService.recordIpAttempt(clientIp);
        if (!loginCaptchaService.consume(dto.getCaptchaToken().trim())) {
            throw ApiException.badRequest("滑块验证已失效，请重新验证");
        }
        loginThrottleService.recordAccountAttempt(loginIdentifier, clientIp);

        String canonicalUsername = credentialAuthenticationPort
                .authenticate(loginIdentifier, dto.getPassword())
                .orElse(null);
        if (canonicalUsername == null) {
            throw authenticationFailed(loginIdentifier, clientIp);
        }
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", canonicalUsername)
                .eq("is_valid", 1));
        if (user == null) {
            throw authenticationFailed(loginIdentifier, clientIp);
        }

        // 202609101605 - 完成了登录查询和会话组装从Controller迁移到应用服务的任务。
        loginThrottleService.recordSuccess(loginIdentifier, clientIp);
        UserVO profile = userService.getUserProfile(user.getId());
        List<Integer> roleCodes = profile.getRoleCodes() == null
                ? Collections.emptyList()
                : profile.getRoleCodes();
        List<MenuTreeVO> menus = roleCodes.isEmpty()
                ? Collections.emptyList()
                : systemService.getMenuTreeByRoleCodes(roleCodes);
        TokenPair tokenPair = authTokenService.issue(user);

        LoginVO response = new LoginVO();
        response.setUserInfo(toSessionUser(profile));
        response.setMenus(menus);
        response.setMustChangePassword(Integer.valueOf(1).equals(profile.getMustChangePassword()));
        log.info("User logged in: {}", user.getUsername());
        // 202609201535 - 修复了Access Token暴露给前端并被Web Storage持久化的问题。
        return new LoginResult(response, tokenPair);
    }

    /**
     * 轮换当前会话的刷新令牌并签发新的访问令牌。
     *
     * @param refreshRequest 刷新请求专用标识
     * @param refreshToken HttpOnly Cookie 中的刷新令牌
     * @return 新的一对认证令牌
     */
    public TokenPair refresh(String refreshRequest, String refreshToken) {
        if (!"1".equals(refreshRequest)) {
            throw ApiException.badRequest("刷新请求标识无效");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.unauthorized("未提供刷新令牌，请重新登录");
        }
        return authTokenService.rotate(refreshToken);
    }

    /**
     * 撤销指定用户的全部刷新令牌。
     *
     * @param userId 当前登录用户ID
     */
    public void logout(Long userId) {
        if (userId != null) {
            authTokenService.revokeAll(userId);
        }
    }

    private ApiException authenticationFailed(String loginIdentifier, String clientIp) {
        log.warn("Login failed - invalid credentials from client: {}", clientIp);
        loginThrottleService.recordFailure(loginIdentifier, clientIp);
        return ApiException.unauthorized("用户名或密码错误");
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
}
