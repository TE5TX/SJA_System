package com.sja.system.framework.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting security-related information from the
 * current authenticated principal. Authorization decisions never trust role
 * or user fields read directly from a request token.
 */
@Component
public class SecurityUtils {

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
     * Get the current user's ID from the verified SecurityContext principal.
     *
     * @param request the HTTP request
     * @return the user ID, or null if token is missing/invalid
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        SjaUserPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getUserId();
    }

    /**
     * Get the current user's username from the verified SecurityContext principal.
     *
     * @param request the HTTP request
     * @return the username, or null if token is missing/invalid
     */
    public String getCurrentUsernameFromToken(HttpServletRequest request) {
        SjaUserPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getUsername();
    }

    /**
     * Get the current user's highest role code loaded from the database.
     *
     * @param request the HTTP request
     * @return the role code, or null if token is missing/invalid
     */
    public Integer getCurrentRoleCode(HttpServletRequest request) {
        SjaUserPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getHighestRoleCode();
    }

    private SjaUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SjaUserPrincipal) {
            return (SjaUserPrincipal) authentication.getPrincipal();
        }
        return null;
    }
}
