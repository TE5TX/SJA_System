package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.entity.SysOperationLog;
import com.sja.system.mapper.SysOperationLogMapper;
import com.sja.system.dto.OperationLogQueryDTO;
import com.sja.system.common.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogServiceImplTest {

    @Test
    void throwsWhenAuditInsertFails() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        when(mapper.insert(any(SysOperationLog.class))).thenReturn(0);
        OperationLogServiceImpl service = new OperationLogServiceImpl(mapper, Optional::empty);

        assertThrows(IllegalStateException.class,
                () -> service.log(1L, "管理员", "0", "UPDATE", "USER", 8L, "修改人员", ""));
    }

    @Test
    void marksIndependentFailureAuditAsFailure() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        when(mapper.insert(any(SysOperationLog.class))).thenReturn(1);
        OperationLogServiceImpl service = new OperationLogServiceImpl(mapper, Optional::empty);

        service.logFailure(1L, "管理员", "0", "PUT", "USER", "请求失败，HTTP状态码=400");

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals("FAILURE", captor.getValue().getResultStatus());
    }

    @Test
    void detachesUserReferenceWithoutDeletingAuditRows() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        OperationLogServiceImpl service = new OperationLogServiceImpl(mapper, Optional::empty);

        service.detachUserReferences(8L);

        verify(mapper).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    void rejectsAuditLogReadsForNonManagerAtServiceBoundary() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        OperationLogServiceImpl service = new OperationLogServiceImpl(mapper, Optional::empty);

        ApiException error = assertThrows(ApiException.class,
                () -> service.getAuthorizedLogList(new OperationLogQueryDTO(), 4));

        // 202609181625 - 验证操作日志查询权限不再仅由 Controller 约束。
        assertEquals(403, error.getStatus());
    }
}
