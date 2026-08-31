package com.sja.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_recruitment_form")
public class SysRecruitmentForm {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Integer isPublished;

    private Long publishedBy;

    private LocalDateTime publishedAt;

    private Long updatedBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
