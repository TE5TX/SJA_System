package com.sja.system.service.fee;

import com.sja.system.service.system.OperationLogService;

import com.sja.system.common.ApiException;
import com.sja.system.dal.mysql.BizFeeStandardMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class FeeStandardServiceTest {

    @Test
    void rejectsStandardLookupForNonManagerAtServiceBoundary() {
        BizFeeStandardMapper mapper = mock(BizFeeStandardMapper.class);
        FeeStandardService service = new FeeStandardService(mapper, mock(OperationLogService.class));

        ApiException error = assertThrows(ApiException.class, () -> service.list(4));

        // 202609181625 - 验证稿费标准权限不会因绕过 Controller 而失效。
        assertEquals(403, error.getStatus());
        verify(mapper, never()).selectList(org.mockito.ArgumentMatchers.any());
    }
}
