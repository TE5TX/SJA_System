package com.sja.system.service.fee;

import com.sja.system.application.dto.fee.FeeExportDTO;
import com.sja.system.application.model.FeeExportFile;
import com.sja.system.application.vo.fee.FeeDetailVO;
import com.sja.system.application.vo.fee.FeeListVO;
import com.sja.system.dal.mysql.model.FeeListQuery;
import com.sja.system.service.system.OperationLogService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 自定义稿费导出对未发布任务隐藏日期的回归测试。 */
// 202609281627 - 验证自定义稿费表不会把内部归属日期显示为实际发布日期。
class FeeExportServiceUnpublishedDateTest {

    @Test
    void customWorkbookShowsDashForUnpublishedTask() throws Exception {
        FeeQueryService queryService = mock(FeeQueryService.class);
        FeeExportService service = new FeeExportService(queryService, mock(FeeAccessService.class),
                mock(OperationLogService.class), mock(FeeFixedTemplatePort.class));
        FeeListQuery query = new FeeListQuery();
        query.setWorkTypes(Set.of("VIDEO"));
        when(queryService.createExportQuery(null, null, null, null, null,
                null, null, null, null, 1L, 0, null)).thenReturn(query);
        when(queryService.count(query)).thenReturn(1L);
        when(queryService.copyForWorkType(query, "VIDEO")).thenReturn(query);

        FeeListVO item = new FeeListVO();
        item.setId(7L);
        item.setWorkType("VIDEO");
        item.setTitle("待发布视频");
        item.setIsPublished(0);
        when(queryService.loadBatch(query, 0, 200)).thenReturn(List.of(item));
        FeeDetailVO detail = new FeeDetailVO();
        detail.setWorkInfo(Map.of("publishDate", LocalDate.of(2026, 9, 28), "isPublished", 0));
        when(queryService.detail(7L, "VIDEO", 1L, 0)).thenReturn(detail);

        FeeExportDTO dto = new FeeExportDTO();
        dto.setTemplateType("CUSTOM");
        dto.setFields(List.of("title", "publishDate"));
        FeeExportFile file = service.export(dto, 1L, 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
            assertEquals("-", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }
}
