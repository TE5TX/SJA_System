package com.sja.system.dto;

import java.util.List;

/** Short-lived server-owned candidates; never returned to the browser. */
// 202609181445 - 完成了九列名单有效候选行的操作者绑定快照模型。
public record UserRosterSnapshot(Long operatorId, Long roleId, int totalCount,
                                 List<Candidate> candidates, List<SkippedRow> skippedRows) {
    public record Candidate(int rowNumber, String realName, String studentNo, String college,
                            String className, String phone, String idCard, String dormAddress,
                            String bankCard, List<Long> deptIds) {
    }

    /** Preview-invalid rows are preserved so the final result can account for every source row. */
    // 202609181147 - 保存预览跳过行以保证确认结果数量与原始名单一致。
    public record SkippedRow(int rowNumber, String realName, String studentNo, String reason) {
    }
}
