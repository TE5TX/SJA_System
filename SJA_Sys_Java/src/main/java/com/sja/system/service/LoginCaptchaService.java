package com.sja.system.service;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginCaptchaService {

    private final ImageCaptchaApplication captchaApplication;

    public ApiResponse<ImageCaptchaVO> generate() {
        return captchaApplication.generateCaptcha(CaptchaTypeConstant.SLIDER);
    }

    public ApiResponse<Map<String, String>> verify(String id, ImageCaptchaTrack track) {
        ApiResponse<?> result = captchaApplication.matching(id, track);
        if (!result.isSuccess()) {
            return ApiResponse.of(result.getCode(), result.getMsg(), null);
        }
        return ApiResponse.ofSuccess(Collections.singletonMap("token", id));
    }

    public boolean consume(String token) {
        if (!(captchaApplication instanceof SecondaryVerificationApplication)) {
            throw new IllegalStateException("验证码二次校验未启用");
        }
        return ((SecondaryVerificationApplication) captchaApplication).secondaryVerification(token);
    }
}
