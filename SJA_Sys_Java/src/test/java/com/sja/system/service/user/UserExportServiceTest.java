package com.sja.system.service.user;

import com.sja.system.service.system.OperationLogService;

import com.sja.system.application.dto.user.UserExportDTO;
import com.sja.system.application.vo.user.UserVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserExportServiceTest {

    @Test
    void generatesSelectedFieldsInOrderAndAuditsExport() throws Exception {
        UserService userService = mock(UserService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        UserExportService service = new UserExportService(userService, operationLogService);
        UserVO user = new UserVO();
        user.setRealName("测试用户");
        user.setStudentNo("001234567890");
        user.setDeptNames(Arrays.asList("采编部", "视觉部"));
        when(userService.queryUsersForExport(any())).thenReturn(Collections.singletonList(user));
        UserExportDTO dto = new UserExportDTO();
        dto.setFields(Arrays.asList("studentNo", "realName", "deptNames"));

        var file = service.export(dto, 1L, "管理员", 0, "127.0.0.1");

        assertTrue(file.isPresent());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.get().content()))) {
            assertEquals("人员信息", workbook.getSheetAt(0).getSheetName());
            assertEquals("学号", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("部门", workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue());
            assertEquals("001234567890", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("测试用户", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("采编部、视觉部", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
        }
        verify(operationLogService).log(1L, "管理员", "0", "EXPORT", "USER", null,
                "导出人员信息，共 1 条，字段：[studentNo, realName, deptNames]", "127.0.0.1");
    }
}
