package com.sja.system.controller.async;

import com.sja.system.controller.fee.FeeController;
import com.sja.system.controller.user.UserController;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.application.dto.user.ProfileUpdateDTO;
import com.sja.system.service.fee.FeeApplicationService;
import com.sja.system.service.user.UserApplicationService;
import com.sja.system.application.vo.async.AsyncTaskAcceptedVO;
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
