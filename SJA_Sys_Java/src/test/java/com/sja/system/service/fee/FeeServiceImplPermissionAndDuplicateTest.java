package com.sja.system.service.fee;

import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.FeeSubmitDTO;
import com.sja.system.dal.dataobject.BizEditorialArticle;
import com.sja.system.dal.dataobject.BizWorkContributor;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.dataobject.SysUserDept;
import com.sja.system.dal.mysql.BizEditorialArticleMapper;
import com.sja.system.dal.mysql.BizWorkContributorMapper;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.dal.mysql.SysUserDeptMapper;
import com.sja.system.service.fee.FeeCalculationService;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.fee.EditorialFeeWorkAdapter;
import com.sja.system.service.fee.FeeAccessService;
import com.sja.system.service.fee.FeeCommandService;
import com.sja.system.service.fee.FeeContributorService;
import com.sja.system.service.fee.FeeQueryService;
import com.sja.system.service.fee.FeeWorkAdapterRegistry;
import com.sja.system.service.fee.FeeWorkStateService;
import com.sja.system.application.vo.fee.FeeDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplPermissionAndDuplicateTest {

    @Mock
    private BizEditorialArticleMapper editorialMapper;
    @Mock
    private SysUserDeptMapper sysUserDeptMapper;
    @Mock
    private SysDeptMapper sysDeptMapper;
    @Mock
    private BizWorkContributorMapper contributorMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private FeeAccessService accessService;
    @Mock
    private FeeContributorService contributorService;
    @Mock
    private FeeCalculationService calculationService;
    @Mock
    private FeeWorkStateService stateService;
    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private FeeQueryService queryService;

    @Test
    void ministerCanViewAnyDepartmentButNeverReceivesAmounts() {
        BizEditorialArticle article = new BizEditorialArticle();
        article.setId(1L);
        article.setTitle("跨部门稿件");
        article.setPlatform("微信");
        article.setIsValid(1);
        article.setIsSpecialNode(1);
        article.setSpecialMultiplier(3);
        article.setTotalAmount(new BigDecimal("300.00"));
        article.setAuditStatus("SUBMITTED");
        when(editorialMapper.selectById(1L)).thenReturn(article);

        BizWorkContributor contributor = new BizWorkContributor();
        contributor.setUserId(8L);
        contributor.setRoleInWork("AUTHOR");
        contributor.setFeeAmount(new BigDecimal("300.00"));
        when(contributorMapper.selectList(any())).thenReturn(Collections.singletonList(contributor));

        SysUser author = new SysUser();
        author.setId(8L);
        author.setRealName("测试作者");
        author.setStudentNo("2026000000001");
        when(sysUserMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(author));

        FeeDetailVO detail = queryService.detail(1L, "EDITORIAL", 93L, 4);

        assertEquals("跨部门稿件", detail.getWorkInfo().get("title"));
        assertNull(detail.getTotalAmount());
        assertNull(detail.getContributors().get(0).getFeeAmount());
        assertFalse(detail.getWorkInfo().containsKey("isSpecialNode"));
        assertFalse(detail.getWorkInfo().containsKey("specialMultiplier"));
    }

    @Test
    void duplicateArticleIsRejectedWithConflict() {
        when(editorialMapper.selectCount(any())).thenReturn(1L);
        FeeSubmitDTO dto = new FeeSubmitDTO();
        dto.setWorkType("EDITORIAL");
        Map<String, Object> info = new HashMap<>();
        info.put("title", "重复稿件");
        info.put("platform", "校报");
        info.put("category", "原创一般稿件");
        info.put("publishDate", LocalDate.of(2026, 8, 10));
        dto.setWorkInfo(info);
        FeeSubmitDTO.ContributorDTO contributor = new FeeSubmitDTO.ContributorDTO();
        contributor.setUserId(2L);
        contributor.setRoleInWork("AUTHOR");
        dto.setContributors(Collections.singletonList(contributor));

        EditorialFeeWorkAdapter adapter = new EditorialFeeWorkAdapter(editorialMapper);
        FeeWorkAdapterRegistry registry = new FeeWorkAdapterRegistry(Collections.singletonList(adapter));
        FeeCommandService commandService = new FeeCommandService(accessService, registry, contributorService,
                calculationService, queryService, stateService, operationLogService);

        ApiException error = assertThrows(ApiException.class,
                () -> commandService.submit(2L, "secretary", "秘书长", 3, dto));

        assertEquals(409, error.getStatus());
        org.mockito.Mockito.verify(editorialMapper, org.mockito.Mockito.never())
                .insert(any(BizEditorialArticle.class));
    }
}
