package com.sja.system.controller.fee;

import com.sja.system.application.model.FeeExportFile;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.framework.web.support.DownloadResponseFactory;
import com.sja.system.application.dto.fee.FeeAuditDTO;
import com.sja.system.application.dto.fee.FeeBatchAuditDTO;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.application.dto.fee.FeeExportDTO;
import com.sja.system.application.dto.fee.FeeListQueryDTO;
import com.sja.system.application.dto.fee.MonthlyFeeExportDTO;
import com.sja.system.service.fee.FeeApplicationService;
import com.sja.system.application.vo.fee.FeeContributorVO;
import com.sja.system.application.vo.fee.FeeDetailVO;
import com.sja.system.application.vo.fee.FeeListVO;
import com.sja.system.application.vo.async.AsyncTaskAcceptedVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 稿费管理协议适配器，仅负责参数绑定、方法级鉴权和HTTP响应转换。
 */
@RestController
@RequestMapping("/api/v1/fee")
@RequiredArgsConstructor
// 202609211059 - 完成了稿费Controller业务编排下沉，仅保留HTTP协议适配职责。
public class FeeController {

    private final FeeApplicationService feeApplicationService;

    /** 返回稿费参与人员选择所需的最小人员信息。 */
    @GetMapping("/contributors")
    @PreAuthorize("hasAuthority('fee:contributors:view')")
    public Result<List<FeeContributorVO>> getContributors() {
        return Result.success(feeApplicationService.contributors());
    }

    /** 接收稿费提交请求并返回异步任务受理结果。 */
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('fee:submit')")
    public ResponseEntity<Result<AsyncTaskAcceptedVO>> submitFee(
            @RequestBody @Valid FeeSubmitDTO dto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            CurrentActor actor) {
        AsyncTaskAcceptedVO task = feeApplicationService.submit(dto, idempotencyKey, actor);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success("稿费任务已进入队列", task));
    }

    /** 按查询对象中的筛选条件分页查询稿费任务。 */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('fee:view')")
    public Result<PageResult<FeeListVO>> getFeeList(@Valid FeeListQueryDTO query, CurrentActor actor) {
        return Result.success(feeApplicationService.list(query, actor));
    }

    /** 查询指定作品的稿费详情。 */
    @GetMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAuthority('fee:view')")
    public Result<FeeDetailVO> getFeeDetail(@PathVariable String workType,
                                            @PathVariable Long id,
                                            CurrentActor actor) {
        return Result.success(feeApplicationService.detail(id, workType, actor));
    }

    @PutMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAnyAuthority('fee:audit', 'fee:edit')")
    public Result<Void> updateFee(@PathVariable String workType,
                                  @PathVariable Long id,
                                  @RequestBody @Valid FeeSubmitDTO dto,
                                  CurrentActor actor) {
        feeApplicationService.update(id, workType, dto, actor);
        return Result.success();
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('fee:submit', 'fee:audit', 'fee:edit')")
    public Result<List<BigDecimal>> calculateFee(@RequestBody @Valid FeeSubmitDTO dto,
                                                  CurrentActor actor) {
        return Result.success(feeApplicationService.calculate(dto, actor));
    }

    /** 审核指定稿费任务。 */
    @PutMapping("/audit/{workType}/{id}")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> auditFee(@PathVariable String workType,
                                 @PathVariable Long id,
                                 @RequestBody @Valid FeeAuditDTO dto,
                                 CurrentActor actor) {
        feeApplicationService.audit(id, workType, dto, actor);
        return Result.success();
    }

    @PutMapping("/audit/batch")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> batchAuditFee(@RequestBody @Valid FeeBatchAuditDTO dto,
                                      CurrentActor actor) {
        feeApplicationService.batchAudit(dto, actor);
        return Result.success();
    }

    @PutMapping("/audit/{workType}/{id}/reset")
    @PreAuthorize("hasAuthority('fee:audit')")
    public Result<Void> resetAudit(@PathVariable String workType,
                                   @PathVariable Long id,
                                   CurrentActor actor) {
        feeApplicationService.resetAudit(id, workType, actor);
        return Result.success();
    }

    @DeleteMapping("/detail/{workType}/{id}")
    @PreAuthorize("hasAnyAuthority('fee:audit', 'fee:edit')")
    public Result<Void> invalidateRejectedFee(@PathVariable String workType,
                                              @PathVariable Long id,
                                              CurrentActor actor) {
        feeApplicationService.invalidateRejected(id, workType, actor);
        return Result.success();
    }

    /** 导出稿费明细文件。 */
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('fee:export')")
    public ResponseEntity<byte[]> exportFee(@RequestBody @Valid FeeExportDTO dto,
                                             CurrentActor actor) {
        return toDownloadResponse(feeApplicationService.export(dto, actor));
    }

    @PostMapping("/monthly-summary/export")
    @PreAuthorize("hasAuthority('fee:export')")
    public ResponseEntity<byte[]> exportMonthlySummary(@RequestBody @Valid MonthlyFeeExportDTO dto,
                                                        CurrentActor actor) {
        return toDownloadResponse(feeApplicationService.exportMonthly(dto, actor));
    }

    /**
     * 将应用服务生成的文件转换为标准HTTP下载响应，不承载稿费业务逻辑。
     */
    private ResponseEntity<byte[]> toDownloadResponse(FeeExportFile file) {
        // 202609201621 - 稿费与月份稿费导出统一使用标准下载响应工厂。
        return DownloadResponseFactory.create(file);
    }
}
