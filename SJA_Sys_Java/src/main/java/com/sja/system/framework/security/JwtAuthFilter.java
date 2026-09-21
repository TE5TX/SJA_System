package com.sja.system.framework.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT authentication filter that runs once per request.
 * 从 HttpOnly Cookie 提取访问令牌、验证令牌，
 * and sets the Spring Security authentication context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtUtils.validateAccessToken(token)) {
            try {
                String username = jwtUtils.getUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!(userDetails instanceof SjaUserPrincipal)) {
                    throw new IllegalStateException("Unsupported authenticated principal");
                }
                SjaUserPrincipal principal = (SjaUserPrincipal) userDetails;
                if (!principal.getUserId().equals(jwtUtils.getUserId(token))
                        || !principal.getTokenVersion().equals(jwtUtils.getTokenVersion(token))) {
                    throw new IllegalStateException("Token has been revoked");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("Cannot set user authentication in security context: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从仅由服务端写入的 HttpOnly Cookie 提取访问令牌。
     */
    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        // 202609201535 - 修复了前端必须读取Access Token并拼装Authorization请求头的问题。
        return java.util.Arrays.stream(request.getCookies())
                .filter(cookie -> AuthenticationCookieNames.ACCESS.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
