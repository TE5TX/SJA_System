package com.sja.system.application.vo.user;

import java.util.List;

/** Final roster import accounting; failures contain only locating fields. */
// 202609181147 - 完成了人员名单导入的成功、跳过和中止汇总模型。
public record UserRosterImportResultVO(int totalCount, int importedCount, int skippedCount,
                                       int unprocessedCount, boolean aborted, String abortReason,
                                       List<Failure> failures, String failureReportBase64) {
    public record Failure(int rowNumber, String realName, String studentNo, String reason) {
    }
}
