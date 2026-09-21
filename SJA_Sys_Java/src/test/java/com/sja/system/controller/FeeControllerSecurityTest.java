package com.sja.system.controller;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.FeeExportFile;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.service.FeeApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeControllerSecurityTest {

    @Test
    void forwardsRoleCodeForServiceLayerAmountCalculationAuthorization() {
        FeeApplicationService applicationService = mock(FeeApplicationService.class);
        FeeController controller = new FeeController(applicationService);
        CurrentActor actor = new CurrentActor(7L, "部长", 4);

        FeeSubmitDTO dto = new FeeSubmitDTO();
        controller.calculateFee(dto, actor);

        verify(applicationService).calculate(dto, actor);
    }

    @Test
    void convertsServiceFileIntoHttpDownloadResponse() {
        FeeApplicationService applicationService = mock(FeeApplicationService.class);
        FeeController controller = new FeeController(applicationService);
        CurrentActor actor = new CurrentActor(7L, "管理员", 0);
        byte[] content = {1, 2, 3};
        when(applicationService.export(any(FeeExportDTO.class), any()))
                .thenReturn(new FeeExportFile("稿费明细.xlsx", FeeExportFile.XLSX_CONTENT_TYPE, content));

        ResponseEntity<byte[]> response = controller.exportFee(new FeeExportDTO(), actor);

        // 202609201130 - 验证Controller只负责把导出文件转换为HTTP下载响应。
        assertEquals(200, response.getStatusCode().value());
        assertEquals(FeeExportFile.XLSX_CONTENT_TYPE, response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getContentDisposition().isAttachment());
        assertEquals("稿费明细.xlsx", response.getHeaders().getContentDisposition().getFilename());
        assertArrayEquals(content, response.getBody());
        verify(applicationService).export(any(FeeExportDTO.class), any());
    }
}
