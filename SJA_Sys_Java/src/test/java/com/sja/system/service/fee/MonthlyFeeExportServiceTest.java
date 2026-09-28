package com.sja.system.service.fee;

import com.sja.system.service.system.OperationLogService;

import com.sja.system.application.model.FeeExportFile;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.fee.MonthlyFeeExportDTO;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.dataobject.SysUserDept;
import com.sja.system.dal.dataobject.SysUserRole;
import com.sja.system.dal.mysql.MonthlyFeeExportMapper;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysUserDeptMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.dal.mysql.SysUserRoleMapper;
import com.sja.system.application.vo.fee.MonthlyFeeComponentVO;
import com.sja.system.application.vo.fee.MonthlyEditorialPlatformVO;
import org.apache.ibatis.annotations.Select;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        SysDept photography = dept(3L, "摄影部", "PHOTOGRAPHY", 3);
        SysDept visual = dept(4L, "视觉部", "VISUAL", 4);
        when(deptMapper.selectList(any())).thenReturn(Arrays.asList(presidium, editorial, photography, visual));
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(
                user(1L, "视觉主席", "440100000000000001"),
                user(2L, "小队成员", null),
                user(3L, "采编甲", "440100000000000003"),
                user(4L, "采编乙", "440100000000000004"),
                user(5L, "老师", "440100000000000005"),
                user(6L, "采编主席", "440100000000000006"),
                user(7L, "小队队长", "440100000000000007"),
                user(8L, "摄影甲", "440100000000000008")));
        when(userDeptMapper.selectList(any())).thenReturn(Arrays.asList(
                userDept(1L, 1L), userDept(1L, 4L), userDept(3L, 2L), userDept(4L, 2L),
                userDept(6L, 1L), userDept(6L, 2L), userDept(8L, 3L)));
        when(roleMapper.selectList(any())).thenReturn(Arrays.asList(
                role(1L, 1, "主席"), role(3L, 3, "秘书长"), role(2L, 2, "老师")));
        when(userRoleMapper.selectList(any())).thenReturn(Arrays.asList(
                userRole(1L, 3L), userRole(5L, 2L), userRole(6L, 1L)));
        when(exportMapper.selectMonthlyComponents(any(), any(), any(), any())).thenReturn(Arrays.asList(
                component(1L, "133.4", "主席团成员"), component(1L, "40.0", "微信统筹编辑"),
                component(2L, "50.0", "校园巡查组员"), component(3L, "20.0", null),
                component(4L, "30.0", null), component(6L, "10.0", "主席团成员"),
                component(7L, "80.0", "校园巡查组长"), component(8L, "40.0", null)));
        when(exportMapper.selectEditorialPlatforms()).thenReturn(Collections.emptyList());

        MonthlyFeeExportDTO dto = new MonthlyFeeExportDTO();
        // 202609100927 - 完成了跨月份导出查询边界和文件名的回归验证。
        dto.setStartMonth("2026-05");
        dto.setEndMonth("2026-06");
        FeeExportFile file = service.export(dto, 99L, 0, "管理员");

        verify(exportMapper).selectMonthlyComponents(
                eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 6, 30)),
                eq("2026-05"), eq("2026-06"));
        assertTrue(file.fileName().contains("2026-05至2026-06"));
        assertEquals(FeeExportFile.XLSX_CONTENT_TYPE, file.contentType());

        assertTrue(file.content().length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            assertEquals("所属部门", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("主席团成员", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("采编主席", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("视觉主席", sheet.getRow(2).getCell(1).getStringCellValue());
            // 202609281105 - 验证月份导出直接使用已保存的个人金额（含均分向上取整后的金额）。
            assertEquals("133.4+40", sheet.getRow(2).getCell(3).getStringCellValue());
            assertEquals(173.4, sheet.getRow(2).getCell(4).getNumericCellValue());
            assertEquals("主席团+微信编辑", sheet.getRow(2).getCell(5).getStringCellValue());
            assertEquals("校园巡查小组", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("小队队长", sheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals("巡查组长", sheet.getRow(3).getCell(5).getStringCellValue());
            assertEquals("小队成员", sheet.getRow(4).getCell(1).getStringCellValue());
            assertEquals("巡查", sheet.getRow(4).getCell(5).getStringCellValue());
            assertEquals("采编部", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("摄影部", sheet.getRow(7).getCell(0).getStringCellValue());
            assertEquals(403.4, sheet.getRow(8).getCell(4).getNumericCellValue());
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                assertFalse("老师".equals(row.getCell(1).getStringCellValue()));
            }
            assertEquals(3, sheet.getNumMergedRegions());
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    assertEquals(HorizontalAlignment.CENTER, cell.getCellStyle().getAlignment());
                    assertEquals(VerticalAlignment.CENTER, cell.getCellStyle().getVerticalAlignment());
                    assertEquals(BorderStyle.THIN, cell.getCellStyle().getBorderTop());
                    assertEquals(BorderStyle.THIN, cell.getCellStyle().getBorderBottom());
                    assertEquals(BorderStyle.THIN, cell.getCellStyle().getBorderLeft());
                    assertEquals(BorderStyle.THIN, cell.getCellStyle().getBorderRight());
                }
            }
        }
    }

    @Test
    void exportsChairmanInResponsibilityOrder() throws Exception {
        MonthlyFeeExportMapper exportMapper = mock(MonthlyFeeExportMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        MonthlyFeeExportService service = new MonthlyFeeExportService(exportMapper, userMapper, deptMapper,
                userDeptMapper, roleMapper, userRoleMapper, mock(OperationLogService.class));

        when(deptMapper.selectList(any())).thenReturn(Arrays.asList(
                dept(1L, "主席团", "PRESIDIUM", 1), dept(2L, "采编部", "EDITORIAL", 2),
                dept(3L, "摄影部", "PHOTOGRAPHY", 3), dept(4L, "视觉部", "VISUAL", 4),
                dept(5L, "视频部", "VIDEO", 5)));
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(
                user(1L, "主席", "1"), user(2L, "秘书长", "2"), user(3L, "采编微信", "3"),
                user(4L, "采编院网", "4"), user(5L, "采编校报", "5"), user(6L, "摄影部长", "6"),
                user(7L, "视觉部长", "7"), user(8L, "视频部长", "8")));
        when(userDeptMapper.selectList(any())).thenReturn(Arrays.asList(
                userDept(1L, 1L), userDept(2L, 1L),
                userDept(3L, 1L), userDept(3L, 2L), userDept(4L, 1L), userDept(4L, 2L),
                userDept(5L, 1L), userDept(5L, 2L), userDept(6L, 1L), userDept(6L, 3L),
                userDept(7L, 1L), userDept(7L, 4L), userDept(8L, 1L), userDept(8L, 5L)));
        when(roleMapper.selectList(any())).thenReturn(Arrays.asList(
                role(1L, 1, "主席"), role(3L, 3, "秘书长"), role(4L, 4, "部长"),
                role(5L, 5, "微信统筹编辑")));
        when(userRoleMapper.selectList(any())).thenReturn(Arrays.asList(
                userRole(1L, 1L), userRole(2L, 3L), userRole(3L, 4L), userRole(3L, 5L),
                userRole(4L, 4L), userRole(5L, 4L), userRole(6L, 4L), userRole(7L, 4L), userRole(8L, 4L)));
        when(exportMapper.selectMonthlyComponents(any(), any(), any(), any())).thenReturn(Arrays.asList(
                component(1L, "1.0", "主席团成员"), component(2L, "1.0", "主席团成员"),
                component(3L, "1.0", "主席团成员"), component(4L, "1.0", "主席团成员"),
                component(5L, "1.0", "主席团成员"), component(6L, "1.0", "主席团成员"),
                component(7L, "1.0", "主席团成员"), component(8L, "1.0", "主席团成员")));
        when(exportMapper.selectEditorialPlatforms()).thenReturn(Collections.singletonList(platform(4L, "院网")));

        MonthlyFeeExportDTO dto = new MonthlyFeeExportDTO();
        dto.setStartMonth("2026-06");
        dto.setEndMonth("2026-06");
        FeeExportFile file = service.export(dto, 99L, 0, "管理员");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            String[] expected = {"主席", "秘书长", "采编微信", "采编院网", "采编校报", "摄影部长", "视觉部长", "视频部长"};
            for (int index = 0; index < expected.length; index++) {
                assertEquals(expected[index], sheet.getRow(index + 1).getCell(1).getStringCellValue());
            }
        }
    }

    @Test
    void monthlyComponentQueryOnlyIncludesApprovedTasks() throws Exception {
        Select select = MonthlyFeeExportMapper.class
                .getMethod("selectMonthlyComponents", java.time.LocalDate.class, java.time.LocalDate.class,
                        String.class, String.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value());
        assertFalse(sql.contains("SUBMITTED"));
        assertEquals(5, sql.split("audit_status='APPROVED'", -1).length - 1);
        assertTrue(sql.contains("s.subsidy_month BETWEEN #{startMonth} AND #{endMonth}"));
        // 202609151004 - 验证已通过但未发布的稿费任务也会进入月份导出。
        assertEquals(4, sql.split("is_published=0 OR", -1).length - 1);
    }

    @Test
    void rejectsReversedMonthRangeBeforeQuerying() {
        MonthlyFeeExportMapper exportMapper = mock(MonthlyFeeExportMapper.class);
        MonthlyFeeExportService service = new MonthlyFeeExportService(exportMapper, mock(SysUserMapper.class),
                mock(SysDeptMapper.class), mock(SysUserDeptMapper.class), mock(SysRoleMapper.class),
                mock(SysUserRoleMapper.class), mock(OperationLogService.class));
        MonthlyFeeExportDTO dto = new MonthlyFeeExportDTO();
        dto.setStartMonth("2026-09");
        dto.setEndMonth("2026-08");

        ApiException error = assertThrows(ApiException.class, () ->
                service.export(dto, 99L, 0, "管理员"));

        assertEquals("开始月份不能晚于结束月份", error.getMessage());
        verifyNoInteractions(exportMapper);
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

    private MonthlyFeeComponentVO component(Long userId, String amount, String subsidyCategory) {
        MonthlyFeeComponentVO component = new MonthlyFeeComponentVO();
        component.setUserId(userId);
        component.setFeeAmount(new BigDecimal(amount));
        component.setSubsidyCategory(subsidyCategory);
        return component;
    }

    private MonthlyEditorialPlatformVO platform(Long userId, String platform) {
        MonthlyEditorialPlatformVO item = new MonthlyEditorialPlatformVO();
        item.setUserId(userId);
        item.setPlatform(platform);
        return item;
    }
}
