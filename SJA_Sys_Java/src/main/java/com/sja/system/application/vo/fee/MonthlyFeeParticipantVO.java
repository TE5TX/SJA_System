package com.sja.system.application.vo.fee;

import lombok.Data;

import java.math.BigDecimal;

/** 月份稿费任务参与人批量查询结果。 */
@Data
// 202609281642 - 完成了预览任务参与人信息的批量查询投影。
public class MonthlyFeeParticipantVO {
    private String workType;
    private Long workId;
    private String name;
    private String role;
    /** 202609281715 - 为任务详情提供每位参与人的实际保存金额。 */
    private BigDecimal feeAmount;
}
