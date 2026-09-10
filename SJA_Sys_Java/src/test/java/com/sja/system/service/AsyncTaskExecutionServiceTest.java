package com.sja.system.service;

import com.sja.system.async.AsyncTaskEnvelope;
import com.sja.system.async.AsyncTaskType;
import com.sja.system.application.port.ActiveUserAccessPort;
import com.sja.system.application.port.ActiveUserAccessPort.ActiveUserAccess;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.entity.SysAsyncTaskExecution;
import com.sja.system.mapper.SysAsyncTaskExecutionMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

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
        ActiveUserAccessPort activeUserAccessPort = mock(ActiveUserAccessPort.class);
        when(activeUserAccessPort.findActiveUserById(56L)).thenReturn(Optional.of(activeUser()));
        AsyncTaskExecutionService service = new AsyncTaskExecutionService(
                mapper, queueService, userService, feeService, activeUserAccessPort);

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
        ActiveUserAccessPort activeUserAccessPort = mock(ActiveUserAccessPort.class);
        AsyncTaskExecutionService service = new AsyncTaskExecutionService(
                mapper, queueService, userService, mock(FeeService.class),
                activeUserAccessPort);
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

    private ActiveUserAccess activeUser() {
        return new ActiveUserAccess(56L, "20230001", "测试用户", 13, Set.of("fee:submit"));
    }
}
