package com.sja.system.application.dto.fee;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FeeStandardUpdateDTO {

    @NotNull(message = "规则值不能为空")
    @DecimalMin(value = "0", message = "规则值不能小于0")
    private BigDecimal ruleValue;

    private String description;

    private String remark;
}
