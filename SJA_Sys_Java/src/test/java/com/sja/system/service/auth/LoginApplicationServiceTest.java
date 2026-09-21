package com.sja.system.service.auth;

import com.sja.system.application.model.LoginResult;
import com.sja.system.application.model.TokenPair;
import com.sja.system.application.port.CredentialAuthenticationPort;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.auth.LoginDTO;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.application.vo.system.MenuTreeVO;
import com.sja.system.application.vo.user.UserVO;
import com.sja.system.service.system.SystemService;
import com.sja.system.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginApplicationServiceTest {

    private CredentialAuthenticationPort authenticationPort;
    private SysUserMapper userMapper;
    private SystemService systemService;
    private UserService userService;
    private LoginCaptchaService captchaService;
    private LoginThrottleService throttleService;
    private AuthTokenService tokenService;
    private LoginApplicationService service;

    @BeforeEach
    void setUp() {
        authenticationPort = mock(CredentialAuthenticationPort.class);
        userMapper = mock(SysUserMapper.class);
        systemService = mock(SystemService.class);
        userService = mock(UserService.class);
        captchaService = mock(LoginCaptchaService.class);
        throttleService = mock(LoginThrottleService.class);
        tokenService = mock(AuthTokenService.class);
        service = new LoginApplicationService(authenticationPort, userMapper, systemService,
                userService, captchaService, throttleService, tokenService);
    }

    @Test
    void checksThrottleBeforeConsumingCaptcha() {
        doThrow(ApiException.tooManyRequests("登录尝试过于频繁", 30))
                .when(throttleService).assertAllowed("student", "127.0.0.1");

        assertThrows(ApiException.class, () -> service.login(loginDto(), "127.0.0.1"));

        verifyNoInteractions(captchaService, authenticationPort);
    }

    @Test
    void invalidCaptchaDoesNotCountAsAnAccountPasswordAttempt() {
        when(captchaService.consume("captcha-token")).thenReturn(false);

        assertThrows(ApiException.class, () -> service.login(loginDto(), "127.0.0.1"));

        verify(throttleService).recordIpAttempt("127.0.0.1");
        verify(throttleService, never()).recordAccountAttempt(any(), any());
        verifyNoInteractions(authenticationPort);
    }

    @Test
    void buildsLoginResponseFromExistingUserAndSystemServices() {
        LoginDTO dto = loginDto();
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("student");
        when(captchaService.consume("captcha-token")).thenReturn(true);
        when(authenticationPort.authenticate("student", "password123"))
                .thenReturn(Optional.of("student"));
        when(userMapper.selectOne(any())).thenReturn(user);

        UserVO profile = new UserVO();
        profile.setId(7L);
        profile.setUsername("student");
        profile.setRealName("测试用户");
        profile.setRoleCodes(List.of(3));
        profile.setMustChangePassword(1);
        when(userService.getUserProfile(7L)).thenReturn(profile);
        when(systemService.getMenuTreeByRoleCodes(List.of(3))).thenReturn(List.of(new MenuTreeVO()));
        when(tokenService.issue(user)).thenReturn(new TokenPair("access", "refresh"));

        // 202609101605 - 完成了登录应用服务复用用户资料与菜单服务的测试。
        LoginResult result = service.login(dto, "127.0.0.1");

        // 202609201535 - 验证登录响应模型不再携带可被JavaScript读取的Access Token。
        assertEquals("access", result.tokenPair().getAccessToken());
        assertEquals("refresh", result.tokenPair().getRefreshToken());
        assertEquals("测试用户", result.response().getUserInfo().getRealName());
        assertTrue(result.response().getMustChangePassword());
        verify(throttleService).recordSuccess("student", "127.0.0.1");
    }

    @Test
    void delegatesValidRefreshTokenRotationToTokenService() {
        TokenPair expected = new TokenPair("new-access", "new-refresh");
        when(tokenService.rotate("refresh-token")).thenReturn(expected);

        TokenPair actual = service.refresh("1", "refresh-token");

        // 202609201535 - 验证Controller不再承担令牌刷新业务判断。
        assertEquals(expected, actual);
        verify(tokenService).rotate("refresh-token");
    }

    @Test
    void rejectsMissingRefreshTokenBeforeCallingTokenService() {
        assertThrows(ApiException.class, () -> service.refresh("1", " "));
        verify(tokenService, never()).rotate(any());
    }

    private LoginDTO loginDto() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("student");
        dto.setPassword("password123");
        dto.setCaptchaToken("captcha-token");
        return dto;
    }
}
