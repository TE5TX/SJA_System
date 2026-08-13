package com.sja.system.service.impl;

import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.entity.BizEditorialArticle;
import com.sja.system.entity.BizWorkContributor;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserDept;
import com.sja.system.mapper.BizEditorialArticleMapper;
import com.sja.system.mapper.BizWorkContributorMapper;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.vo.FeeDetailVO;
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

    @InjectMocks
    private FeeServiceImpl feeService;

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
        author.setRealName("测试作者");
        author.setStudentNo("2026000000001");
        when(sysUserMapper.selectById(8L)).thenReturn(author);

        FeeDetailVO detail = feeService.getFeeDetail(1L, "EDITORIAL", 93L, 4);

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

        ApiException error = assertThrows(ApiException.class,
                () -> feeService.submitFee(2L, "secretary", "秘书长", 3, dto));

        assertEquals(409, error.getStatus());
        org.mockito.Mockito.verify(editorialMapper, org.mockito.Mockito.never()).insert(any());
    }
}
