package com.sja.system.service.fee;

import com.sja.system.application.dto.fee.FeeAuditDTO;
import com.sja.system.dal.dataobject.BizEditorialArticle;
import com.sja.system.dal.mysql.BizEditorialArticleMapper;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.fee.FeeAccessService;
import com.sja.system.service.fee.FeeAuditService;
import com.sja.system.service.fee.FeeWorkStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplResetAuditTest {

    @Mock
    private BizEditorialArticleMapper editorialMapper;

    @Mock
    private OperationLogService operationLogService;
    @Mock
    private FeeAccessService accessService;

    private FeeAuditService feeService;

    @BeforeEach
    void setUp() {
        FeeWorkStateService stateService = new FeeWorkStateService(editorialMapper, null, null, null, null);
        feeService = new FeeAuditService(accessService, stateService, operationLogService);
    }

    @Test
    void approvedTaskCanReturnToSubmitted() {
        BizEditorialArticle approved = new BizEditorialArticle();
        approved.setId(12L);
        approved.setAuditStatus("APPROVED");
        approved.setIsValid(1);
        approved.setRemark("审核通过");
        when(editorialMapper.selectById(12L)).thenReturn(approved);

        feeService.reset(12L, "EDITORIAL", 1L, "管理员", "0", 0);

        ArgumentCaptor<BizEditorialArticle> captor = ArgumentCaptor.forClass(BizEditorialArticle.class);
        verify(editorialMapper).updateById(captor.capture());
        assertEquals("SUBMITTED", captor.getAllValues().get(0).getAuditStatus());
        verify(operationLogService).log(1L, "管理员", "0", "RESET_AUDIT",
                "FEE", 12L, "将已通过稿费任务退回待审核 [EDITORIAL] id=12", "");
    }

    @Test
    void nonApprovedTaskCannotReturnToSubmitted() {
        BizEditorialArticle submitted = new BizEditorialArticle();
        submitted.setId(13L);
        submitted.setAuditStatus("SUBMITTED");
        submitted.setIsValid(1);
        when(editorialMapper.selectById(13L)).thenReturn(submitted);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> feeService.reset(13L, "EDITORIAL", 1L, "管理员", "0", 0));

        assertEquals("只有已通过的稿费任务可以退回待审核", error.getMessage());
        verify(editorialMapper, never()).updateById(any(BizEditorialArticle.class));
        verify(operationLogService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void nonManagerCannotResetAuditEvenWhenCallingServiceDirectly() {
        doThrow(com.sja.system.common.ApiException.forbidden("只有管理员或秘书长可以退回审批"))
                .when(accessService).ensureCanManageAmount(4, "退回审批");
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> feeService.reset(13L, "EDITORIAL", 4L, "部长", "4", 4));

        // 202609181625 - 验证审核业务授权不再只依赖 Controller 的 HTTP 入口校验。
        assertEquals("只有管理员或秘书长可以退回审批", error.getMessage());
        verify(editorialMapper, never()).selectById(any());
    }

    @Test
    void approvedTaskCannotBeAuditedAgain() {
        BizEditorialArticle approved = new BizEditorialArticle();
        approved.setId(16L);
        approved.setAuditStatus("APPROVED");
        approved.setIsValid(1);
        when(editorialMapper.selectById(16L)).thenReturn(approved);
        FeeAuditDTO dto = new FeeAuditDTO();
        dto.setApproved(false);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> feeService.audit(16L, "EDITORIAL", dto, 1L, "管理员", "0", 0));

        assertEquals("只有待审核的稿费任务可以审核", error.getMessage());
        verify(editorialMapper, never()).updateById(any(BizEditorialArticle.class));
    }

    @Test
    void rejectedTaskIsMarkedInvalidInsteadOfPhysicallyDeleted() {
        BizEditorialArticle rejected = new BizEditorialArticle();
        rejected.setId(14L);
        rejected.setAuditStatus("REJECTED");
        rejected.setIsValid(1);
        when(editorialMapper.selectById(14L)).thenReturn(rejected);

        feeService.invalidateRejected(14L, "EDITORIAL", 1L, "管理员", "0", 0);

        ArgumentCaptor<BizEditorialArticle> captor = ArgumentCaptor.forClass(BizEditorialArticle.class);
        verify(editorialMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getIsValid());
        verify(operationLogService).log(1L, "管理员", "0", "DELETE",
                "FEE", 14L, "将已驳回稿费任务设为无效 [EDITORIAL] id=14", "");
    }

    @Test
    void submittedTaskCannotBeMarkedInvalid() {
        BizEditorialArticle submitted = new BizEditorialArticle();
        submitted.setId(15L);
        submitted.setAuditStatus("SUBMITTED");
        submitted.setIsValid(1);
        when(editorialMapper.selectById(15L)).thenReturn(submitted);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> feeService.invalidateRejected(15L, "EDITORIAL", 1L, "管理员", "0", 0));

        assertEquals("只有已驳回的稿费任务可以删除", error.getMessage());
        verify(editorialMapper, never()).updateById(any(BizEditorialArticle.class));
    }
}
