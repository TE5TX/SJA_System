package com.sja.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("biz_visual_work")
public class BizVisualWork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private Integer authorCount;

    private BigDecimal authorAvgFee;

    private LocalDate publishDate;

    private String articleLink;

    private Integer isPublished;

    private Integer isSpecialNode;

    private Integer specialMultiplier;

    private BigDecimal totalAmount;

    private String auditStatus;

    private Integer isValid;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
