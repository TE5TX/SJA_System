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
@TableName("biz_photography_work")
public class BizPhotographyWork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String platform;

    private Integer photoCount;

    private Integer readCount;

    private LocalDate publishDate;

    private String articleLink;

    private Integer isPublished;

    private BigDecimal totalAmount;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
