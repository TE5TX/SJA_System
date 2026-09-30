package com.sja.system.service.user;

import com.sja.system.service.async.AsyncTaskQueueService;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.dto.user.UserDeleteDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/** 人员应用用例编排测试。 */
class UserApplicationServiceTest {

    @Test
    void verifiesPasswordBeforeDeletingManagedUser() {
        UserService userService = mock(UserService.class);
        UserApplicationService applicationService = new UserApplicationService(
                userService, mock(UserImportService.class), mock(UserRosterImportService.class),
                mock(UserExportService.class), mock(AsyncTaskQueueService.class),
                mock(com.sja.system.service.storage.ImportArchiveService.class));
        UserDeleteDTO dto = new UserDeleteDTO();
        dto.setCurrentPassword("current-password");
        CurrentActor actor = new CurrentActor(1L, "管理员", 0);

        applicationService.delete(2L, dto, actor);

        // 202609211059 - 验证删除人员的密码复核和删除顺序已由应用服务统一保证。
        InOrder calls = inOrder(userService);
        calls.verify(userService).verifyCurrentPassword(1L, "current-password");
        calls.verify(userService).deleteUser(2L, 1L, "管理员", "0");
    }
}
