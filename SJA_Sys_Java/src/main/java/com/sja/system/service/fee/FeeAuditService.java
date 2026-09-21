package com.sja.system.service.fee;

import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeBatchAuditDTO;
import com.sja.system.dto.FeeTaskRefDTO;
import com.sja.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 稿费审核、批量审核、退回待审和驳回作废用例服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
// 202609201644 - 将稿费审核状态流转和审核日志从综合服务中独立出来。
public class FeeAuditService {

    private static final DateTimeFormatter AUDIT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeeAccessService accessService;
    private final FeeWorkStateService stateService;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String workType, FeeAuditDTO dto, Long auditorId,
                      String auditorName, String auditorRole, Integer roleCode) {
        accessService.ensureCanManageAmount(roleCode, "审核和调整稿费金额");
        if (!"SUBMITTED".equals(stateService.auditStatus(workType, id))) {
            throw ApiException.badRequest("只有待审核的稿费任务可以审核");
        }
        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        String action = approved ? "审核通过" : "审核驳回";
        String opinion = dto.getOpinion() != null ? dto.getOpinion() : "";
        String remark = String.format("[%s] %s: %s | %s", auditorName, action, opinion,
                LocalDateTime.now().format(AUDIT_TIME));
        stateService.updateAudit(workType, id, approved ? "APPROVED" : "REJECTED", remark);
        operationLogService.log(auditorId, auditorName, auditorRole, "AUDIT", "FEE", id,
                String.format("%s [%s] id=%d: %s", action, workType, id, opinion), "");
        log.info("稿费审核完成: type={}, id={}, approved={}, auditor={}", workType, id, approved, auditorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(FeeBatchAuditDTO dto, Long auditorId, String auditorName,
                           String auditorRole, Integer roleCode) {
        accessService.ensureCanManageAmount(roleCode, "批量审核稿费");
        Set<String> keys = dto.getTasks().stream().map(this::taskKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (keys.size() != dto.getTasks().size()) throw ApiException.badRequest("批量审核任务不能重复");
        FeeAuditDTO audit = new FeeAuditDTO();
        audit.setApproved(dto.getApproved());
        audit.setOpinion(dto.getOpinion());
        for (FeeTaskRefDTO task : dto.getTasks()) {
            audit(task.getId(), task.getWorkType(), audit, auditorId, auditorName, auditorRole, roleCode);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reset(Long id, String workType, Long operatorId, String operatorName,
                      String operatorRole, Integer roleCode) {
        accessService.ensureCanManageAmount(roleCode, "退回审批");
        if (!"APPROVED".equals(stateService.auditStatus(workType, id))) {
            throw ApiException.badRequest("只有已通过的稿费任务可以退回待审核");
        }
        String remark = String.format("[%s] 退回待审核 | %s", operatorName,
                LocalDateTime.now().format(AUDIT_TIME));
        stateService.updateAudit(workType, id, "SUBMITTED", remark);
        operationLogService.log(operatorId, operatorName, operatorRole, "RESET_AUDIT", "FEE", id,
                String.format("将已通过稿费任务退回待审核 [%s] id=%d", workType, id), "");
    }

    @Transactional(rollbackFor = Exception.class)
    public void invalidateRejected(Long id, String workType, Long operatorId, String operatorName,
                                   String operatorRole, Integer roleCode) {
        accessService.ensureCanEdit(roleCode);
        accessService.ensureCanView(operatorId, roleCode, workType);
        if (!"REJECTED".equals(stateService.auditStatus(workType, id))) {
            throw ApiException.badRequest("只有已驳回的稿费任务可以删除");
        }
        stateService.updateValidity(workType, id, 0);
        operationLogService.log(operatorId, operatorName, operatorRole, "DELETE", "FEE", id,
                String.format("将已驳回稿费任务设为无效 [%s] id=%d", workType, id), "");
    }

    private String taskKey(FeeTaskRefDTO task) {
        return task.getWorkType() + ":" + task.getId();
    }
}
