package com.sja.system.controller;

import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.FeeService;
import com.sja.system.service.UserService;
import com.sja.system.vo.FeeContributorVO;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * Fee (remuneration) management controller.
 * Handles submission, listing, detail, audit, and export of work fee records.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fee")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    /**
     * Return active users that can be selected as contributors. This deliberately
     * exposes only name and student number instead of the full user profile.
     */
    @GetMapping("/contributors")
    @PreAuthorize("hasAuthority('fee:contributors:view')")
    public Result<List<FeeContributorVO>> getContributors() {
        return Result.success(userService.queryActiveFeeContributors());
    }

    /**
     * Submit a new fee record for a piece of work.
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('fee:submit')")
    public Result<Void> submitFee(@RequestBody @Valid FeeSubmitDTO dto, HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        String username = securityUtils.getCurrentUsernameFromToken(request);

        if (userId == null) {
            return Result.error(401, "未授权");
        }

        String realName = username;
        Integer roleCode = securityUtils.getCurrentRoleCode(request);

        feeService.submitFee(userId, username, realName, roleCode, dto);
        return Result.success();
    }

    /**
     * List fee records with optional filters.
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('fee:view')")
    public Result<PageResult<FeeListVO>> getFeeList(
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {

        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        PageResult<FeeListVO> result = feeService.getFeeList(deptCode, month, status, pageNum, pageSize, operatorId, roleCode);
        return Result.success(result);
    }

    /**
     * Get detailed information for a specific fee record.
     */
    @GetMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAuthority('fee:view')")
    public Result<FeeDetailVO> getFeeDetail(@PathVariable String workType,
                                            @PathVariable Long id,
                                            HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        FeeDetailVO detail = feeService.getFeeDetail(id, workType, operatorId, roleCode);
        return Result.success(detail);
    }

    @PutMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAnyAuthority('fee:audit', 'fee:edit')")
    public Result<Void> updateFee(@PathVariable String workType,
                                  @PathVariable Long id,
                                  @RequestBody @Valid FeeSubmitDTO dto,
                                  HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        String operatorName = securityUtils.getCurrentUsernameFromToken(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        String operatorRole = roleCode != null ? String.valueOf(roleCode) : "";
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        feeService.updateFee(id, workType, dto, operatorId, operatorName, operatorRole, roleCode);
        return Result.success();
    }

    /**
     * Audit (approve/reject) a fee record.
     */
    @PutMapping("/audit/{workType}/{id}")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> auditFee(@PathVariable String workType,
                                 @PathVariable Long id,
                                 @RequestBody @Valid FeeAuditDTO dto,
                                 HttpServletRequest request) {
        Long auditorId = securityUtils.getCurrentUserId(request);
        String auditorName = securityUtils.getCurrentUsernameFromToken(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        String auditorRole = roleCode != null ? String.valueOf(roleCode) : "";

        if (auditorId == null) {
            return Result.error(401, "未授权");
        }
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            return Result.error(403, "只有管理员或秘书长可以审核和调整稿费金额");
        }

        feeService.auditFee(id, workType, dto, auditorId, auditorName, auditorRole);
        return Result.success();
    }

    @PutMapping("/audit/{workType}/{id}/reset")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> resetAudit(@PathVariable String workType,
                                   @PathVariable Long id,
                                   HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            return Result.error(403, "只有管理员或秘书长可以退回审批");
        }
        feeService.resetAudit(id, workType, operatorId,
                securityUtils.getCurrentUsernameFromToken(request), String.valueOf(roleCode));
        return Result.success();
    }

    @DeleteMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAnyAuthority('fee:audit', 'fee:edit')")
    public Result<Void> invalidateRejectedFee(@PathVariable String workType,
                                              @PathVariable Long id,
                                              HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        feeService.invalidateRejectedFee(id, workType, operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "", roleCode);
        return Result.success();
    }

    /**
     * Export fee records to an Excel file.
     */
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('fee:export')")
    public void exportFee(@RequestBody @Valid FeeExportDTO dto,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            throw ApiException.unauthorized("未授权");
        }
        feeService.exportFee(dto, operatorId, roleCode, response);
    }
}
