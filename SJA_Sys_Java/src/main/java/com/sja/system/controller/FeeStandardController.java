package com.sja.system.controller;

import com.sja.system.common.ApiException;
import com.sja.system.common.Result;
import com.sja.system.dto.FeeStandardUpdateDTO;
import com.sja.system.entity.BizFeeStandard;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.FeeStandardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fee/standards")
@RequiredArgsConstructor
public class FeeStandardController {

    private final FeeStandardService feeStandardService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<List<BizFeeStandard>> list(HttpServletRequest request) {
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            throw ApiException.forbidden("只有管理员或秘书长可以查看稿费标准");
        }
        return Result.success(feeStandardService.list());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid FeeStandardUpdateDTO dto,
                               HttpServletRequest request) {
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            throw ApiException.forbidden("只有管理员或秘书长可以修改稿费标准");
        }
        feeStandardService.update(id, dto,
                securityUtils.getCurrentUserId(request),
                securityUtils.getCurrentUsernameFromToken(request),
                String.valueOf(roleCode), request.getRemoteAddr());
        return Result.success();
    }
}
