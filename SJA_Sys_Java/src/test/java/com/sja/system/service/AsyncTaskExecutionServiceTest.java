package com.sja.system.service;

import com.sja.system.async.AsyncTaskEnvelope;
import com.sja.system.async.AsyncTaskType;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.entity.SysAsyncTaskExecution;
import com.sja.system.mapper.SysAsyncTaskExecutionMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.security.SjaUserPrincipal;
import com.sja.system.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncTaskExecutionServiceTest {

    @Test
    void writesCompletionMarkerAfterBusinessOperation() {
        SysAsyncTaskExecutionMapper mapper = mock(SysAsyncTaskExecutionMapper.class);
        AsyncTaskQueueService queueService = mock(AsyncTaskQueueService.class);
        UserService userService = mock(UserService.class);
        FeeService feeService = mock(FeeService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        when(userDetailsService.loadUserById(56L)).thenReturn(mock(SjaUserPrincipal.class));
        AsyncTaskExecutionService service = new AsyncTaskExecutionService(
                mapper, queueService, userService, feeService, userMapper, userDetailsService);

        AsyncTaskEnvelope envelope = profileEnvelope();
        ProfileUpdateDTO payload = new ProfileUpdateDTO();
        when(queueService.decryptPayload(envelope, ProfileUpdateDTO.class)).thenReturn(payload);

        service.execute(envelope);

        verify(userService).updateProfile(56L, payload);
        verify(mapper).insert(any(SysAsyncTaskExecution.class));
    }

    @Test
    void skipsBusinessOperationWhenTaskWasAlreadyCommitted() {
        SysAsyncTaskExecutionMapper mapper = mock(SysAsyncTaskExecutionMapper.class);
        AsyncTaskQueueService queueService = mock(AsyncTaskQueueService.class);
        UserService userService = mock(UserService.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        AsyncTaskExecutionService service = new AsyncTaskExecutionService(
                mapper, queueService, userService, mock(FeeService.class),
                mock(SysUserMapper.class), userDetailsService);
        AsyncTaskEnvelope envelope = profileEnvelope();
        when(mapper.selectById(envelope.getTaskId())).thenReturn(new SysAsyncTaskExecution());

        service.execute(envelope);

        verify(queueService, never()).decryptPayload(any(), any());
        verify(userService, never()).updateProfile(any(), any());
        verify(mapper, never()).insert(any(SysAsyncTaskExecution.class));
    }

    private AsyncTaskEnvelope profileEnvelope() {
        return AsyncTaskEnvelope.builder()
                .taskId("11111111-1111-1111-1111-111111111111")
                .type(AsyncTaskType.PROFILE_UPDATE)
                .ownerId(56L)
                .payload("encrypted")
                .attempt(0)
                .build();
    }
}
