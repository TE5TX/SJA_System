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
@TableName("biz_video_work")
public class BizVideoWork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String videoTitle;

    private String category;

    private Integer videoDurationSec;

    private Integer likeCount;

    private String reprintStatus;

    private Integer isComplexEdit;

    private LocalDate publishDate;

    private String videoLink;

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
