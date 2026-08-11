package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class MonthlyFeeExportDTO {

    @NotBlank(message = "导出月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "导出月份格式必须为yyyy-MM")
    private String month;
}
