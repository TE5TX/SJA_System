package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sja.system.common.PageResult;
import com.sja.system.entity.SysOperationLog;
import com.sja.system.mapper.SysOperationLogMapper;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        try {
            SysOperationLog logEntity = new SysOperationLog();
            logEntity.setUserId(userId);
            logEntity.setRealName(realName);
            logEntity.setRoleName(roleName);
            logEntity.setOperationType(operationType);
            logEntity.setTargetModule(targetModule);
            logEntity.setTargetId(targetId);
            logEntity.setDetail(detail);
            logEntity.setIpAddress(ipAddress);

            sysOperationLogMapper.insert(logEntity);
        } catch (Exception e) {
            // Log failure should not break the main business flow
            log.error("Failed to write operation log: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageResult<OperationLogVO> getLogList(Integer pageNum, Integer pageSize) {
        int pn = pageNum != null ? pageNum : 1;
        int ps = pageSize != null ? pageSize : 10;

        Page<SysOperationLog> page = new Page<>(pn, ps);
        QueryWrapper<SysOperationLog> query = new QueryWrapper<>();
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
        vo.setDetail(entity.getDetail());
        vo.setIpAddress(entity.getIpAddress());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
