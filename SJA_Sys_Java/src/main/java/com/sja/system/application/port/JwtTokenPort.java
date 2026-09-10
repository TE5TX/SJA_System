package com.sja.system.application.port;

import java.time.Instant;

/**
 * Token operations required by the authentication application service.
 */
public interface JwtTokenPort {

    String generateAccessToken(Long userId, String username, Integer tokenVersion);

    String generateRefreshToken(Long userId, String username, Integer tokenVersion);

    boolean validateRefreshToken(String token);

    Long getUserId(String token);

    String getUsername(String token);

    Integer getTokenVersion(String token);

    String getTokenId(String token);

    Instant getExpirationInstant(String token);
}
