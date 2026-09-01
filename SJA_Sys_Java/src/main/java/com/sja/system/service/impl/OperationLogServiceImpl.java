package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sja.system.common.PageResult;
import com.sja.system.common.MaskingUtils;
import com.sja.system.dto.OperationLogQueryDTO;
import com.sja.system.entity.SysOperationLog;
import com.sja.system.mapper.SysOperationLogMapper;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.sja.system.security.RequestTraceFilter;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Operation log service implementation.
 * Records and retrieves audit log entries for system operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void log(Long userId, String realName, String roleName, String operationType,
                    String targetModule, Long targetId, String detail, String ipAddress) {
        logChange(userId, realName, roleName, operationType, targetModule, targetId,
                detail, null, null, ipAddress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logChange(Long userId, String realName, String roleName, String operationType,
                          String targetModule, Long targetId, String detail,
                          String beforeData, String afterData, String ipAddress) {
        insertLog(userId, realName, roleName, operationType, targetModule, targetId,
                detail, beforeData, afterData, ipAddress, "SUCCESS");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logFailure(Long userId, String realName, String roleName, String operationType,
                           String targetModule, String detail) {
        insertLog(userId, realName, roleName, operationType, targetModule, null,
                detail, null, null, null, "FAILURE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void detachUserReferences(Long userId) {
        // 202609011700 - 完成了删除人员时保留历史审计日志并解除用户外键引用的处理。
        sysOperationLogMapper.update(null, new UpdateWrapper<SysOperationLog>()
                .eq("user_id", userId)
                .set("user_id", null));
    }

    private void insertLog(Long userId, String realName, String roleName, String operationType,
                           String targetModule, Long targetId, String detail,
                           String beforeData, String afterData, String ipAddress,
                           String resultStatus) {
        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setUserId(userId);
        logEntity.setRealName(realName);
        logEntity.setRoleName(roleName);
        logEntity.setOperationType(operationType);
        logEntity.setTargetModule(targetModule);
        logEntity.setTargetId(targetId);
        logEntity.setDetail(MaskingUtils.sanitizeLogDetail(detail));
        logEntity.setBeforeData(MaskingUtils.sanitizeLogDetail(beforeData));
        logEntity.setAfterData(MaskingUtils.sanitizeLogDetail(afterData));
        logEntity.setResultStatus(resultStatus);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object requestId = request.getAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE);
            Object start = request.getAttribute(RequestTraceFilter.REQUEST_START_ATTRIBUTE);
            logEntity.setRequestId(requestId == null ? null : requestId.toString());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setRequestUri(truncate(request.getRequestURI(), 255));
            logEntity.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
            logEntity.setIpAddress(request.getRemoteAddr());
            if (start instanceof Long) {
                logEntity.setDurationMs(Math.max(0, System.currentTimeMillis() - (Long) start));
            }
        } else {
            logEntity.setIpAddress(ipAddress);
        }

        int inserted = sysOperationLogMapper.insert(logEntity);
        if (inserted != 1) {
            throw new IllegalStateException("操作日志写入失败");
        }
    }

    @Override
    public PageResult<OperationLogVO> getLogList(Integer pageNum, Integer pageSize) {
        OperationLogQueryDTO query = new OperationLogQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return getLogList(query);
    }

    @Override
    public PageResult<OperationLogVO> getLogList(OperationLogQueryDTO filter) {
        int pn = filter.getPageNum() != null ? Math.max(1, filter.getPageNum()) : 1;
        int ps = filter.getPageSize() != null ? Math.min(200, Math.max(1, filter.getPageSize())) : 10;

        Page<SysOperationLog> page = new Page<>(pn, ps);
        QueryWrapper<SysOperationLog> query = new QueryWrapper<>();
        if (org.springframework.util.StringUtils.hasText(filter.getRealName())) {
            query.like("real_name", filter.getRealName().trim());
        }
        if (org.springframework.util.StringUtils.hasText(filter.getOperationType())) {
            query.eq("operation_type", filter.getOperationType().trim());
        }
        if (org.springframework.util.StringUtils.hasText(filter.getTargetModule())) {
            query.eq("target_module", filter.getTargetModule().trim());
        }
        if (org.springframework.util.StringUtils.hasText(filter.getResultStatus())) {
            query.eq("result_status", filter.getResultStatus().trim());
        }
        if (org.springframework.util.StringUtils.hasText(filter.getRequestId())) {
            query.eq("request_id", filter.getRequestId().trim());
        }
        query.ge(filter.getStartTime() != null, "create_time", filter.getStartTime());
        query.le(filter.getEndTime() != null, "create_time", filter.getEndTime());
        query.orderByDesc("create_time");

        Page<SysOperationLog> resultPage = sysOperationLogMapper.selectPage(page, query);

        List<OperationLogVO> voList = resultPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, resultPage.getTotal(), (long) pn, (long) ps);
    }

    private OperationLogVO toVO(SysOperationLog entity) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setRealName(entity.getRealName());
        vo.setRoleName(entity.getRoleName());
        vo.setOperationType(entity.getOperationType());
        vo.setTargetModule(entity.getTargetModule());
        vo.setTargetId(entity.getTargetId());
        vo.setDetail(MaskingUtils.sanitizeLogDetail(entity.getDetail()));
        vo.setIpAddress(entity.getIpAddress());
        vo.setRequestId(entity.getRequestId());
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setRequestUri(entity.getRequestUri());
        vo.setResultStatus(entity.getResultStatus());
        vo.setBeforeData(MaskingUtils.sanitizeLogDetail(entity.getBeforeData()));
        vo.setAfterData(MaskingUtils.sanitizeLogDetail(entity.getAfterData()));
        vo.setUserAgent(entity.getUserAgent());
        vo.setDurationMs(entity.getDurationMs());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
