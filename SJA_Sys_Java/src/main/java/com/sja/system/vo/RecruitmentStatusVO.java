package com.sja.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecruitmentStatusVO {

    private boolean published;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}
