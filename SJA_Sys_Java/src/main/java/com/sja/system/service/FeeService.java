package com.sja.system.service;

import com.sja.system.common.PageResult;
import com.sja.system.dto.FeeAuditDTO;
import com.sja.system.dto.FeeBatchAuditDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.vo.FeeDetailVO;
import com.sja.system.vo.FeeListVO;

import javax.servlet.http.HttpServletResponse;
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
     * @param pageNum  page number (1-based)
     * @param pageSize page size
     * @return paginated fee list
     */
    PageResult<FeeListVO> getFeeList(String deptCode, String month, String status, String contributorName,
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

    List<BigDecimal> calculateFee(FeeSubmitDTO dto);

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
    void auditFee(Long id, String workType, FeeAuditDTO dto, Long auditorId, String auditorName, String auditorRole);

    void batchAuditFee(FeeBatchAuditDTO dto, Long auditorId, String auditorName, String auditorRole);

    void resetAudit(Long id, String workType, Long operatorId, String operatorName, String operatorRole);

    /**
     * Mark a rejected fee task as invalid without physically deleting its data.
     */
    void invalidateRejectedFee(Long id, String workType, Long operatorId, String operatorName,
                               String operatorRole, Integer roleCode);

    /**
     * Export fee records to an Excel file written to the HTTP response.
     *
     * @param deptCode filter by department (nullable)
     * @param month    filter by month in "yyyy-MM" format (nullable)
     * @param response HTTP response to write the Excel file to
     */
    void exportFee(FeeExportDTO dto, Long operatorId, Integer roleCode, HttpServletResponse response);
}
