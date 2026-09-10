package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeExportDTO;
import com.sja.system.mapper.BizEditorialArticleMapper;
import com.sja.system.mapper.BizPhotographyWorkMapper;
import com.sja.system.mapper.BizVideoWorkMapper;
import com.sja.system.mapper.BizVisualWorkMapper;
import com.sja.system.mapper.BizMonthlySubsidyMapper;
import com.sja.system.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
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
    @Mock private OperationLogService operationLogService;
    @InjectMocks private FeeServiceImpl service;

    @Test
    void listCombinesBothRangesAcrossMonthBoundary() {
        service.getFeeList(null, null, null, null, null,
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
        service.exportFee(dto, 1L, 0, new MockHttpServletResponse());
        verifyRanges();
    }

    @Test
    void rejectsReversedOrMalformedRangesBeforeQuerying() {
        assertThrows(ApiException.class, () -> service.getFeeList(null, null, null, null, null,
                "2026-09-10", "2026-09-09", null, null, 1, 10, 1L, 0));
        assertThrows(ApiException.class, () -> service.getFeeList(null, null, null, null, null,
                null, null, "2026-09-10", "2026-09-09", 1, 10, 1L, 0));
        assertThrows(ApiException.class, () -> service.getFeeList(null, null, null, null, null,
                "2026-02-30", null, null, null, 1, 10, 1L, 0));
        verifyNoInteractions(editorialMapper, photographyMapper, videoMapper, visualMapper, monthlySubsidyMapper);
    }

    @Test
    void acceptsOpenEndedRange() {
        service.getFeeList(null, null, null, null, null,
                "2026-08-20", null, null, "2026-09-09", 1, 10, 1L, 0);
        verify(editorialMapper).selectList(argThat(q -> q.getSqlSegment().contains("publish_date >=")
                && !q.getSqlSegment().contains("publish_date <=")
                && !q.getSqlSegment().contains("create_time >=")
                && q.getSqlSegment().contains("create_time <")));
    }

    private void verifyRanges() {
        verify(editorialMapper).selectList(argThat(q -> matches(q, false)));
        verify(photographyMapper).selectList(argThat(q -> matches(q, false)));
        verify(videoMapper).selectList(argThat(q -> matches(q, false)));
        verify(visualMapper).selectList(argThat(q -> matches(q, false)));
        verify(monthlySubsidyMapper).selectList(argThat(q -> matches(q, true)));
    }

    private boolean matches(Wrapper<?> wrapper, boolean subsidy) {
        if (!(wrapper instanceof QueryWrapper<?> query)) return false;
        String sql = query.getSqlSegment();
        String column = subsidy ? "subsidy_month" : "publish_date";
        var values = query.getParamNameValuePairs().values();
        return sql.contains(column + " >=") && sql.contains(column + " <=")
                && sql.contains("create_time >=") && sql.contains("create_time <")
                && values.contains(LocalDate.of(2026, 8, 31).atStartOfDay())
                && values.contains(LocalDate.of(2026, 9, 10).atStartOfDay())
                && values.contains(subsidy ? "2026-08" : LocalDate.of(2026, 8, 20))
                && values.contains(subsidy ? "2026-09" : LocalDate.of(2026, 9, 9));
    }
}
