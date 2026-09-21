package com.sja.system.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAuthorizationExpressionsTest {

    private final RoleAuthorizationExpressions expressions = new RoleAuthorizationExpressions();

    @Test
    void evaluatesEveryRoleAuthorityInsteadOfOnlyOnePrimaryRole() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user", "", List.of(
                new SimpleGrantedAuthority("ROLE_10"),
                new SimpleGrantedAuthority("ROLE_3")));

        // 202609201146 - 验证多角色用户按角色合集执行方法安全判断。
        assertTrue(expressions.canImportUsers(authentication));
        assertTrue(expressions.canManageRecruitment(authentication));
    }

    @Test
    void rejectsUnknownOrMalformedRoleAuthorities() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "user", "", List.of(
                new SimpleGrantedAuthority("ROLE_UNKNOWN"),
                new SimpleGrantedAuthority("ROLE_99")));

        assertFalse(expressions.canImportUsers(authentication));
        assertFalse(expressions.canManageRecruitment(authentication));
    }
}
