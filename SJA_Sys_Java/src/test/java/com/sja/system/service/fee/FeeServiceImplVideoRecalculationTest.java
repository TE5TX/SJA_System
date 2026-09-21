package com.sja.system.service.fee;

import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.dal.dataobject.BizEditorialArticle;
import com.sja.system.dal.dataobject.BizVideoWork;
import com.sja.system.dal.dataobject.BizWorkContributor;
import com.sja.system.dal.mysql.BizEditorialArticleMapper;
import com.sja.system.dal.mysql.BizVideoWorkMapper;
import com.sja.system.dal.mysql.BizWorkContributorMapper;
import com.sja.system.service.fee.FeeCalculationService;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.fee.EditorialFeeWorkAdapter;
import com.sja.system.service.fee.FeeAccessService;
import com.sja.system.service.fee.FeeCommandService;
import com.sja.system.service.fee.FeeContributorService;
import com.sja.system.service.fee.FeeQueryService;
import com.sja.system.service.fee.FeeWorkAdapterRegistry;
import com.sja.system.service.fee.FeeWorkStateService;
import com.sja.system.service.fee.VideoFeeWorkAdapter;
import com.sja.system.application.vo.fee.FeeDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplVideoRecalculationTest {

    @Mock
    private BizEditorialArticleMapper editorialMapper;
    @Mock
    private BizVideoWorkMapper videoMapper;
    @Mock
    private BizWorkContributorMapper contributorMapper;
    @Mock
    private FeeCalculationService feeCalculationService;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private FeeAccessService accessService;
    @Mock
    private FeeQueryService queryService;
    @Mock
    private FeeWorkStateService stateService;

    private FeeCommandService feeService;

    @BeforeEach
    void setUp() {
        FeeWorkAdapterRegistry registry = new FeeWorkAdapterRegistry(List.of(
                new EditorialFeeWorkAdapter(editorialMapper), new VideoFeeWorkAdapter(videoMapper)));
        feeService = new FeeCommandService(accessService, registry,
                new FeeContributorService(contributorMapper), feeCalculationService,
                queryService, stateService, operationLogService);
    }

    @Test
    void unpublishedSubmissionDoesNotPersistPublishDate() {
        FeeSubmitDTO dto = new FeeSubmitDTO();
        dto.setWorkType("EDITORIAL");
        Map<String, Object> info = new HashMap<>();
        info.put("title", "未发布稿件");
        info.put("platform", "院网");
        info.put("category", "莲峰群英");
        info.put("publishDate", LocalDate.of(2026, 8, 12));
        info.put("isPublished", 0);
        info.put("isSpecialNode", 0);
        info.put("specialMultiplier", 1);
        dto.setWorkInfo(info);

        FeeSubmitDTO.ContributorDTO contributor = new FeeSubmitDTO.ContributorDTO();
        contributor.setUserId(21L);
        contributor.setRoleInWork("AUTHOR");
        dto.setContributors(Collections.singletonList(contributor));
        when(feeCalculationService.calculateAutomaticFees("EDITORIAL", info, dto.getContributors()))
                .thenReturn(Collections.singletonList(new BigDecimal("120.0")));

        feeService.submit(1L, "admin", "管理员", 0, dto);

        ArgumentCaptor<BizEditorialArticle> articleCaptor = ArgumentCaptor.forClass(BizEditorialArticle.class);
        verify(editorialMapper).insert(articleCaptor.capture());
        assertNull(articleCaptor.getValue().getPublishDate());
        assertNull(info.get("publishDate"));
    }

    @Test
    void managerVideoEditUsesRecalculatedFeeInsteadOfSubmittedOldAmount() {
        BizVideoWork existing = new BizVideoWork();
        existing.setId(8L);
        existing.setIsValid(1);
        existing.setAuditStatus("SUBMITTED");

        BizVideoWork reloaded = new BizVideoWork();
        reloaded.setId(8L);
        reloaded.setIsValid(1);
        reloaded.setAuditStatus("SUBMITTED");
        reloaded.setVideoTitle("计费重算测试");
        reloaded.setCategory("活动新闻类视频");
        reloaded.setLikeCount(300);
        reloaded.setTotalAmount(new BigDecimal("550.0"));
        when(videoMapper.selectById(8L)).thenReturn(existing);
        when(accessService.canManageAmount(0)).thenReturn(true);
        when(stateService.auditStatus("VIDEO", 8L)).thenReturn("SUBMITTED");
        when(queryService.detail(8L, "VIDEO", 1L, 0))
                .thenReturn(detail("VIDEO", "修改前", new BigDecimal("1.0")),
                        detail("VIDEO", "计费重算测试", new BigDecimal("550.0")));

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

        feeService.update(8L, "VIDEO", dto, 1L, "管理员", "0", 0);

        ArgumentCaptor<BizWorkContributor> contributorCaptor = ArgumentCaptor.forClass(BizWorkContributor.class);
        verify(contributorMapper).insert(contributorCaptor.capture());
        assertEquals(new BigDecimal("550.0"), contributorCaptor.getValue().getFeeAmount());

        ArgumentCaptor<BizVideoWork> videoCaptor = ArgumentCaptor.forClass(BizVideoWork.class);
        verify(videoMapper, org.mockito.Mockito.atLeastOnce()).updateById(videoCaptor.capture());
        assertEquals(new BigDecimal("550.0"), videoCaptor.getAllValues().stream()
                .filter(item -> item.getTotalAmount() != null)
                .findFirst().orElseThrow(AssertionError::new).getTotalAmount());

        ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> beforeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> afterCaptor = ArgumentCaptor.forClass(String.class);
        verify(operationLogService).logChange(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("0"),
                org.mockito.ArgumentMatchers.eq("UPDATE"),
                org.mockito.ArgumentMatchers.eq("FEE"),
                org.mockito.ArgumentMatchers.eq(8L),
                detailCaptor.capture(), beforeCaptor.capture(), afterCaptor.capture(),
                org.mockito.ArgumentMatchers.anyString());
        assertTrue(detailCaptor.getValue().contains("变更"));
        assertTrue(beforeCaptor.getValue().contains("\"workInfo\""));
        assertTrue(afterCaptor.getValue().contains("\"workInfo\""));
        assertNotEquals(beforeCaptor.getValue(), afterCaptor.getValue());
    }

    @Test
    void managerEditorialEditUsesRecalculatedFeeInsteadOfSubmittedOldAmount() {
        BizEditorialArticle existing = new BizEditorialArticle();
        existing.setId(9L);
        existing.setIsValid(1);
        existing.setAuditStatus("SUBMITTED");
        when(editorialMapper.selectById(9L)).thenReturn(existing);
        when(accessService.canManageAmount(0)).thenReturn(true);
        when(stateService.auditStatus("EDITORIAL", 9L)).thenReturn("SUBMITTED");
        when(queryService.detail(9L, "EDITORIAL", 1L, 0))
                .thenReturn(detail("EDITORIAL", "修改前", new BigDecimal("1.0")),
                        detail("EDITORIAL", "采编计费重算测试", new BigDecimal("120.0")));

        FeeSubmitDTO dto = new FeeSubmitDTO();
        dto.setWorkType("EDITORIAL");
        Map<String, Object> info = new HashMap<>();
        info.put("title", "采编计费重算测试");
        info.put("platform", "院网");
        info.put("category", "莲峰群英");
        info.put("publishDate", LocalDate.of(2026, 8, 12));
        info.put("isPublished", 1);
        info.put("isSpecialNode", 0);
        info.put("specialMultiplier", 1);
        dto.setWorkInfo(info);

        FeeSubmitDTO.ContributorDTO contributor = new FeeSubmitDTO.ContributorDTO();
        contributor.setUserId(21L);
        contributor.setRoleInWork("AUTHOR");
        contributor.setFeeAmount(new BigDecimal("1.0"));
        dto.setContributors(Collections.singletonList(contributor));
        when(feeCalculationService.calculateAutomaticFees("EDITORIAL", info, dto.getContributors()))
                .thenReturn(Collections.singletonList(new BigDecimal("120.0")));

        feeService.update(9L, "EDITORIAL", dto, 1L, "管理员", "0", 0);

        ArgumentCaptor<BizWorkContributor> contributorCaptor = ArgumentCaptor.forClass(BizWorkContributor.class);
        verify(contributorMapper).insert(contributorCaptor.capture());
        assertEquals(new BigDecimal("120.0"), contributorCaptor.getValue().getFeeAmount());

        ArgumentCaptor<BizEditorialArticle> articleCaptor = ArgumentCaptor.forClass(BizEditorialArticle.class);
        verify(editorialMapper, org.mockito.Mockito.atLeastOnce()).updateById(articleCaptor.capture());
        assertEquals(new BigDecimal("120.0"), articleCaptor.getAllValues().stream()
                .filter(item -> item.getTotalAmount() != null)
                .findFirst().orElseThrow(AssertionError::new).getTotalAmount());
    }

    private FeeDetailVO detail(String workType, String title, BigDecimal total) {
        FeeDetailVO detail = new FeeDetailVO();
        detail.setWorkType(workType);
        detail.setWorkInfo(new HashMap<>(Map.of("title", title)));
        detail.setContributors(Collections.emptyList());
        detail.setStatus("SUBMITTED");
        detail.setTotalAmount(total);
        return detail;
    }
}
