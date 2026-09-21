package com.sja.system.application.dto.fee;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class FeeAuditDTO {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    private String opinion;

}
