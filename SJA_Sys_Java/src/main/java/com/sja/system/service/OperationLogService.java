package com.sja.system.service;

import com.sja.system.common.PageResult;
import com.sja.system.vo.OperationLogVO;

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

    /**
     * Retrieve paginated operation logs, newest first.
     *
     * @param pageNum  page number (1-based)
     * @param pageSize page size
     * @return paginated log list
     */
    PageResult<OperationLogVO> getLogList(Integer pageNum, Integer pageSize);
}
