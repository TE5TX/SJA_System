package com.sja.system.controller;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import com.sja.system.dto.CaptchaVerifyDTO;
import com.sja.system.service.LoginCaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/captcha")
@RequiredArgsConstructor
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
