package com.sja.system.service.impl;

import com.sja.system.common.PageResult;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.BizEditorialArticleMapper;
import com.sja.system.mapper.BizMonthlySubsidyMapper;
import com.sja.system.mapper.BizPhotographyWorkMapper;
import com.sja.system.mapper.BizVideoWorkMapper;
import com.sja.system.mapper.BizVisualWorkMapper;
import com.sja.system.mapper.FeeQueryMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.model.FeeListRow;
import com.sja.system.service.fee.FeeAccessService;
import com.sja.system.service.fee.FeeQueryService;
import com.sja.system.service.fee.FeeWorkType;
import com.sja.system.vo.FeeListVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 稿费列表数据库分页与提交人批量装配回归测试。
 */
@ExtendWith(MockitoExtension.class)
class FeeServiceImplQueryPaginationTest {

    @Mock private FeeQueryMapper feeQueryMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private BizEditorialArticleMapper editorialMapper;
    @Mock private BizPhotographyWorkMapper photographyMapper;
    @Mock private BizVideoWorkMapper videoMapper;
    @Mock private BizVisualWorkMapper visualMapper;
    @Mock private BizMonthlySubsidyMapper monthlySubsidyMapper;
    @Mock private FeeAccessService accessService;
    @InjectMocks private FeeQueryService service;

    @Test
    void queryUsesDatabaseOffsetPaginationAndBatchesSubmitters() {
        FeeListRow first = row(101L, 8L, "第一篇");
        FeeListRow second = row(102L, 8L, "第二篇");
        SysUser submitter = new SysUser();
        submitter.setId(8L);
        submitter.setRealName("提交人");
        when(feeQueryMapper.countFeeItems(any())).thenReturn(500L);
        when(feeQueryMapper.selectFeeItems(any(), eq(4L), eq(2))).thenReturn(List.of(first, second));
        when(sysUserMapper.selectBatchIds(anyCollection())).thenReturn(List.of(submitter));

        when(accessService.allowedDepartmentCodes(1L, 0)).thenReturn(FeeWorkType.allDepartmentCodes());
        PageResult<FeeListVO> result = service.list(null, null, null, null, null,
                null, null, null, null, 3, 2, 1L, 0);

        // 202609201115 - 验证列表只读取目标页，并且提交人不再逐条查询。
        assertEquals(500L, result.getTotal());
        assertEquals(List.of("提交人", "提交人"), result.getList().stream()
                .map(FeeListVO::getSubmitterName).toList());
        verify(feeQueryMapper).countFeeItems(any());
        verify(feeQueryMapper).selectFeeItems(any(), eq(4L), eq(2));
        verify(sysUserMapper).selectBatchIds(anyCollection());
        verify(editorialMapper, never()).selectList(any());
        verify(photographyMapper, never()).selectList(any());
        verify(videoMapper, never()).selectList(any());
        verify(visualMapper, never()).selectList(any());
        verify(monthlySubsidyMapper, never()).selectList(any());
    }

    private FeeListRow row(Long id, Long submitterId, String title) {
        FeeListRow row = new FeeListRow();
        row.setId(id);
        row.setWorkType("EDITORIAL");
        row.setTitle(title);
        row.setSubmitterId(submitterId);
        return row;
    }
}
