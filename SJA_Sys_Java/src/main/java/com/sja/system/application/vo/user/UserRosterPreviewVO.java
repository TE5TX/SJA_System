package com.sja.system.application.vo.user;

import java.util.List;

/** Non-sensitive row-level preview for the ten-column personnel roster. */
// 202609181104 - 完成了人员名单逐行预览与跳过原因的响应模型。
public record UserRosterPreviewVO(String importToken, Long expiresIn, int total,
                                  int validCount, int invalidCount, String roleName,
                                  List<String> ignoredHeaders, List<Row> rows) {
    public record Row(int rowNumber, String realName, String studentNo,
                      List<String> deptNames, boolean valid, List<String> errors) {
    }
}
