package com.sja.system.controller;

import com.sja.system.dto.UserExportDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserImportService;
import com.sja.system.service.UserService;
import com.sja.system.vo.UserVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerExportTest {

    @Test
    void downloadsEmptyTwoColumnBatchRegistrationTemplate() throws Exception {
        UserController controller = new UserController(mock(UserService.class), mock(UserImportService.class),
                mock(OperationLogService.class), mock(SecurityUtils.class));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.downloadUserImportTemplate(response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("批量注册人员", workbook.getSheetAt(0).getSheetName());
            assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("学号", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals(0, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    void exportsSelectedFieldsInOrderAsXlsx() throws Exception {
        UserService userService = mock(UserService.class);
        // 202608210907 - 为人员控制器新增的批量注册服务补充单元测试依赖。
        UserImportService userImportService = mock(UserImportService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UserController controller = new UserController(userService, userImportService, operationLogService, securityUtils);

        UserVO user = new UserVO();
        user.setRealName("测试用户");
        user.setStudentNo("001234567890");
        user.setDeptNames(Arrays.asList("采编部", "视觉部"));
        when(userService.queryUsersForExport(any())).thenReturn(Collections.singletonList(user));
        when(securityUtils.getCurrentUserId(any())).thenReturn(1L);
        when(securityUtils.getCurrentUsernameFromToken(any())).thenReturn("管理员");
        when(securityUtils.getCurrentRoleCode(any())).thenReturn(0);

        UserExportDTO dto = new UserExportDTO();
        dto.setFields(Arrays.asList("studentNo", "realName", "deptNames"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.exportUsers(dto, request, response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertTrue(response.getHeader("Content-Disposition").contains(".xlsx"));
        assertTrue(response.getContentAsByteArray().length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("人员信息", workbook.getSheetAt(0).getSheetName());
            assertEquals("学号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("部门", workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue());
            assertEquals("001234567890", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("测试用户", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("采编部、视觉部", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }

        verify(operationLogService).log(1L, "管理员", "0", "EXPORT", "USER", null,
                "导出人员信息，共 1 条，字段：[studentNo, realName, deptNames]", request.getRemoteAddr());
    }
}
