package com.sja.system.controller;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.service.FeeApplicationService;
import com.sja.system.service.UserApplicationService;
import com.sja.system.vo.AsyncTaskAcceptedVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncWriteControllerTest {

    @Test
    void profileUpdateReturnsAcceptedTask() {
        UserApplicationService applicationService = mock(UserApplicationService.class);
        UserController controller = new UserController(applicationService);
        CurrentActor actor = new CurrentActor(56L, "20230001", 4);
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        AsyncTaskAcceptedVO accepted = acceptedTask();
        when(applicationService.updateProfile(dto, "profile-request-1", actor)).thenReturn(accepted);

        var response = controller.updateProfile(dto, "profile-request-1", actor);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(accepted, response.getBody().getData());
        verify(applicationService).updateProfile(dto, "profile-request-1", actor);
    }

    @Test
    void feeSubmitReturnsAcceptedTask() {
        FeeApplicationService applicationService = mock(FeeApplicationService.class);
        FeeController controller = new FeeController(applicationService);
        CurrentActor actor = new CurrentActor(56L, "20230001", 4);
        FeeSubmitDTO dto = new FeeSubmitDTO();
        AsyncTaskAcceptedVO accepted = acceptedTask();
        when(applicationService.submit(dto, "fee-request-1", actor)).thenReturn(accepted);

        var response = controller.submitFee(dto, "fee-request-1", actor);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(accepted, response.getBody().getData());
        verify(applicationService).submit(dto, "fee-request-1", actor);
    }

    private AsyncTaskAcceptedVO acceptedTask() {
        return new AsyncTaskAcceptedVO(
                "11111111-1111-1111-1111-111111111111",
                "PENDING",
                "/api/v1/async-tasks/11111111-1111-1111-1111-111111111111",
                "status-token");
    }
}
