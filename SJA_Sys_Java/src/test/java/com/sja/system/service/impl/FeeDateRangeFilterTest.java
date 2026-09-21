package com.sja.system.service.impl;

import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.mapper.BizEditorialArticleMapper;
import com.sja.system.mapper.BizPhotographyWorkMapper;
import com.sja.system.mapper.BizVideoWorkMapper;
import com.sja.system.mapper.BizVisualWorkMapper;
import com.sja.system.mapper.BizMonthlySubsidyMapper;
import com.sja.system.mapper.FeeQueryMapper;
import com.sja.system.mapper.model.FeeListQuery;
import com.sja.system.service.OperationLogService;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 202609091610 - 完成了日期范围、跨月边界及导出一致性的回归测试。 */
@ExtendWith(MockitoExtension.class)
class FeeDateRangeFilterTest {
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
    void listCombinesBothRangesAcrossMonthBoundary() {
        queryService.list(null, null, null, null, null,
                "2026-08-20", "2026-09-09", "2026-08-31", "2026-09-09",
                1, 10, 1L, 0);
        verifyRanges();
    }

    @Test
    void exportUsesTheSameRanges() {
        FeeExportDTO dto = new FeeExportDTO();
        dto.setPublishStartDate("2026-08-20");
        dto.setPublishEndDate("2026-09-09");
        dto.setCreateStartDate("2026-08-31");
        dto.setCreateEndDate("2026-09-09");
        dto.setFields(List.of("title"));
        exportService.export(dto, 1L, 0);
        verifyRanges();
    }

    @Test
    void rejectsReversedOrMalformedRangesBeforeQuerying() {
        assertThrows(ApiException.class, () -> queryService.list(null, null, null, null, null,
                "2026-09-10", "2026-09-09", null, null, 1, 10, 1L, 0));
        assertThrows(ApiException.class, () -> queryService.list(null, null, null, null, null,
                null, null, "2026-09-10", "2026-09-09", 1, 10, 1L, 0));
        assertThrows(ApiException.class, () -> queryService.list(null, null, null, null, null,
                "2026-02-30", null, null, null, 1, 10, 1L, 0));
        verifyNoInteractions(editorialMapper, photographyMapper, videoMapper, visualMapper, monthlySubsidyMapper);
    }

    @Test
    void acceptsOpenEndedRange() {
        queryService.list(null, null, null, null, null,
                "2026-08-20", null, null, "2026-09-09", 1, 10, 1L, 0);
        verify(feeQueryMapper).countFeeItems(argThat(query -> query.getPublishStart() != null
                && query.getPublishEnd() == null && query.getCreateStart() == null
                && query.getCreateEndExclusive() != null));
    }

    private void verifyRanges() {
        verify(feeQueryMapper).countFeeItems(argThat(this::matches));
    }

    private boolean matches(FeeListQuery query) {
        return java.time.LocalDate.of(2026, 8, 20).equals(query.getPublishStart())
                && java.time.LocalDate.of(2026, 9, 9).equals(query.getPublishEnd())
                && java.time.LocalDateTime.of(2026, 8, 31, 0, 0).equals(query.getCreateStart())
                && java.time.LocalDateTime.of(2026, 9, 10, 0, 0).equals(query.getCreateEndExclusive())
                && "2026-08".equals(query.getPublishStartMonth())
                && "2026-09".equals(query.getPublishEndMonth());
    }
}
