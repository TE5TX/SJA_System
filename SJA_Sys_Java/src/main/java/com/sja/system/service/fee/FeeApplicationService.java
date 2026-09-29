package com.sja.system.service.fee;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.FeeExportFile;
import com.sja.system.common.PageResult;
import com.sja.system.application.dto.fee.FeeAuditDTO;
import com.sja.system.application.dto.fee.FeeBatchAuditDTO;
import com.sja.system.application.dto.fee.FeeExportDTO;
import com.sja.system.application.dto.fee.FeeListQueryDTO;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.application.dto.fee.MonthlyFeeExportDTO;
import com.sja.system.application.vo.async.AsyncTaskAcceptedVO;
import com.sja.system.application.vo.fee.FeeContributorVO;
import com.sja.system.application.vo.fee.FeeDetailVO;
import com.sja.system.application.vo.fee.FeeListVO;
import com.sja.system.application.vo.fee.MonthlyFeePreviewVO;
import com.sja.system.service.async.AsyncTaskQueueService;
import com.sja.system.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 稿费模块应用用例协调服务。
 *
 * <p>集中协调稿费门面、异步队列、人员密码复核和月份导出，Controller 只负责
 * HTTP 参数绑定与响应转换。具体计算、权限和持久化规则仍由各专业服务负责。</p>
 */
@Service
@RequiredArgsConstructor
// 202609211059 - 完成了稿费Controller中的跨服务编排、队列选择和密码复核下沉。
public class FeeApplicationService {

    private final FeeService feeService;
    private final MonthlyFeeExportService monthlyFeeExportService;
    private final UserService userService;
    private final AsyncTaskQueueService asyncTaskQueueService;

    public List<FeeContributorVO> contributors() {
        return userService.queryActiveFeeContributors();
    }

    public AsyncTaskAcceptedVO submit(FeeSubmitDTO dto, String idempotencyKey, CurrentActor actor) {
        return asyncTaskQueueService.enqueueFee(actor.userId(), dto, idempotencyKey);
    }

    public PageResult<FeeListVO> list(FeeListQueryDTO query, CurrentActor actor) {
        return feeService.getFeeList(query.getDeptCode(), query.getMonth(), query.getStatus(),
                query.getContributorName(), query.getCreateDate(), query.getPublishStartDate(),
                query.getPublishEndDate(), query.getCreateStartDate(), query.getCreateEndDate(),
                query.getPageNum(), query.getPageSize(), actor.userId(), actor.roleCode());
    }

    public FeeDetailVO detail(Long id, String workType, CurrentActor actor) {
        return feeService.getFeeDetail(id, workType, actor.userId(), actor.roleCode());
    }

    public void update(Long id, String workType, FeeSubmitDTO dto, CurrentActor actor) {
        feeService.updateFee(id, workType, dto, actor.userId(), actor.username(),
                actor.roleText(), actor.roleCode());
    }

    public List<BigDecimal> calculate(FeeSubmitDTO dto, CurrentActor actor) {
        return feeService.calculateFee(dto, actor.roleCode());
    }

    public void audit(Long id, String workType, FeeAuditDTO dto, CurrentActor actor) {
        feeService.auditFee(id, workType, dto, actor.userId(), actor.username(),
                actor.roleText(), actor.roleCode());
    }

    /** 密码复核与批量审核属于同一个应用用例，不允许由 Controller 分步协调。 */
    public void batchAudit(FeeBatchAuditDTO dto, CurrentActor actor) {
        userService.verifyCurrentPassword(actor.userId(), dto.getCurrentPassword());
        feeService.batchAuditFee(dto, actor.userId(), actor.username(), actor.roleText(), actor.roleCode());
    }

    public void resetAudit(Long id, String workType, CurrentActor actor) {
        feeService.resetAudit(id, workType, actor.userId(), actor.username(),
                actor.roleText(), actor.roleCode());
    }

    public void invalidateRejected(Long id, String workType, CurrentActor actor) {
        feeService.invalidateRejectedFee(id, workType, actor.userId(), actor.username(),
                actor.roleText(), actor.roleCode());
    }

    public FeeExportFile export(FeeExportDTO dto, CurrentActor actor) {
        return feeService.exportFee(dto, actor.userId(), actor.roleCode());
    }

    public FeeExportFile exportMonthly(MonthlyFeeExportDTO dto, CurrentActor actor) {
        return monthlyFeeExportService.export(dto, actor.userId(), actor.roleCode(), actor.username());
    }

    /** 返回月份稿费预览；与导出使用相同的清单构建服务。 */
    public MonthlyFeePreviewVO previewMonthly(MonthlyFeeExportDTO dto, CurrentActor actor) {
        // 202609281642 - 完成了月份稿费预览用例的应用层转发。
        return monthlyFeeExportService.preview(dto, actor.roleCode());
    }
}
