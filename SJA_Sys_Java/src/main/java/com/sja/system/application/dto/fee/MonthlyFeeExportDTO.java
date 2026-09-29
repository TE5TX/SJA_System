package com.sja.system.application.dto.fee;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class MonthlyFeeExportDTO {

    // 202609100927 - 完成了月份稿费名单起止月份参数的校验。
    @NotBlank(message = "开始月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "开始月份格式必须为yyyy-MM")
    private String startMonth;

    @NotBlank(message = "结束月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "结束月份格式必须为yyyy-MM")
    private String endMonth;

    /** 202609281642 - 预览后导出时提交清单指纹；直接导出时留空。 */
    @Pattern(regexp = "^[0-9a-f]{64}$", message = "清单指纹格式不正确")
    private String fingerprint;
}
