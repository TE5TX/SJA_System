package com.sja.system.service.user;

import com.sja.system.common.ApiException;
import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.application.dto.user.UserRosterSnapshot;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.application.vo.user.UserRosterImportResultVO;
import com.sja.system.service.system.OperationLogService;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 202609181147 - 验证部分成功、基础设施中止、密码校验和逐行事务边界。
class UserRosterImportServiceTest {

    private final UserImportService previewService = mock(UserImportService.class);
    private final UserRosterRowService rowService = mock(UserRosterRowService.class);
    private final UserService userService = mock(UserService.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final UserRosterImportService service = new UserRosterImportService(
            previewService, rowService, userService, operationLogService);

    @Test
    void continuesAfterOneRowConflictAndIncludesPreviewFailures() throws Exception {
        UserRosterSnapshot snapshot = snapshot();
        stubConfirmation(snapshot);
        when(rowService.createCandidate(snapshot.candidates().get(0), 5L, 7L, "秘书长", 3)).thenReturn(101L);
        when(rowService.createCandidate(snapshot.candidates().get(1), 5L, 7L, "秘书长", 3))
                .thenThrow(ApiException.conflict("该学号已存在"));
        when(rowService.createCandidate(snapshot.candidates().get(2), 5L, 7L, "秘书长", 3)).thenReturn(103L);

        UserRosterImportResultVO result = service.confirm("token", "password", 7L, "秘书长", 3, "127.0.0.1");

        assertEquals(4, result.totalCount());
        assertEquals(2, result.importedCount());
        assertEquals(2, result.skippedCount());
        assertEquals(0, result.unprocessedCount());
        assertFalse(result.aborted());
        assertEquals(List.of(3, 5), result.failures().stream().map(UserRosterImportResultVO.Failure::rowNumber).toList());
        assertNotNull(result.failureReportBase64());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                Base64.getDecoder().decode(result.failureReportBase64())))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("Excel行号", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(CellType.STRING, sheet.getRow(1).getCell(2).getCellType());
            assertEquals(4, sheet.getRow(0).getLastCellNum());
        }
        verify(operationLogService).log(eq(7L), eq("秘书长"), eq("3"), eq("IMPORT"),
                eq("USER"), eq(null), eq("人员名单批量导入：职务=采编部员，成功=2，跳过=2，未处理=0，中止=false"),
                eq("127.0.0.1"));
    }

    @Test
    void stopsOnInfrastructureFailureWithoutCallingRemainingRows() {
        UserRosterSnapshot snapshot = snapshot();
        stubConfirmation(snapshot);
        when(rowService.createCandidate(snapshot.candidates().get(0), 5L, 7L, "秘书长", 3)).thenReturn(101L);
        when(rowService.createCandidate(snapshot.candidates().get(1), 5L, 7L, "秘书长", 3))
                .thenThrow(new CannotAcquireLockException("database unavailable"));

        UserRosterImportResultVO result = service.confirm("token", "password", 7L, "秘书长", 3, "127.0.0.1");

        assertEquals(4, result.totalCount());
        assertEquals(1, result.importedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(2, result.unprocessedCount());
        assertTrue(result.aborted());
        verify(rowService, never()).createCandidate(snapshot.candidates().get(2), 5L, 7L, "秘书长", 3);
    }

    @Test
    void checksPasswordBeforeConsumingOneUseToken() {
        doThrow(ApiException.forbidden("密码错误")).when(userService).verifyCurrentPassword(7L, "wrong");

        assertThrows(ApiException.class,
                () -> service.confirm("token", "wrong", 7L, "秘书长", 3, "127.0.0.1"));

        verify(previewService, never()).consumeRosterSnapshot(any(), any());
    }

    @Test
    void rowWriterMapsAllFieldsAndDeclaresRequiresNewTransaction() throws Exception {
        UserService creator = mock(UserService.class);
        UserRosterRowService writer = new UserRosterRowService(creator);
        UserRosterSnapshot.Candidate candidate = snapshot().candidates().get(0);

        writer.createCandidate(candidate, 5L, 7L, "秘书长", 3);

        org.mockito.ArgumentCaptor<UserCreateDTO> captor = org.mockito.ArgumentCaptor.forClass(UserCreateDTO.class);
        verify(creator).createUser(captor.capture(), eq(7L), eq("秘书长"), eq("3"));
        UserCreateDTO dto = captor.getValue();
        assertEquals(candidate.studentNo(), dto.getStudentNo());
        assertEquals(candidate.idCard(), dto.getIdCard());
        assertEquals(candidate.college(), dto.getCollege());
        assertEquals(candidate.className(), dto.getClassName());
        assertEquals(candidate.phone(), dto.getPhone());
        assertEquals(candidate.bankCard(), dto.getBankCard());
        assertEquals(candidate.deptIds(), dto.getDeptIds());
        assertEquals(List.of(5L), dto.getRoleIds());
        assertEquals(Boolean.TRUE, dto.getRequirePasswordChange());
        assertEquals(null, dto.getPassword());
        Method method = UserRosterRowService.class.getMethod("createCandidate",
                UserRosterSnapshot.Candidate.class, Long.class, Long.class, String.class, Integer.class);
        assertEquals(Propagation.REQUIRES_NEW, method.getAnnotation(Transactional.class).propagation());
    }

    private void stubConfirmation(UserRosterSnapshot snapshot) {
        when(previewService.consumeRosterSnapshot("token", 7L)).thenReturn(snapshot);
        when(previewService.validateRosterRole(5L, 3)).thenReturn(
                SysRole.builder().id(5L).roleCode(5).roleName("采编部员").isValid(1).build());
    }

    private UserRosterSnapshot snapshot() {
        return new UserRosterSnapshot(7L, 5L, 4, List.of(
                candidate(2, "202600000001"), candidate(3, "202600000002"),
                candidate(4, "202600000003")),
                List.of(new UserRosterSnapshot.SkippedRow(5, "待修正", "202600000004", "身份证号码格式不正确")));
    }

    private UserRosterSnapshot.Candidate candidate(int row, String studentNo) {
        return new UserRosterSnapshot.Candidate(row, "测试人员", studentNo, "计算机学院", "2026级1班",
                "13800000001", "440000200001010001", "1栋101", "6222000000000001",
                List.of(2L));
    }
}
