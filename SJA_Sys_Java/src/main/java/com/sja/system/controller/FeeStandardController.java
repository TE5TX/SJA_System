package com.sja.system.controller;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.Result;
import com.sja.system.dto.FeeStandardUpdateDTO;
import com.sja.system.entity.BizFeeStandard;
import com.sja.system.service.FeeStandardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 稿费标准协议适配器。
 */
@RestController
@RequestMapping("/api/v1/fee/standards")
@RequiredArgsConstructor
// 202609211059 - 完成了稿费标准Controller身份解析下沉。
public class FeeStandardController {

    private final FeeStandardService feeStandardService;
    @GetMapping
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<List<BizFeeStandard>> list(CurrentActor actor) {
        return Result.success(feeStandardService.list(actor.roleCode()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid FeeStandardUpdateDTO dto,
                               CurrentActor actor, HttpServletRequest request) {
        // 202609211059 - 完成了稿费标准Controller身份拼装下沉。
        feeStandardService.update(id, dto, actor.userId(), actor.username(),
                actor.roleText(), request.getRemoteAddr(), actor.roleCode());
        return Result.success();
    }
}
