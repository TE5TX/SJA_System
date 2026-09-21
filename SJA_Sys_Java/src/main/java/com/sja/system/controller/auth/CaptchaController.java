package com.sja.system.controller.auth;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import com.sja.system.application.dto.auth.CaptchaVerifyDTO;
import com.sja.system.service.auth.LoginCaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 登录验证码协议适配器。
 */
@RestController
@RequestMapping("/api/v1/auth/captcha")
@RequiredArgsConstructor
// 202609211059 - 核查确认验证码Controller仅承担请求与响应转换职责。
public class CaptchaController {

    private final LoginCaptchaService loginCaptchaService;

    @PostMapping("/generate")
    public ApiResponse<ImageCaptchaVO> generate() {
        return loginCaptchaService.generate();
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, String>> verify(@RequestBody @Valid CaptchaVerifyDTO dto) {
        return loginCaptchaService.verify(dto.getId(), dto.getData());
    }
}
