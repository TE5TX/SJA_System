package com.sja.system.service;

import com.sja.system.dto.MonthlyFeeExportDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserDept;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.MonthlyFeeExportMapper;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.vo.MonthlyFeeComponentVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyFeeExportServiceTest {

    @Test
    void exportsMembersInGroupOrderWithAuditableFeeExpression() throws Exception {
        MonthlyFeeExportMapper exportMapper = mock(MonthlyFeeExportMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        OperationLogService logService = mock(OperationLogService.class);
        MonthlyFeeExportService service = new MonthlyFeeExportService(exportMapper, userMapper, deptMapper,
                userDeptMapper, roleMapper, userRoleMapper, logService);

        SysDept presidium = dept(1L, "主席团", "PRESIDIUM", 1);
        SysDept editorial = dept(2L, "采编部", "EDITORIAL", 2);
        when(deptMapper.selectList(any())).thenReturn(Arrays.asList(presidium, editorial));
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(
                user(1L, "主席成员", "440100000000000001"),
                user(2L, "小队成员", null),
                user(3L, "采编甲", "440100000000000003"),
                user(4L, "采编乙", "440100000000000004"),
                user(5L, "老师", "440100000000000005")));
        when(userDeptMapper.selectList(any())).thenReturn(Arrays.asList(
                userDept(1L, 1L), userDept(3L, 2L), userDept(4L, 2L)));
        when(roleMapper.selectList(any())).thenReturn(Arrays.asList(
                role(3L, 3, "秘书长"), role(12L, 12, "校园小队队员"), role(2L, 2, "老师")));
        when(userRoleMapper.selectList(any())).thenReturn(Arrays.asList(
                userRole(1L, 3L), userRole(2L, 12L), userRole(5L, 2L)));
        when(exportMapper.selectMonthlyComponents(any(), any(), any())).thenReturn(Arrays.asList(
                component(1L, "30.0"), component(1L, "40.0"), component(2L, "50.0")));

        MonthlyFeeExportDTO dto = new MonthlyFeeExportDTO();
        dto.setMonth("2026-06");
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.export(dto, 99L, 0, "管理员", response);

        assertTrue(response.getContentAsByteArray().length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            assertEquals("所属部门", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("主席团成员", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("主席成员", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("30+40", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals(70.0, sheet.getRow(1).getCell(4).getNumericCellValue());
            assertEquals("校园巡查小组", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("采编部", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("采编乙", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals(120.0, sheet.getRow(5).getCell(4).getNumericCellValue());
            assertEquals(1, sheet.getNumMergedRegions());
        }
    }

    private SysUser user(Long id, String name, String idCard) {
        return SysUser.builder().id(id).realName(name).idCard(idCard).isValid(1).build();
    }

    private SysDept dept(Long id, String name, String code, int order) {
        return SysDept.builder().id(id).deptName(name).deptCode(code).sortOrder(order).isValid(1).build();
    }

    private SysRole role(Long id, int code, String name) {
        return SysRole.builder().id(id).roleCode(code).roleName(name).isValid(1).build();
    }

    private SysUserDept userDept(Long userId, Long deptId) {
        return SysUserDept.builder().userId(userId).deptId(deptId).build();
    }

    private SysUserRole userRole(Long userId, Long roleId) {
        return SysUserRole.builder().userId(userId).roleId(roleId).build();
    }

    private MonthlyFeeComponentVO component(Long userId, String amount) {
        MonthlyFeeComponentVO component = new MonthlyFeeComponentVO();
        component.setUserId(userId);
        component.setFeeAmount(new BigDecimal(amount));
        return component;
    }
}
