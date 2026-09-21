package com.sja.system.service.fee;

import com.sja.system.application.model.FeeExportFile;
import com.sja.system.common.PageResult;
import com.sja.system.application.dto.fee.FeeAuditDTO;
import com.sja.system.application.dto.fee.FeeBatchAuditDTO;
import com.sja.system.application.dto.fee.FeeExportDTO;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.service.fee.FeeService;
import com.sja.system.service.fee.FeeAuditService;
import com.sja.system.service.fee.FeeCommandService;
import com.sja.system.service.fee.FeeExportService;
import com.sja.system.service.fee.FeeQueryService;
import com.sja.system.application.vo.fee.FeeDetailVO;
import com.sja.system.application.vo.fee.FeeListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 稿费管理稳定门面。
 *
 * <p>仅保持原有接口契约并把请求分发给查询、写入、审核和导出用例服务，
 * 不再直接访问 Mapper 或承载作品类型字段转换。</p>
 */
@Service
@RequiredArgsConstructor
// 202609201644 - 将原综合稿费服务收缩为无业务逻辑的应用门面。
public class FeeServiceImpl implements FeeService {
    private final FeeQueryService feeQueryService;
    private final FeeExportService feeExportService;
    private final FeeAuditService feeAuditService;
    private final FeeCommandService feeCommandService;

    @Override
    public void submitFee(Long userId, String username, String realName, Integer roleCode, FeeSubmitDTO dto) {
        feeCommandService.submit(userId, username, realName, roleCode, dto);
    }

    @Override
    public PageResult<FeeListVO> getFeeList(String deptCode, String month, String status, String contributorName, String createDate,
                                            String publishStartDate, String publishEndDate, String createStartDate, String createEndDate,
                                            Integer pageNum, Integer pageSize,
                                            Long operatorId, Integer roleCode) {
        // 202609201644 - 稿费列表查询委托独立查询服务处理。
        return feeQueryService.list(deptCode, month, status, contributorName, createDate,
                publishStartDate, publishEndDate, createStartDate, createEndDate,
                pageNum, pageSize, operatorId, roleCode);
    }

    @Override
    public FeeDetailVO getFeeDetail(Long id, String workType, Long operatorId, Integer roleCode) {
        return feeQueryService.detail(id, workType, operatorId, roleCode);
    }

    @Override
    public void updateFee(Long id, String workType, FeeSubmitDTO dto, Long operatorId,
                          String operatorName, String operatorRole, Integer roleCode) {
        feeCommandService.update(id, workType, dto, operatorId, operatorName, operatorRole, roleCode);
    }

    @Override
    public List<BigDecimal> calculateFee(FeeSubmitDTO dto, Integer operatorRoleCode) {
        return feeCommandService.calculate(dto, operatorRoleCode);
    }

    @Override
    public void auditFee(Long id, String workType, FeeAuditDTO dto, Long auditorId,
                         String auditorName, String auditorRole, Integer auditorRoleCode) {
        feeAuditService.audit(id, workType, dto, auditorId, auditorName, auditorRole, auditorRoleCode);
    }

    @Override
    public void batchAuditFee(FeeBatchAuditDTO dto, Long auditorId,
                              String auditorName, String auditorRole, Integer auditorRoleCode) {
        feeAuditService.batchAudit(dto, auditorId, auditorName, auditorRole, auditorRoleCode);
    }

    @Override
    public void resetAudit(Long id, String workType, Long operatorId,
                           String operatorName, String operatorRole, Integer operatorRoleCode) {
        feeAuditService.reset(id, workType, operatorId, operatorName, operatorRole, operatorRoleCode);
    }

    @Override
    public void invalidateRejectedFee(Long id, String workType, Long operatorId, String operatorName,
                                      String operatorRole, Integer roleCode) {
        feeAuditService.invalidateRejected(id, workType, operatorId, operatorName, operatorRole, roleCode);
    }

    @Override
    public FeeExportFile exportFee(FeeExportDTO dto, Long operatorId, Integer roleCode) {
        // 202609201644 - 稿费文件生成委托独立导出服务处理。
        return feeExportService.export(dto, operatorId, roleCode);
    }
}
