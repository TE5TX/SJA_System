package com.sja.system.controller;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.Result;
import com.sja.system.dto.RecruitmentRegistrationDTO;
import com.sja.system.dto.RecruitmentStatusUpdateDTO;
import com.sja.system.infrastructure.web.MultipartUploadedFile;
import com.sja.system.service.RecruitmentService;
import com.sja.system.vo.RecruitmentStatusVO;
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

/**
 * 招新注册与发布状态管理协议适配器。
 */
@RestController
@RequestMapping("/api/v1/recruitment")
@RequiredArgsConstructor
// 202609211059 - 完成了招新Controller身份解析下沉并确认公开注册流程无业务编排。
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    @GetMapping("/status")
    public Result<RecruitmentStatusVO> getStatus() {
        return Result.success(recruitmentService.getStatus());
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> register(
            @RequestPart("data") @Valid RecruitmentRegistrationDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        // 202609211059 - 完成了Web上传对象向应用层文件模型的适配。
        recruitmentService.register(dto, photo == null ? null : new MultipartUploadedFile(photo));
        return Result.success("注册成功", null);
    }

    @PutMapping("/manage/status")
    @PreAuthorize("@roleAuthorization.canManageRecruitment(authentication)")
    public Result<RecruitmentStatusVO> updateStatus(
            @RequestBody @Valid RecruitmentStatusUpdateDTO dto,
            CurrentActor actor) {
        // 202609211059 - 完成了招新管理Controller身份解析下沉。
        return Result.success(recruitmentService.updateStatus(
                dto.getPublished(), dto.getPublishStartAt(), dto.getPublishEndAt(), actor.userId(),
                actor.username(), actor.roleCode()));
    }
}
