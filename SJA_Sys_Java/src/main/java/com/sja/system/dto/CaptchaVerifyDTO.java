package com.sja.system.dto;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CaptchaVerifyDTO {

    @NotBlank(message = "验证码编号不能为空")
    private String id;

    @Valid
    @NotNull(message = "验证码轨迹不能为空")
    private ImageCaptchaTrack data;
}
