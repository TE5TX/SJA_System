package com.sja.system.controller;

import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.FeeService;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final SecurityUtils securityUtils;

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

        // Get real name and dept code from the token context
        // In a full implementation, these would come from the user profile
        String realName = username;
        String deptCode = "";

        feeService.submitFee(userId, username, realName, deptCode, dto);
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
            @RequestParam(defaultValue = "10") Integer pageSize) {

        PageResult<FeeListVO> result = feeService.getFeeList(deptCode, month, status, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * Get detailed information for a specific fee record.
     */
    @GetMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAuthority('fee:view')")
    public Result<FeeDetailVO> getFeeDetail(@PathVariable String workType,
                                            @PathVariable Long id) {
        FeeDetailVO detail = feeService.getFeeDetail(id, workType);
        return Result.success(detail);
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

        feeService.auditFee(id, workType, dto, auditorId, auditorName, auditorRole);
        return Result.success();
    }

    /**
     * Export fee records to an Excel file.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('fee:export')")
    public void exportFee(@RequestParam(required = false) String deptCode,
                          @RequestParam(required = false) String month,
                          HttpServletResponse response) {
        feeService.exportFee(deptCode, month, response);
    }
}
