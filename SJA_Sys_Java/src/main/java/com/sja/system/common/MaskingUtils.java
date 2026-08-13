package com.sja.system.common;

import com.sja.system.vo.UserVO;

// 202608101042 - 修复了人员列表、导出和操作日志暴露完整敏感信息的问题。
public final class MaskingUtils {

    private MaskingUtils() {
    }

    public static void maskUser(UserVO user) {
        if (user == null) return;
        user.setPhone(maskMiddle(user.getPhone(), 3, 4));
        user.setIdCard(maskMiddle(user.getIdCard(), 4, 4));
        user.setBankCard(maskMiddle(user.getBankCard(), 4, 4));
        user.setDormAddress(maskAddress(user.getDormAddress()));
    }

    public static String sanitizeLogDetail(String detail) {
        if (detail == null || detail.isEmpty()) return detail;
        String sanitized = detail.replaceAll(
                "(?i)(idCard|phone|bankCard|dormAddress)\\s*\\[[^\\]]*\\]",
                "$1 [已脱敏]");
        sanitized = sanitized.replaceAll(
                "(?i)(\\\"?(?:idCard|phone|bankCard|dormAddress)\\\"?\\s*:\\s*\\\")[^\\\"]*(\\\")",
                "$1已脱敏$2");
        sanitized = sanitized.replaceAll("(?<!\\d)1\\d{10}(?!\\d)", "手机号已脱敏");
        sanitized = sanitized.replaceAll("(?<!\\d)\\d{17}[0-9Xx](?!\\d)", "身份证号已脱敏");
        return sanitized.replaceAll("(?<!\\d)\\d{16,19}(?!\\d)", "银行卡号已脱敏");
    }

    private static String maskMiddle(String value, int prefix, int suffix) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= prefix + suffix) return "****";
        return value.substring(0, prefix) + "****" + value.substring(value.length() - suffix);
    }

    private static String maskAddress(String value) {
        if (value == null || value.isEmpty()) return value;
        int visible = Math.min(3, value.length());
        return value.substring(0, visible) + "***";
    }
}
