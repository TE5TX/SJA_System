package com.sja.system.service.auth;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sja.system.application.model.TokenPair;
import com.sja.system.common.ApiException;
import com.sja.system.dal.dataobject.SysRefreshToken;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.mysql.SysRefreshTokenMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.framework.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenServiceTest {

    private JwtUtils jwtUtils;
    private SysUserMapper userMapper;
    private SysRefreshTokenMapper refreshTokenMapper;
    private AuthTokenService service;
    private final List<SysRefreshToken> insertedTokens = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret",
                "0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtils, "accessExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", 120_000L);
        jwtUtils.init();

        userMapper = mock(SysUserMapper.class);
        refreshTokenMapper = mock(SysRefreshTokenMapper.class);
        when(refreshTokenMapper.insert(any(SysRefreshToken.class))).thenAnswer(invocation -> {
            SysRefreshToken token = invocation.getArgument(0);
            token.setId((long) insertedTokens.size() + 1);
            insertedTokens.add(token);
            return 1;
        });
        service = new AuthTokenService(jwtUtils, userMapper, refreshTokenMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rotatesOnceAndRevokesTheFamilyWhenAnOldRefreshTokenIsReplayed() {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setUsername("student");
        user.setIsValid(1);
        user.setTokenVersion(3);

        TokenPair initial = service.issue(user);
        SysRefreshToken originalRecord = insertedTokens.get(0);
        when(refreshTokenMapper.selectOne(any())).thenReturn(originalRecord);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(refreshTokenMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(userMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        TokenPair rotated = service.rotate(initial.getRefreshToken());

        assertTrue(jwtUtils.validateAccessToken(rotated.getAccessToken()));
        assertTrue(jwtUtils.validateRefreshToken(rotated.getRefreshToken()));
        assertNotEquals(initial.getRefreshToken(), rotated.getRefreshToken());
        assertEquals(originalRecord.getTokenFamily(), insertedTokens.get(1).getTokenFamily());

        originalRecord.setRevokedAt(LocalDateTime.now());
        originalRecord.setReplacedBy(insertedTokens.get(1).getTokenId());

        assertThrows(ApiException.class, () -> service.rotate(initial.getRefreshToken()));
        verify(userMapper, atLeastOnce()).update(isNull(), any(Wrapper.class));
    }
}
