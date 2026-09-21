package com.sja.system.service.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 集中角色授权策略的权限矩阵测试。 */
class RoleAuthorizationPolicyTest {

    @Test
    void sensitiveFeePermissionsFollowTheCentralMatrix() {
        assertTrue(RoleAuthorizationPolicy.canManageFeeAmounts(KnownRole.ADMINISTRATOR.code()));
        assertTrue(RoleAuthorizationPolicy.canManageFeeAmounts(KnownRole.SECRETARY_GENERAL.code()));
        assertFalse(RoleAuthorizationPolicy.canManageFeeAmounts(KnownRole.CHAIRMAN.code()));
        assertFalse(RoleAuthorizationPolicy.canManageFeeAmounts(99));
    }

    @Test
    void globalInformationScopeIsConsistentAcrossServices() {
        assertTrue(RoleAuthorizationPolicy.canViewAllPersonalInformation(KnownRole.ADMINISTRATOR.code()));
        assertTrue(RoleAuthorizationPolicy.canViewAllPersonalInformation(KnownRole.CHAIRMAN.code()));
        assertTrue(RoleAuthorizationPolicy.canViewAllPersonalInformation(KnownRole.TEACHER.code()));
        assertTrue(RoleAuthorizationPolicy.canViewAllPersonalInformation(KnownRole.SECRETARY_GENERAL.code()));
        assertFalse(RoleAuthorizationPolicy.canViewAllPersonalInformation(KnownRole.MINISTER.code()));
    }

    @Test
    void roleAssignmentUsesExplicitLevelsInsteadOfNumericComparison() {
        assertTrue(RoleAuthorizationPolicy.canAssignRole(
                KnownRole.ADMINISTRATOR.code(), KnownRole.CHAIRMAN.code()));
        assertFalse(RoleAuthorizationPolicy.canAssignRole(
                KnownRole.SECRETARY_GENERAL.code(), KnownRole.TEACHER.code()));
        assertTrue(RoleAuthorizationPolicy.canAssignRosterRole(
                KnownRole.SECRETARY_GENERAL.code(), KnownRole.FORMAL_MEMBER.code()));
        assertFalse(RoleAuthorizationPolicy.canAssignRosterRole(
                KnownRole.SECRETARY_GENERAL.code(), KnownRole.VISITOR.code()));
        // 202609201146 - 验证未知角色默认拒绝敏感授权，避免新增角色意外越权。
        assertFalse(RoleAuthorizationPolicy.canAssignRole(KnownRole.ADMINISTRATOR.code(), 99));
    }
}
