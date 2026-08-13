package com.sja.system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token utility for generating, parsing and validating tokens.
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private Key signingKey;

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
    public String generateAccessToken(Long userId, String username, Integer tokenVersion) {
        return generateToken(userId, username, tokenVersion, "ACCESS", accessExpiration);
    }

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
                .setClaims(claims)
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse and return all claims from the token.
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract userId claim.
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract username claim (also available via getSubject).
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * Extract the server-side revocation version claim.
     */
    public Integer getTokenVersion(String token) {
        Number version = parseToken(token).get("tokenVersion", Number.class);
        return version == null ? 0 : version.intValue();
    }

    public String getTokenType(String token) {
        return parseToken(token).get("tokenType", String.class);
    }

    public String getTokenId(String token) {
        return parseToken(token).getId();
    }

    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
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
