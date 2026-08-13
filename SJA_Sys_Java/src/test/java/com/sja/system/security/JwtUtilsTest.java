package com.sja.system.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret",
                "0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtils, "accessExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", 120_000L);
        jwtUtils.init();
    }

    @Test
    void separatesAccessAndRefreshTokensAndCarriesRevocationVersion() {
        String access = jwtUtils.generateAccessToken(7L, "student", 4);
        String refresh = jwtUtils.generateRefreshToken(7L, "student", 4);

        assertTrue(jwtUtils.validateAccessToken(access));
        assertFalse(jwtUtils.validateRefreshToken(access));
        assertTrue(jwtUtils.validateRefreshToken(refresh));
        assertFalse(jwtUtils.validateAccessToken(refresh));
        assertEquals(7L, jwtUtils.getUserId(access));
        assertEquals(4, jwtUtils.getTokenVersion(access));
    }
}
