package com.sja.system.service.system;

import com.sja.system.common.PageResult;
import com.sja.system.application.vo.system.OperationLogVO;
import com.sja.system.application.dto.system.OperationLogQueryDTO;

/**
 * Operation log service interface.
 * Records and retrieves system operation audit logs.
 */
public interface OperationLogService {

    /**
     * Record an operation log entry.
     *
     * @param userId        who performed the operation
     * @param realName      operator's real name
     * @param roleName      operator's role name
     * @param operationType type of operation (e.g. CREATE, UPDATE, DELETE, LOGIN, EXPORT)
     * @param targetModule  module affected (e.g. USER, FEE, SYSTEM)
     * @param targetId      primary key of the affected entity (nullable)
     * @param detail        human-readable description of what changed
     * @param ipAddress     client IP address
     */
    void log(Long userId, String realName, String roleName, String operationType,
             String targetModule, Long targetId, String detail, String ipAddress);

    void logChange(Long userId, String realName, String roleName, String operationType,
                   String targetModule, Long targetId, String detail,
                   String beforeData, String afterData, String ipAddress);

    void logFailure(Long userId, String realName, String roleName, String operationType,
                    String targetModule, String detail);

    /**
     * Remove the foreign-key reference to a user without deleting historical audit records.
     */
    void detachUserReferences(Long userId);

    /**
     * Retrieve paginated operation logs, newest first.
     *
     * @param pageNum  page number (1-based)
     * @param pageSize page size
     * @return paginated log list
     */
    PageResult<OperationLogVO> getLogList(Integer pageNum, Integer pageSize);

    PageResult<OperationLogVO> getLogList(OperationLogQueryDTO query);

    /**
     * Retrieves audit logs after enforcing the business role boundary.
     */
    PageResult<OperationLogVO> getAuthorizedLogList(OperationLogQueryDTO query, Integer operatorRoleCode);
}
