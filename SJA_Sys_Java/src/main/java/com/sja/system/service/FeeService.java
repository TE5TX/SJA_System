package com.sja.system.service;

import com.sja.system.application.model.FeeExportFile;
import com.sja.system.common.PageResult;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeBatchAuditDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fee (remuneration) management service interface.
 * Handles submission, listing, detail retrieval, auditing, and export
 * for editorial, photography, video, and visual works.
 */
public interface FeeService {

    /**
     * Submit a new fee record for a piece of work.
     *
     * @param userId    submitting user
     * @param username  submitting user's login name
     * @param realName  submitting user's real name
     * @param deptCode  submitting user's department code
     * @param dto       fee submission data (work info + contributors)
     */
    void submitFee(Long userId, String username, String realName, Integer roleCode, FeeSubmitDTO dto);

    /**
     * List fee records with optional filters.
     *
     * @param deptCode filter by department (nullable)
     * @param month    filter by month in "yyyy-MM" format (nullable)
     * @param status   filter by status (nullable)
     * @param createDate creation date in yyyy-MM-dd format (nullable)
     * @param pageNum  page number (1-based)
     * @param pageSize page size
     * @return paginated fee list
     */
    // 202609091535 - 完成了创建日期筛选的服务接口定义。
    default PageResult<FeeListVO> getFeeList(String deptCode, String month, String status, String contributorName, String createDate,
                                     Integer pageNum, Integer pageSize, Long operatorId, Integer roleCode) {
        return getFeeList(deptCode, month, status, contributorName, createDate,
                null, null, null, null, pageNum, pageSize, operatorId, roleCode);
    }

    /** 202609091610 - 完成了日期范围查询服务接口，旧接口继续兼容。 */
    PageResult<FeeListVO> getFeeList(String deptCode, String month, String status, String contributorName, String createDate,
                                    String publishStartDate, String publishEndDate, String createStartDate, String createEndDate,
                                    Integer pageNum, Integer pageSize, Long operatorId, Integer roleCode);

    /**
     * Get detailed information for a specific work's fee record.
     *
     * @param id       work primary key
     * @param workType one of EDITORIAL, PHOTO, VIDEO, VISUAL, SUBSIDY
     * @return full fee detail including contributors
     */
    FeeDetailVO getFeeDetail(Long id, String workType, Long operatorId, Integer roleCode);

    void updateFee(Long id, String workType, FeeSubmitDTO dto, Long operatorId, String operatorName, String operatorRole,
                   Integer roleCode);

    List<BigDecimal> calculateFee(FeeSubmitDTO dto, Integer operatorRoleCode);

    /**
     * Audit (approve/reject) a fee record.
     *
     * @param id           work primary key
     * @param workType     one of EDITORIAL, PHOTO, VIDEO, VISUAL
     * @param dto          audit decision and opinion
     * @param auditorId    auditor user ID
     * @param auditorName  auditor display name
     * @param auditorRole  auditor role name
     */
    void auditFee(Long id, String workType, FeeAuditDTO dto, Long auditorId, String auditorName,
                  String auditorRole, Integer auditorRoleCode);

    void batchAuditFee(FeeBatchAuditDTO dto, Long auditorId, String auditorName, String auditorRole,
                       Integer auditorRoleCode);

    void resetAudit(Long id, String workType, Long operatorId, String operatorName, String operatorRole,
                    Integer operatorRoleCode);

    /**
     * Mark a rejected fee task as invalid without physically deleting its data.
     */
    void invalidateRejectedFee(Long id, String workType, Long operatorId, String operatorName,
                               String operatorRole, Integer roleCode);

    /**
     * 根据查询条件生成稿费明细导出文件。
     *
     * @param dto        导出条件及字段配置
     * @param operatorId 操作人ID
     * @param roleCode   操作人角色编码
     * @return 与HTTP协议无关的导出文件
     */
    FeeExportFile exportFee(FeeExportDTO dto, Long operatorId, Integer roleCode);
}
