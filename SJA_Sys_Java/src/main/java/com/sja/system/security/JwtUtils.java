package com.sja.system.security;

import com.sja.system.application.port.JwtTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token utility for generating, parsing and validating tokens.
 */
@Slf4j
@Component
public class JwtUtils implements JwtTokenPort {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT token for the given user.
     *
     * @param userId   user primary key
     * @param username login name
     * @param tokenVersion current server-side token version
     * @return signed JWT string
     */
    @Override
    public String generateAccessToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, tokenVersion, "ACCESS", accessExpiration);
    }

    @Override
    public String generateRefreshToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, tokenVersion, "REFRESH", refreshExpiration);
    }

    private String generateToken(Long userId, String username, Integer tokenVersion,
                                 String tokenType, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("tokenVersion", tokenVersion == null ? 0 : tokenVersion);
        claims.put("tokenType", tokenType);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parse and return all claims from the token.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract userId claim.
     */
    @Override
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract username claim (also available via getSubject).
     */
    @Override
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Extract the server-side revocation version claim.
     */
    @Override
    public Integer getTokenVersion(String token) {
        Number version = parseToken(token).get("tokenVersion", Number.class);
        return version == null ? 0 : version.intValue();
    }

    public String getTokenType(String token) {
        return parseToken(token).get("tokenType", String.class);
    }

    @Override
    public String getTokenId(String token) {
        return parseToken(token).getId();
    }

    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    @Override
    public Instant getExpirationInstant(String token) {
        return getExpiration(token).toInstant();
    }

    /**
     * Validate the token: not expired, signature OK, claims parseable.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token) && "ACCESS".equals(getTokenType(token));
    }

    @Override
    public boolean validateRefreshToken(String token) {
        return validateToken(token) && "REFRESH".equals(getTokenType(token));
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * Check whether the token has expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
