package com.sja.system.service;

import com.sja.system.async.AsyncTaskEnvelope;
import com.sja.system.application.port.ActiveUserAccessPort;
import com.sja.system.application.port.ActiveUserAccessPort.ActiveUserAccess;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.entity.SysAsyncTaskExecution;
import com.sja.system.mapper.SysAsyncTaskExecutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 在同一个数据库事务中执行业务写入并记录任务完成标记。
 */
@Service
@RequiredArgsConstructor
public class AsyncTaskExecutionService {

    private final SysAsyncTaskExecutionMapper executionMapper;
    private final AsyncTaskQueueService queueService;
    private final UserService userService;
    private final FeeService feeService;
    private final ActiveUserAccessPort activeUserAccessPort;

    @Transactional(rollbackFor = Exception.class)
    public void execute(AsyncTaskEnvelope envelope) {
        // 202608192148 - 修复了消费者宕机重试可能重复写入稿费任务的问题。
        if (executionMapper.selectById(envelope.getTaskId()) != null) {
            return;
        }

        // 202609101503 - 修复了异步任务服务直接依赖Security具体实现的问题。
        ActiveUserAccess activeUser = activeUserAccessPort.findActiveUserById(envelope.getOwnerId())
                .orElseThrow(() -> ApiException.forbidden("账号已被禁用或不存在，任务无法继续执行"));

        switch (envelope.getType()) {
            case PROFILE_UPDATE:
                ProfileUpdateDTO profile = queueService.decryptPayload(envelope, ProfileUpdateDTO.class);
                userService.updateProfile(envelope.getOwnerId(), profile);
                break;
            case FEE_SUBMIT:
                if (!activeUser.hasAuthority("fee:submit")) {
                    throw ApiException.forbidden("稿费提交权限已被撤销");
                }
                FeeSubmitDTO fee = queueService.decryptPayload(envelope, FeeSubmitDTO.class);
                feeService.submitFee(activeUser.userId(), activeUser.username(), activeUser.realName(),
                        activeUser.highestRoleCode(), fee);
                break;
            default:
                throw new IllegalArgumentException("不支持的异步任务类型: " + envelope.getType());
        }

        SysAsyncTaskExecution execution = new SysAsyncTaskExecution();
        execution.setTaskId(envelope.getTaskId());
        execution.setTaskType(envelope.getType().name());
        execution.setOwnerId(envelope.getOwnerId());
        execution.setCompletedAt(LocalDateTime.now());
        executionMapper.insert(execution);
    }
}
