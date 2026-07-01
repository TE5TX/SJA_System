package com.sja.system.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting security-related information from the
 * current security context and JWT tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final JwtUtils jwtUtils;

    /**
     * Get the current authenticated username from the SecurityContext.
     *
     * @return the username, or null if not authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    /**
     * Extract the JWT token string from the request Authorization header.
     *
     * @param request the HTTP request
     * @return the raw token string, or null if not present
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Get the current user's ID from the JWT token in the request.
     *
     * @param request the HTTP request
     * @return the user ID, or null if token is missing/invalid
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            return jwtUtils.getUserId(token);
        }
        return null;
    }

    /**
     * Get the current user's username from the JWT token in the request.
     *
     * @param request the HTTP request
     * @return the username, or null if token is missing/invalid
     */
    public String getCurrentUsernameFromToken(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            return jwtUtils.getUsername(token);
        }
        return null;
    }

    /**
     * Get the current user's highest role code from the JWT token in the request.
     *
     * @param request the HTTP request
     * @return the role code, or null if token is missing/invalid
     */
    public Integer getCurrentRoleCode(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            return jwtUtils.getRoleCode(token);
        }
        return null;
    }
}
