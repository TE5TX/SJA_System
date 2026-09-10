package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
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
    @Mock private OperationLogServiceImpl operationLogService;
    @InjectMocks private FeeServiceImpl service;

    @Test
    void listAppliesWholeDayRangeToEveryWorkType() {
        service.getFeeList(null, null, null, null, "2026-09-09", 1, 10, 1L, 0);
        verifyRanges();
    }

    @Test
    void exportUsesTheSameCreationDateRange() {
        FeeExportDTO dto = new FeeExportDTO();
        dto.setCreateDate("2026-09-09");
        dto.setFields(List.of("title"));
        service.exportFee(dto, 1L, 0, new MockHttpServletResponse());
        verifyRanges();
    }

    @Test
    void emptyDateDoesNotRestrictCreationTime() {
        service.getFeeList(null, null, null, null, "", 1, 10, 1L, 0);
        verify(editorialMapper).selectList(argThat(query ->
                !query.getSqlSegment().contains("create_time >=")));
    }

    @Test
    void invalidDatesAreRejectedBeforeQuerying() {
        for (String date : List.of("2026-02-30", "2026-9-9", "invalid")) {
            assertThrows(ApiException.class, () ->
                    service.getFeeList(null, null, null, null, date, 1, 10, 1L, 0));
        }
        verifyNoInteractions(editorialMapper, photographyMapper, videoMapper,
                visualMapper, monthlySubsidyMapper);
    }

    private void verifyRanges() {
        verify(editorialMapper).selectList(argThat(this::hasWholeDayRange));
        verify(photographyMapper).selectList(argThat(this::hasWholeDayRange));
        verify(videoMapper).selectList(argThat(this::hasWholeDayRange));
        verify(visualMapper).selectList(argThat(this::hasWholeDayRange));
        verify(monthlySubsidyMapper).selectList(argThat(this::hasWholeDayRange));
    }

    private boolean hasWholeDayRange(com.baomidou.mybatisplus.core.conditions.Wrapper<?> wrapper) {
        if (!(wrapper instanceof QueryWrapper<?> query)) return false;
        String sql = query.getSqlSegment();
        return sql.contains("create_time >=") && sql.contains("create_time <")
                && query.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 9, 9, 0, 0))
                && query.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 9, 10, 0, 0));
    }
}
