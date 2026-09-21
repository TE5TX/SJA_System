package com.sja.system.service.fee;

import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeExportDTO;
import com.sja.system.dal.mysql.*;
import com.sja.system.dal.mysql.model.FeeListQuery;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.fee.FeeAccessService;
import com.sja.system.service.fee.FeeExportService;
import com.sja.system.service.fee.FeeFixedTemplatePort;
import com.sja.system.service.fee.FeeQueryService;
import com.sja.system.service.fee.FeeWorkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 202609091535 - 完成了创建日期查询与导出筛选的边界回归测试。 */
@ExtendWith(MockitoExtension.class)
class FeeCreateDateFilterTest {
    @Mock private BizEditorialArticleMapper editorialMapper;
    @Mock private BizPhotographyWorkMapper photographyMapper;
    @Mock private BizVideoWorkMapper videoMapper;
    @Mock private BizVisualWorkMapper visualMapper;
    @Mock private BizMonthlySubsidyMapper monthlySubsidyMapper;
    @Mock private FeeQueryMapper feeQueryMapper;
    @Mock private OperationLogService operationLogService;
    @Mock private FeeAccessService accessService;
    @Mock private FeeFixedTemplatePort fixedTemplatePort;
    @InjectMocks private FeeQueryService queryService;
    private FeeExportService exportService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(accessService.allowedDepartmentCodes(1L, 0))
                .thenReturn(FeeWorkType.allDepartmentCodes());
        exportService = new FeeExportService(queryService, accessService, operationLogService, fixedTemplatePort);
    }

    @Test
    void listAppliesWholeDayRangeToEveryWorkType() {
        queryService.list(null, null, null, null, "2026-09-09", null, null, null, null, 1, 10, 1L, 0);
        verifyRanges();
    }

    @Test
    void exportUsesTheSameCreationDateRange() {
        FeeExportDTO dto = new FeeExportDTO();
        dto.setCreateDate("2026-09-09");
        dto.setFields(List.of("title"));
        exportService.export(dto, 1L, 0);
        verifyRanges();
    }

    @Test
    void emptyDateDoesNotRestrictCreationTime() {
        queryService.list(null, null, null, null, "", null, null, null, null, 1, 10, 1L, 0);
        verify(feeQueryMapper).countFeeItems(argThat(query ->
                query.getCreateStart() == null && query.getCreateEndExclusive() == null));
    }

    @Test
    void invalidDatesAreRejectedBeforeQuerying() {
        for (String date : List.of("2026-02-30", "2026-9-9", "invalid")) {
            assertThrows(ApiException.class, () ->
                    queryService.list(null, null, null, null, date, null, null, null, null, 1, 10, 1L, 0));
        }
        verifyNoInteractions(feeQueryMapper);
    }

    private void verifyRanges() {
        verify(feeQueryMapper).countFeeItems(argThat(this::hasWholeDayRange));
    }

    private boolean hasWholeDayRange(FeeListQuery query) {
        return java.time.LocalDateTime.of(2026, 9, 9, 0, 0).equals(query.getCreateStart())
                && java.time.LocalDateTime.of(2026, 9, 10, 0, 0).equals(query.getCreateEndExclusive());
    }
}
