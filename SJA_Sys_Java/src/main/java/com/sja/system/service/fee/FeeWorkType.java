package com.sja.system.service.fee;

import com.sja.system.common.ApiException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 稿费作品类型及其所属部门的统一定义。
 */
// 202609201644 - 完成了稿费作品类型字符串和部门映射的集中管理。
public enum FeeWorkType {

    EDITORIAL("EDITORIAL", "EDITORIAL", "采编部"),
    PHOTO("PHOTO", "PHOTOGRAPHY", "摄影部"),
    VIDEO("VIDEO", "VIDEO", "视频部"),
    VISUAL("VISUAL", "VISUAL", "视觉部"),
    SUBSIDY("SUBSIDY", "MANAGEMENT", "月度补贴");

    private final String code;
    private final String departmentCode;
    private final String departmentName;

    FeeWorkType(String code, String departmentCode, String departmentName) {
        this.code = code;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
    }

    public String code() {
        return code;
    }

    public String departmentCode() {
        return departmentCode;
    }

    public String departmentName() {
        return departmentName;
    }

    public static FeeWorkType require(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("不支持的作品类型: " + code));
    }

    public static Set<String> codesForDepartments(Set<String> departmentCodes) {
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(values())
                .filter(type -> departmentCodes.contains(type.departmentCode))
                .map(FeeWorkType::code)
                .forEach(result::add);
        return result;
    }

    public static Set<String> allDepartmentCodes() {
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(values()).map(FeeWorkType::departmentCode).forEach(result::add);
        return result;
    }
}
