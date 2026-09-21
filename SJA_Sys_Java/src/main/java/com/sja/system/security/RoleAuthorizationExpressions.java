package com.sja.system.security;

import com.sja.system.authorization.RoleAuthorizationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 为方法安全注解提供集中角色策略适配。
 *
 * <p>本类只负责把Spring Security身份转换为角色码；业务授权规则仍位于独立授权包中。</p>
 */
@Component("roleAuthorization")
// 202609201146 - 修复了Controller安全注解直接硬编码角色码的问题。
public class RoleAuthorizationExpressions {

    public boolean canImportUsers(Authentication authentication) {
        return hasAuthorizedRole(authentication, RoleAuthorizationPolicy::canImportUsers);
    }

    public boolean canManageRecruitment(Authentication authentication) {
        return hasAuthorizedRole(authentication, RoleAuthorizationPolicy::canManageRecruitment);
    }

    private boolean hasAuthorizedRole(Authentication authentication, Predicate<Integer> policy) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(this::parseRoleCode)
                .filter(Objects::nonNull)
                .anyMatch(policy);
    }

    private Integer parseRoleCode(String authority) {
        try {
            return Integer.valueOf(authority.substring("ROLE_".length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
