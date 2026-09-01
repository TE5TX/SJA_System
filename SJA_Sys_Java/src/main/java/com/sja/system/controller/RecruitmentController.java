package com.sja.system.controller;

import com.sja.system.common.ApiException;
import com.sja.system.common.Result;
import com.sja.system.dto.RecruitmentRegistrationDTO;
import com.sja.system.dto.RecruitmentStatusUpdateDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.LoginCaptchaService;
import com.sja.system.service.RecruitmentService;
import com.sja.system.vo.RecruitmentStatusVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/recruitment")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final LoginCaptchaService loginCaptchaService;
    private final SecurityUtils securityUtils;

    @GetMapping("/status")
    public Result<RecruitmentStatusVO> getStatus() {
        return Result.success(recruitmentService.getStatus());
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> register(
            @RequestPart("data") @Valid RecruitmentRegistrationDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        if (!loginCaptchaService.consume(dto.getCaptchaToken().trim())) {
            throw ApiException.badRequest("验证码无效或已过期，请重新验证");
        }
        recruitmentService.register(dto, photo);
        return Result.success("注册成功", null);
    }

    @PutMapping("/manage/status")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public Result<RecruitmentStatusVO> updateStatus(
            @RequestBody @Valid RecruitmentStatusUpdateDTO dto,
            HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            throw ApiException.unauthorized("未授权");
        }
        return Result.success(recruitmentService.updateStatus(
                dto.getPublished(), dto.getPublishStartAt(), dto.getPublishEndAt(), operatorId,
                securityUtils.getCurrentUsernameFromToken(request), roleCode));
    }
}
