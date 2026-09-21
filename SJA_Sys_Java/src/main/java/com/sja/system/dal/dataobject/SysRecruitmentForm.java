package com.sja.system.dal.dataobject;

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

    // 202609011534 - 公开注册的可调整开放时间段。
    private LocalDateTime publishStartAt;

    private LocalDateTime publishEndAt;

    private Long updatedBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
