package com.sja.system.service;

import com.sja.system.async.AsyncTaskEnvelope;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.entity.SysAsyncTaskExecution;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysAsyncTaskExecutionMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.security.SjaUserPrincipal;
import com.sja.system.security.UserDetailsServiceImpl;
import com.sja.system.common.ApiException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final SysUserMapper sysUserMapper;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional(rollbackFor = Exception.class)
    public void execute(AsyncTaskEnvelope envelope) {
        // 202608192148 - 修复了消费者宕机重试可能重复写入稿费任务的问题。
        if (executionMapper.selectById(envelope.getTaskId()) != null) {
            return;
        }

        // 202608192148 - 修复了排队期间账号被禁用或角色被撤销后仍按旧权限执行的问题。
        SjaUserPrincipal principal;
        try {
            principal = userDetailsService.loadUserById(envelope.getOwnerId());
        } catch (UsernameNotFoundException e) {
            throw ApiException.forbidden("账号已被禁用或不存在，任务无法继续执行");
        }

        switch (envelope.getType()) {
            case PROFILE_UPDATE:
                ProfileUpdateDTO profile = queueService.decryptPayload(envelope, ProfileUpdateDTO.class);
                userService.updateProfile(envelope.getOwnerId(), profile);
                break;
            case FEE_SUBMIT:
                boolean canSubmit = principal.getAuthorities().stream()
                        .anyMatch(authority -> "fee:submit".equals(authority.getAuthority()));
                if (!canSubmit) {
                    throw ApiException.forbidden("稿费提交权限已被撤销");
                }
                SysUser user = sysUserMapper.selectById(envelope.getOwnerId());
                if (user == null) {
                    throw ApiException.forbidden("用户不存在，任务无法继续执行");
                }
                FeeSubmitDTO fee = queueService.decryptPayload(envelope, FeeSubmitDTO.class);
                feeService.submitFee(envelope.getOwnerId(), user.getUsername(), user.getRealName(),
                        principal.getHighestRoleCode(), fee);
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
