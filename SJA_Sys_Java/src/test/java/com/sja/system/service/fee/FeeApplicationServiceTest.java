package com.sja.system.service.fee;

import com.sja.system.service.async.AsyncTaskQueueService;
import com.sja.system.service.user.UserService;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.dto.fee.FeeBatchAuditDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/** 稿费应用用例编排测试。 */
class FeeApplicationServiceTest {

    @Test
    void verifiesPasswordBeforeBatchAudit() {
        FeeService feeService = mock(FeeService.class);
        UserService userService = mock(UserService.class);
        FeeApplicationService applicationService = new FeeApplicationService(
                feeService, mock(MonthlyFeeExportService.class), userService,
                mock(AsyncTaskQueueService.class));
        FeeBatchAuditDTO dto = new FeeBatchAuditDTO();
        dto.setCurrentPassword("current-password");
        CurrentActor actor = new CurrentActor(1L, "管理员", 0);

        applicationService.batchAudit(dto, actor);

        // 202609211059 - 验证敏感操作的密码复核顺序已由应用服务统一保证。
        InOrder calls = inOrder(userService, feeService);
        calls.verify(userService).verifyCurrentPassword(1L, "current-password");
        calls.verify(feeService).batchAuditFee(dto, 1L, "管理员", "0", 0);
    }
}
