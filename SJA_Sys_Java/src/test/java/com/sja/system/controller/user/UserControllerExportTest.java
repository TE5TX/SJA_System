package com.sja.system.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.UserExportFile;
import com.sja.system.application.dto.user.UserExportDTO;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.service.user.UserImportService;
import com.sja.system.service.user.UserExportService;
import com.sja.system.service.user.UserRosterImportService;
import com.sja.system.service.user.UserService;
import com.sja.system.service.async.AsyncTaskQueueService;
import com.sja.system.service.user.UserApplicationService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserControllerExportTest {

    @Test
    void downloadsEmptyTwoColumnBatchRegistrationTemplate() throws Exception {
        UserController controller = new UserController(applicationServiceWithTemplates());
        ResponseEntity<byte[]> response = controller.downloadUserImportTemplate();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("filename*=UTF-8''"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            assertEquals("批量注册人员", workbook.getSheetAt(0).getSheetName());
            assertEquals("姓名", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("学号", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals(0, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    void downloadsRosterTemplateWithAllHeadersAndTextIdentifierColumns() throws Exception {
        // 202609181445 - 验证九列模板移除成员身份且证件、账号字段保持文本格式。
        UserController controller = new UserController(applicationServiceWithTemplates());
        ResponseEntity<byte[]> response = controller.downloadUserRosterImportTemplate();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("filename*=UTF-8''"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("人员批量导入", sheet.getSheetName());
            List<String> headers = List.of("姓名", "学号", "二级学院", "年级专业班级", "手机号码",
                    "身份证号码", "宿舍地址", "团内部门", "银行卡号");
            for (int index = 0; index < headers.size(); index++) {
                assertEquals(headers.get(index), sheet.getRow(0).getCell(index).getStringCellValue());
            }
            for (int index : List.of(1, 4, 5, 8)) {
                assertEquals("@", sheet.getColumnStyle(index).getDataFormatString());
            }
            assertEquals(0, sheet.getLastRowNum());
        }
    }

    @Test
    void delegatesUserExportAndWritesApplicationServiceFile() throws Exception {
        UserApplicationService applicationService = mock(UserApplicationService.class);
        UserController controller = new UserController(applicationService);
        CurrentActor actor = new CurrentActor(1L, "管理员", 0);

        UserExportDTO dto = new UserExportDTO();
        dto.setFields(Arrays.asList("studentNo", "realName", "deptNames"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        byte[] content = "test-workbook".getBytes();
        when(applicationService.export(dto, actor, request.getRemoteAddr()))
                .thenReturn(Optional.of(new UserExportFile("人员信息.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content)));

        ResponseEntity<byte[]> response = controller.exportUsers(dto, actor, request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getContentType().toString()
                .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("filename*=UTF-8''"));
        assertEquals("test-workbook", new String(response.getBody()));
    }

    @Test
    void returnsNoContentWhenUserExportHasNoRows() {
        UserApplicationService applicationService = mock(UserApplicationService.class);
        UserController controller = new UserController(applicationService);
        when(applicationService.export(any(), any(), any())).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = controller.exportUsers(new UserExportDTO(),
                new CurrentActor(1L, "管理员", 0), new MockHttpServletRequest());

        // 202609201621 - 无可导出数据时保持204响应，不生成空附件。
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getHeaders().getFirst("Content-Disposition"));
    }

    private UserImportService templateService() {
        return new UserImportService(mock(SysUserMapper.class), mock(SysDeptMapper.class),
                mock(SysRoleMapper.class), mock(UserService.class), null, new ObjectMapper(),
                "test:user-import", 600, 2 * 1024 * 1024, 500);
    }

    private UserApplicationService applicationServiceWithTemplates() {
        return new UserApplicationService(mock(UserService.class), templateService(),
                mock(UserRosterImportService.class), mock(UserExportService.class),
                mock(AsyncTaskQueueService.class),
                mock(com.sja.system.service.storage.ImportArchiveService.class));
    }
}
