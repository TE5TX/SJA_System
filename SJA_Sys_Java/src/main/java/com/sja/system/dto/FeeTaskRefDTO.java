package com.sja.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

@Data
public class FeeTaskRefDTO {

    @NotBlank(message = "作品类型不能为空")
    @Pattern(regexp = "EDITORIAL|PHOTO|VIDEO|VISUAL|SUBSIDY", message = "作品类型不正确")
    private String workType;

    @NotNull(message = "任务ID不能为空")
    @Positive(message = "任务ID必须大于0")
    private Long id;
}
