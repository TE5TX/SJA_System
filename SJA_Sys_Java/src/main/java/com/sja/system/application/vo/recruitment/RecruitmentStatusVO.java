package com.sja.system.application.vo.recruitment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecruitmentStatusVO {

    private boolean published;

    private LocalDateTime publishedAt;

    private LocalDateTime publishStartAt;

    private LocalDateTime publishEndAt;

    /** 当前时刻是否实际允许提交注册。 */
    private boolean acceptingRegistration;

    private LocalDateTime updatedAt;
}
