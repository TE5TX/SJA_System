package com.sja.system.controller;

import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeBatchAuditDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.dto.MonthlyFeeExportDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.FeeService;
import com.sja.system.service.AsyncTaskQueueService;
import com.sja.system.service.MonthlyFeeExportService;
import com.sja.system.service.UserService;
import com.sja.system.vo.FeeContributorVO;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;
import com.sja.system.vo.AsyncTaskAcceptedVO;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
    private final MonthlyFeeExportService monthlyFeeExportService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final AsyncTaskQueueService asyncTaskQueueService;

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
    public ResponseEntity<Result<AsyncTaskAcceptedVO>> submitFee(
            @RequestBody @Valid FeeSubmitDTO dto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未授权"));
        }

        // 202608192148 - 修复了稿费集中提交时同步计算和写库阻塞 Web 请求线程的问题。
        AsyncTaskAcceptedVO task = asyncTaskQueueService.enqueueFee(
                userId, dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success("稿费任务已进入队列", task));
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
            @RequestParam(required = false) String contributorName,
            // 202609091535 - 完成了稿费列表创建日期查询参数的接入。
            @RequestParam(required = false) String createDate,
            // 202609091610 - 完成了日期区间查询参数的接入，保留旧客户端单日/月参数。
            @RequestParam(required = false) String publishStartDate,
            @RequestParam(required = false) String publishEndDate,
            @RequestParam(required = false) String createStartDate,
            @RequestParam(required = false) String createEndDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {

        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        PageResult<FeeListVO> result = feeService.getFeeList(
                deptCode, month, status, contributorName, createDate, publishStartDate, publishEndDate,
                createStartDate, createEndDate, pageNum, pageSize, operatorId, roleCode);
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

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('fee:submit', 'fee:audit', 'fee:edit')")
    public Result<List<BigDecimal>> calculateFee(@RequestBody @Valid FeeSubmitDTO dto,
                                                  HttpServletRequest request) {
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            throw ApiException.forbidden("只有管理员和秘书长可以计算并查看稿费金额");
        }
        // 202608111533 - 修复了管理员编辑视频计费字段时参与人员稿费不能实时预览的问题。
        return Result.success(feeService.calculateFee(dto));
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

    @PutMapping("/audit/batch")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> batchAuditFee(@RequestBody @Valid FeeBatchAuditDTO dto,
                                      HttpServletRequest request) {
        Long auditorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (auditorId == null) {
            return Result.error(401, "未授权");
        }
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            return Result.error(403, "只有管理员或秘书长可以批量审核稿费");
        }
        // 202608131617 - 为稿费批量审核增加了当前用户密码二次校验。
        userService.verifyCurrentPassword(auditorId, dto.getCurrentPassword());
        // 202608111533 - 修复了稿费审核页缺少批量通过和批量驳回能力的问题。
        feeService.batchAuditFee(dto, auditorId,
                securityUtils.getCurrentUsernameFromToken(request), String.valueOf(roleCode));
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

    @PostMapping("/monthly-summary/export")
    @PreAuthorize("hasAuthority('fee:export')")
    public void exportMonthlySummary(@RequestBody @Valid MonthlyFeeExportDTO dto,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) throw ApiException.unauthorized("未授权");
        // 202608111715 - 新增了月份稿费发放名单导出接口。
        monthlyFeeExportService.export(dto, operatorId, roleCode,
                securityUtils.getCurrentUsernameFromToken(request), response);
    }
}
