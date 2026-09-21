package com.sja.system.controller;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.dto.UserDeleteDTO;
import com.sja.system.service.UserApplicationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserControllerDeleteTest {

    @Test
    void delegatesManagedUserDeletionToApplicationUseCase() {
        UserApplicationService applicationService = mock(UserApplicationService.class);
        UserController controller = new UserController(applicationService);
        CurrentActor actor = new CurrentActor(1L, "管理员", 0);
        UserDeleteDTO body = new UserDeleteDTO();
        body.setCurrentPassword("current-password");
        var result = controller.deleteUser(2L, body, actor);

        assertEquals(200, result.getCode());
        verify(applicationService).delete(2L, body, actor);
    }
}
