package com.sja.system.authorization;

import java.util.Arrays;
import java.util.Optional;

/**
 * 系统内具有明确业务语义的预置角色。
 *
 * <p>角色码只是稳定业务标识，不能直接使用数值大小推断权限高低。</p>
 */
// 202609201146 - 完成了预置角色码的集中定义，消除业务代码中的魔法数字。
public enum KnownRole {
    ADMINISTRATOR(0, 100, false),
    CHAIRMAN(1, 90, true),
    TEACHER(2, 80, false),
    SECRETARY_GENERAL(3, 70, true),
    MINISTER(4, 60, true),
    WECHAT_EDITOR(5, 40, true),
    LAYOUT_EDITOR(6, 40, true),
    PROOFREADER(7, 40, true),
    INTERN_FIRST(8, 20, true),
    INTERN_SECOND(9, 20, true),
    RETAINED_MEMBER(10, 30, true),
    PATROL_LEADER(11, 30, true),
    PATROL_MEMBER(12, 20, true),
    VISITOR(13, 0, false),
    FORMAL_MEMBER(14, 30, true);

    private final int code;
    private final int assignmentLevel;
    private final boolean monthlyMember;

    KnownRole(int code, int assignmentLevel, boolean monthlyMember) {
        this.code = code;
        this.assignmentLevel = assignmentLevel;
        this.monthlyMember = monthlyMember;
    }

    public int code() {
        return code;
    }

    public int assignmentLevel() {
        return assignmentLevel;
    }

    public boolean isMonthlyMember() {
        return monthlyMember;
    }

    public String authority() {
        return "ROLE_" + code;
    }

    /** 未识别的新角色返回空值，由敏感权限策略按拒绝处理。 */
    public static Optional<KnownRole> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(role -> role.code == code).findFirst();
    }
}
