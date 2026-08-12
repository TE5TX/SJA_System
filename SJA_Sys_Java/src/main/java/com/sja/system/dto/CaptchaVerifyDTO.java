package com.sja.system.dto;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CaptchaVerifyDTO {

    @NotBlank(message = "验证码编号不能为空")
    private String id;

    @Valid
    @NotNull(message = "验证码轨迹不能为空")
    private ImageCaptchaTrack data;
}
