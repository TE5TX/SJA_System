package com.sja.system.application.vo.fee;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MonthlyFeeComponentVO {

    private Long userId;
    private BigDecimal feeAmount;
    private String subsidyCategory;
    /** 202609281642 - 增加参与记录主键，将同金额的不同任务逐笔对应。 */
    private Long contributorId;
    private String workType;
    private Long workId;
    private String title;
    private LocalDate publishDate;
    private Integer isPublished;
    private String subsidyMonth;
}
