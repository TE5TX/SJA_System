package com.sja.system.dal.mysql.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 五类稿费表归一化后的只读查询结果。
 */
@Data
public class FeeListRow {

    private Long id;
    private String workType;
    private String title;
    private String platform;
    private LocalDate publishDate;
    private BigDecimal totalAmount;
    private String auditStatus;
    private Integer isPublished;
    private LocalDateTime createTime;
    private Long submitterId;
}
