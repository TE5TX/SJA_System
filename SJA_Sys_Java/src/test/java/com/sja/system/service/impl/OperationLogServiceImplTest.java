package com.sja.system.service.impl;

import com.sja.system.entity.SysOperationLog;
import com.sja.system.mapper.SysOperationLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogServiceImplTest {

    @Test
    void throwsWhenAuditInsertFails() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        when(mapper.insert(any(SysOperationLog.class))).thenReturn(0);
        OperationLogServiceImpl service = new OperationLogServiceImpl(mapper);

        assertThrows(IllegalStateException.class,
                () -> service.log(1L, "管理员", "0", "UPDATE", "USER", 8L, "修改人员", ""));
    }

    @Test
    void marksIndependentFailureAuditAsFailure() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        when(mapper.insert(any(SysOperationLog.class))).thenReturn(1);
        OperationLogServiceImpl service = new OperationLogServiceImpl(mapper);

        service.logFailure(1L, "管理员", "0", "PUT", "USER", "请求失败，HTTP状态码=400");

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals("FAILURE", captor.getValue().getResultStatus());
    }
}
