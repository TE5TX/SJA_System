package com.sja.system.controller;

import com.sja.system.dto.UserDeleteDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.AsyncTaskQueueService;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserImportService;
import com.sja.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerDeleteTest {

    @Test
    void verifiesCurrentPasswordBeforeDeletingManagedUser() {
        UserService userService = mock(UserService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UserController controller = new UserController(userService, mock(UserImportService.class),
                mock(OperationLogService.class), securityUtils, mock(AsyncTaskQueueService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserDeleteDTO body = new UserDeleteDTO();
        body.setCurrentPassword("current-password");
        when(securityUtils.getCurrentUserId(request)).thenReturn(1L);
        when(securityUtils.getCurrentUsernameFromToken(request)).thenReturn("管理员");
        when(securityUtils.getCurrentRoleCode(request)).thenReturn(0);

        var result = controller.deleteUser(2L, body, request);

        assertEquals(200, result.getCode());
        InOrder calls = inOrder(userService);
        calls.verify(userService).verifyCurrentPassword(1L, "current-password");
        calls.verify(userService).deleteUser(2L, 1L, "管理员", "0");
    }
}
