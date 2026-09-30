package com.sja.system.controller.system;

import com.sja.system.application.dto.system.StorageSettingUpdateDTO;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.Result;
import com.sja.system.service.storage.StorageApplicationService;
import com.sja.system.service.storage.StorageLocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 存储目录管理的HTTP适配器，不处理路径校验和密码复核业务。 */
// 202609301034 - 完成了管理员和秘书长文件保存路径的查询、检测与修改接口。
@RestController
@RequestMapping("/api/v1/system/storage")
@RequiredArgsConstructor
public class StorageSettingsController {

    private final StorageApplicationService applicationService;

    @GetMapping
    @PreAuthorize("@roleAuthorization.canManageStorage(authentication)")
    public Result<List<StorageLocationService.Setting>> settings(CurrentActor actor) {
        return Result.success(applicationService.settings(actor));
    }

    @PostMapping("/check")
    @PreAuthorize("@roleAuthorization.canManageStorage(authentication)")
    public Result<StorageLocationService.Setting> check(@RequestBody @Valid CheckRequest request,
                                                       CurrentActor actor) {
        return Result.success(applicationService.check(request.category(), request.relativeDir(), actor));
    }

    @PutMapping
    @PreAuthorize("@roleAuthorization.canManageStorage(authentication)")
    public Result<StorageLocationService.Setting> update(@RequestBody @Valid StorageSettingUpdateDTO dto,
                                                        CurrentActor actor) {
        return Result.success(applicationService.update(dto, actor));
    }

    /** 仅承载请求字段；具体校验在应用服务和领域服务中完成。 */
    public record CheckRequest(@NotBlank String category, @NotBlank String relativeDir) { }
}
