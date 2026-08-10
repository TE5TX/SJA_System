package com.sja.system.service.impl;

import com.sja.system.common.ApiException;
import com.sja.system.dto.FeeSubmitDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysUserDept;
import com.sja.system.mapper.BizEditorialArticleMapper;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplPermissionAndDuplicateTest {

    @Mock
    private BizEditorialArticleMapper editorialMapper;
    @Mock
    private SysUserDeptMapper sysUserDeptMapper;
    @Mock
    private SysDeptMapper sysDeptMapper;

    @InjectMocks
    private FeeServiceImpl feeService;

    @Test
    void ministerCannotEditAnotherDepartmentsTask() {
        SysUserDept membership = new SysUserDept();
        membership.setUserId(93L);
        membership.setDeptId(5L);
        SysDept visual = new SysDept();
        visual.setId(5L);
        visual.setDeptCode("VISUAL");
        when(sysUserDeptMapper.selectList(any())).thenReturn(Collections.singletonList(membership));
        when(sysDeptMapper.selectById(5L)).thenReturn(visual);

        ApiException error = assertThrows(ApiException.class,
                () -> feeService.updateFee(1L, "EDITORIAL", new FeeSubmitDTO(),
                        93L, "张静文", "4", 4));

        assertEquals(403, error.getStatus());
        verify(editorialMapper, never()).updateById(any());
    }

    @Test
    void ministerCannotDeleteAnotherDepartmentsTask() {
        SysUserDept membership = new SysUserDept();
        membership.setUserId(93L);
        membership.setDeptId(5L);
        SysDept visual = new SysDept();
        visual.setId(5L);
        visual.setDeptCode("VISUAL");
        when(sysUserDeptMapper.selectList(any())).thenReturn(Collections.singletonList(membership));
        when(sysDeptMapper.selectById(5L)).thenReturn(visual);

        ApiException error = assertThrows(ApiException.class,
                () -> feeService.invalidateRejectedFee(1L, "EDITORIAL", 93L,
                        "张静文", "4", 4));

        assertEquals(403, error.getStatus());
        verify(editorialMapper, never()).deleteById(any(Long.class));
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
        verify(editorialMapper, never()).insert(any());
    }
}
