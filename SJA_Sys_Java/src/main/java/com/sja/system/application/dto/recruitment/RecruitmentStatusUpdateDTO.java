package com.sja.system.application.dto.recruitment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecruitmentStatusUpdateDTO {

    @NotNull(message = "发布状态不能为空")
    private Boolean published;

    // 202609011534 - 管理员可设置注册开放区间；为空分别表示立即开放和不自动结束。
    private LocalDateTime publishStartAt;

    private LocalDateTime publishEndAt;
}
