package com.sja.system.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT authentication filter that runs once per request.
 * Extracts the Bearer token from the Authorization header, validates it,
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

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            try {
                String username = jwtUtils.getUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Build authorities from the JWT roleCode claim as a fallback.
                // The UserDetailsService already loads full authorities, but we
                // ensure the ROLE_ prefix authority is present.
                Integer roleCode = jwtUtils.getRoleCode(token);
                List<SimpleGrantedAuthority> authorities = userDetails.getAuthorities().stream()
                        .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                        .collect(Collectors.toList());

                // Ensure ROLE_{roleCode} is present
                String roleAuthority = "ROLE_" + roleCode;
                boolean hasRoleAuth = authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals(roleAuthority));
                if (!hasRoleAuth) {
                    authorities.add(new SimpleGrantedAuthority(roleAuthority));
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("Cannot set user authentication in security context: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the raw JWT string from the Authorization header.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
