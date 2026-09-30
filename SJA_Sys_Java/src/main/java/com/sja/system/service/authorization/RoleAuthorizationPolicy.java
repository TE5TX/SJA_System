package com.sja.system.service.authorization;

import java.util.EnumSet;
import java.util.Set;

/**
 * 集中维护角色对应的业务授权和数据范围。
 *
 * <p>服务层只调用具有业务语义的方法，不再自行拼装角色码集合。未知角色默认不具备敏感权限。</p>
 */
// 202609201146 - 完成了跨服务角色授权规则的统一收口。
public final class RoleAuthorizationPolicy {

    private static final Set<KnownRole> FEE_GLOBAL_VIEWERS = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.CHAIRMAN, KnownRole.TEACHER,
            KnownRole.SECRETARY_GENERAL, KnownRole.MINISTER);
    private static final Set<KnownRole> MANAGEMENT_ROLES = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.CHAIRMAN,
            KnownRole.TEACHER, KnownRole.SECRETARY_GENERAL);
    private static final Set<KnownRole> FEE_AMOUNT_MANAGERS = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.SECRETARY_GENERAL);
    private static final Set<KnownRole> FEE_TASK_EDITORS = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.SECRETARY_GENERAL, KnownRole.MINISTER);
    private static final Set<KnownRole> GLOBAL_INFORMATION_VIEWERS = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.CHAIRMAN,
            KnownRole.TEACHER, KnownRole.SECRETARY_GENERAL);
    private static final Set<KnownRole> USER_IMPORT_MANAGERS = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.CHAIRMAN, KnownRole.SECRETARY_GENERAL);
    // 202609301034 - 完成了文件保存路径仅管理员和秘书长可查看、配置的独立授权策略。
    private static final Set<KnownRole> STORAGE_MANAGERS = EnumSet.of(
            KnownRole.ADMINISTRATOR, KnownRole.SECRETARY_GENERAL);

    private RoleAuthorizationPolicy() {
    }

    public static boolean canViewAllFeeTasks(Integer roleCode) {
        return belongsTo(roleCode, FEE_GLOBAL_VIEWERS);
    }

    public static boolean canSubmitAcrossDepartments(Integer roleCode) {
        return belongsTo(roleCode, MANAGEMENT_ROLES);
    }

    public static boolean canManageFeeAmounts(Integer roleCode) {
        return belongsTo(roleCode, FEE_AMOUNT_MANAGERS);
    }

    public static boolean canEditFeeTasks(Integer roleCode) {
        return belongsTo(roleCode, FEE_TASK_EDITORS);
    }

    public static boolean canViewAllPersonalInformation(Integer roleCode) {
        return belongsTo(roleCode, GLOBAL_INFORMATION_VIEWERS);
    }

    public static boolean canManageFeeStandards(Integer roleCode) {
        return canManageFeeAmounts(roleCode);
    }

    public static boolean canViewOperationLogs(Integer roleCode) {
        return canManageFeeAmounts(roleCode);
    }

    public static boolean canManageMonthlyFeeExport(Integer roleCode) {
        return canManageFeeAmounts(roleCode);
    }

    public static boolean canManageRecruitment(Integer roleCode) {
        return belongsTo(roleCode, USER_IMPORT_MANAGERS);
    }

    public static boolean canImportUsers(Integer roleCode) {
        return belongsTo(roleCode, USER_IMPORT_MANAGERS);
    }

    /** 文件存储路径的查询、检测与修改共用同一权限边界。 */
    public static boolean canManageStorage(Integer roleCode) {
        return belongsTo(roleCode, STORAGE_MANAGERS);
    }

    /** 使用显式授权等级判断角色分配，禁止再比较角色码数值大小。 */
    public static boolean canAssignRole(Integer operatorRoleCode, Integer targetRoleCode) {
        KnownRole operator = KnownRole.fromCode(operatorRoleCode).orElse(null);
        KnownRole target = KnownRole.fromCode(targetRoleCode).orElse(null);
        return operator != null && target != null
                && canImportUsers(operatorRoleCode)
                && operator.assignmentLevel() >= target.assignmentLevel();
    }

    /** 花名册导入只允许分配非核心管理层、非游客的团内职务。 */
    public static boolean canAssignRosterRole(Integer operatorRoleCode, Integer targetRoleCode) {
        KnownRole target = KnownRole.fromCode(targetRoleCode).orElse(null);
        return canAssignRole(operatorRoleCode, targetRoleCode)
                && target != null
                && target != KnownRole.ADMINISTRATOR
                && target != KnownRole.CHAIRMAN
                && target != KnownRole.TEACHER
                && target != KnownRole.SECRETARY_GENERAL
                && target != KnownRole.VISITOR;
    }

    public static boolean isMonthlyMemberRole(Integer roleCode) {
        return KnownRole.fromCode(roleCode).map(KnownRole::isMonthlyMember).orElse(false);
    }

    private static boolean belongsTo(Integer roleCode, Set<KnownRole> allowedRoles) {
        return KnownRole.fromCode(roleCode).map(allowedRoles::contains).orElse(false);
    }
}
