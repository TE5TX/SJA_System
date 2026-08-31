package com.sja.system.controller;

import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.FeeService;
import com.sja.system.service.AsyncTaskQueueService;
import com.sja.system.service.MonthlyFeeExportService;
import com.sja.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeControllerSecurityTest {

    @Test
    void ministerCannotCallAmountCalculationEndpoint() {
        FeeService feeService = mock(FeeService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        FeeController controller = new FeeController(feeService, mock(MonthlyFeeExportService.class),
                mock(UserService.class), securityUtils, mock(AsyncTaskQueueService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(securityUtils.getCurrentRoleCode(request)).thenReturn(4);

        FeeSubmitDTO dto = new FeeSubmitDTO();
        ApiException error = assertThrows(ApiException.class, () -> controller.calculateFee(dto, request));

        assertEquals(403, error.getStatus());
        verify(feeService, never()).calculateFee(dto);
    }
}
