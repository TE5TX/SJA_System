package com.sja.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecruitmentStatusUpdateDTO {

    @NotNull(message = "发布状态不能为空")
    private Boolean published;
}
