package com.sja.system.controller;

import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.AsyncTaskQueueService;
import com.sja.system.service.FeeService;
import com.sja.system.service.MonthlyFeeExportService;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserService;
import com.sja.system.vo.AsyncTaskAcceptedVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncWriteControllerTest {

    @Test
    void profileUpdateReturnsAcceptedTask() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        AsyncTaskQueueService queueService = mock(AsyncTaskQueueService.class);
        UserController controller = new UserController(mock(UserService.class),
                mock(OperationLogService.class), securityUtils, queueService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        AsyncTaskAcceptedVO accepted = acceptedTask();
        when(securityUtils.getCurrentUserId(request)).thenReturn(56L);
        when(queueService.enqueueProfile(56L, dto, "profile-request-1")).thenReturn(accepted);

        var response = controller.updateProfile(dto, "profile-request-1", request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(accepted, response.getBody().getData());
        verify(queueService).enqueueProfile(56L, dto, "profile-request-1");
    }

    @Test
    void feeSubmitReturnsAcceptedTask() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        AsyncTaskQueueService queueService = mock(AsyncTaskQueueService.class);
        FeeController controller = new FeeController(mock(FeeService.class),
                mock(MonthlyFeeExportService.class), mock(UserService.class), securityUtils, queueService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        FeeSubmitDTO dto = new FeeSubmitDTO();
        AsyncTaskAcceptedVO accepted = acceptedTask();
        when(securityUtils.getCurrentUserId(request)).thenReturn(56L);
        when(queueService.enqueueFee(56L, dto, "fee-request-1")).thenReturn(accepted);

        var response = controller.submitFee(dto, "fee-request-1", request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(accepted, response.getBody().getData());
        verify(queueService).enqueueFee(56L, dto, "fee-request-1");
    }

    private AsyncTaskAcceptedVO acceptedTask() {
        return new AsyncTaskAcceptedVO(
                "11111111-1111-1111-1111-111111111111",
                "PENDING",
                "/api/v1/async-tasks/11111111-1111-1111-1111-111111111111",
                "status-token");
    }
}
