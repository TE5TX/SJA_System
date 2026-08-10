package com.sja.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("biz_monthly_subsidy")
public class BizMonthlySubsidy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submitterId;

    private String title;

    private String category;

    private String subsidyMonth;

    private BigDecimal totalAmount;

    private String auditStatus;

    private Integer isValid;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
