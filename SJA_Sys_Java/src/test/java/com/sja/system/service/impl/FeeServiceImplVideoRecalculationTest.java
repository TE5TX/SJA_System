package com.sja.system.service.impl;

import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.entity.BizVideoWork;
import com.sja.system.entity.BizWorkContributor;
import com.sja.system.mapper.BizVideoWorkMapper;
import com.sja.system.mapper.BizWorkContributorMapper;
import com.sja.system.service.FeeCalculationService;
import com.sja.system.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplVideoRecalculationTest {

    @Mock
    private BizVideoWorkMapper videoMapper;
    @Mock
    private BizWorkContributorMapper contributorMapper;
    @Mock
    private FeeCalculationService feeCalculationService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private FeeServiceImpl feeService;

    @Test
    void managerVideoEditUsesRecalculatedFeeInsteadOfSubmittedOldAmount() {
        BizVideoWork existing = new BizVideoWork();
        existing.setId(8L);
        existing.setIsValid(1);
        existing.setAuditStatus("SUBMITTED");
        when(videoMapper.selectById(8L)).thenReturn(existing);

        FeeSubmitDTO dto = new FeeSubmitDTO();
        dto.setWorkType("VIDEO");
        Map<String, Object> info = new HashMap<>();
        info.put("videoTitle", "计费重算测试");
        info.put("category", "活动新闻类视频");
        info.put("likeCount", 300);
        info.put("isReposted", 1);
        info.put("isComplexEdit", 0);
        info.put("publishDate", LocalDate.of(2026, 8, 11));
        info.put("isPublished", 1);
        info.put("isSpecialNode", 0);
        info.put("specialMultiplier", 1);
        dto.setWorkInfo(info);

        FeeSubmitDTO.ContributorDTO contributor = new FeeSubmitDTO.ContributorDTO();
        contributor.setUserId(20L);
        contributor.setRoleInWork("SHOOTER");
        contributor.setFeeAmount(new BigDecimal("1.0"));
        dto.setContributors(Collections.singletonList(contributor));
        when(feeCalculationService.calculateAutomaticFees("VIDEO", info, dto.getContributors()))
                .thenReturn(Collections.singletonList(new BigDecimal("550.0")));

        feeService.updateFee(8L, "VIDEO", dto, 1L, "管理员", "0", 0);

        ArgumentCaptor<BizWorkContributor> contributorCaptor = ArgumentCaptor.forClass(BizWorkContributor.class);
        verify(contributorMapper).insert(contributorCaptor.capture());
        assertEquals(new BigDecimal("550.0"), contributorCaptor.getValue().getFeeAmount());

        ArgumentCaptor<BizVideoWork> videoCaptor = ArgumentCaptor.forClass(BizVideoWork.class);
        verify(videoMapper, org.mockito.Mockito.atLeastOnce()).updateById(videoCaptor.capture());
        assertEquals(new BigDecimal("550.0"), videoCaptor.getAllValues().stream()
                .filter(item -> item.getTotalAmount() != null)
                .findFirst().orElseThrow(AssertionError::new).getTotalAmount());
    }
}
