package com.sja.system.service.user;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.application.dto.user.UserImportCandidateDTO;
import com.sja.system.application.dto.user.UserImportSnapshot;
import com.sja.system.application.dto.user.UserRosterSnapshot;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.framework.web.MultipartUploadedFile;
import com.sja.system.application.vo.user.UserImportPreviewVO;
import com.sja.system.application.vo.user.UserImportResultVO;
import com.sja.system.application.vo.user.UserRosterPreviewVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 202608210907 - 覆盖了批量注册预览校验、越级授权、重复学号及一次性令牌确认流程。
@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysDeptMapper sysDeptMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private UserService userService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserImportService service;

    @BeforeEach
    void setUp() {
        service = new UserImportService(sysUserMapper, sysDeptMapper, sysRoleMapper,
                userService, redisTemplate, objectMapper, "test:user-import", 600, 2 * 1024 * 1024, 500);
    }

    @Test
    void previewsValidWorkbookAndIssuesOperatorBoundToken() {
        stubSelection(5);
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UserImportPreviewVO preview = service.preview(workbook("姓名", "学号",
                        List.of(List.of("张三", "2026010101001"), List.of("李四", "2026010101002"))),
                List.of(2L), List.of(5L), 7L, 3);

        assertEquals(2, preview.getTotal());
        assertEquals(2, preview.getValidCount());
        assertEquals(0, preview.getInvalidCount());
        assertNotNull(preview.getImportToken());
        assertTrue(preview.getRows().stream().allMatch(row -> Boolean.TRUE.equals(row.getValid())));
        verify(valueOperations).set(eq("test:user-import:7:" + preview.getImportToken()),
                anyString(), eq(Duration.ofSeconds(600)));
    }

    @Test
    void reportsEveryOccurrenceOfDuplicateStudentNumberWithoutIssuingToken() {
        stubSelection(5);
        when(sysUserMapper.selectList(any())).thenReturn(List.of());

        UserImportPreviewVO preview = service.preview(workbook("姓名", "学号",
                        List.of(List.of("张三", "2026010101001"), List.of("李四", "2026010101001"))),
                List.of(2L), List.of(5L), 7L, 3);

        assertEquals(0, preview.getValidCount());
        assertEquals(2, preview.getInvalidCount());
        assertNull(preview.getImportToken());
        assertTrue(preview.getRows().stream()
                .allMatch(row -> row.getErrors().contains("学号在文件中重复")));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void rejectsUnexpectedHeaders() {
        stubSelection(5);

        ApiException error = assertThrows(ApiException.class, () -> service.preview(
                workbook("人员", "账号", List.of(List.of("张三", "2026010101001"))),
                List.of(2L), List.of(5L), 7L, 3));

        assertEquals(400, error.getStatus());
        assertTrue(error.getMessage().contains("姓名、学号"));
    }

    @Test
    void secretaryCannotAssignChairmanRole() {
        when(sysDeptMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeDept()));
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(1, "主席")));

        ApiException error = assertThrows(ApiException.class, () -> service.preview(
                workbook("姓名", "学号", List.of(List.of("张三", "2026010101001"))),
                List.of(2L), List.of(1L), 7L, 3));

        assertEquals(403, error.getStatus());
        assertTrue(error.getMessage().contains("权限高于当前用户"));
    }

    @Test
    void consumesTokenOnceAndRegistersEveryCandidateWithSafeDefaults() throws Exception {
        stubSelection(5);
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        UserImportSnapshot snapshot = new UserImportSnapshot(7L, 3, List.of(2L), List.of(5L), List.of(
                new UserImportCandidateDTO(2, "张三", "2026010101001"),
                new UserImportCandidateDTO(3, "李四", "2026010101002")));
        when(valueOperations.getAndDelete("test:user-import:7:" + token))
                .thenReturn(objectMapper.writeValueAsString(snapshot), null);
        when(userService.createUser(any(), eq(7L), eq("secretary"), eq("3")))
                .thenReturn(101L, 102L);

        UserImportResultVO result = service.confirm(token, "current-password", 7L, "secretary", 3);

        assertEquals(2, result.getImportedCount());
        assertEquals(List.of(101L, 102L), result.getUserIds());
        ArgumentCaptor<UserCreateDTO> captor = ArgumentCaptor.forClass(UserCreateDTO.class);
        verify(userService, times(2)).createUser(captor.capture(), eq(7L), eq("secretary"), eq("3"));
        for (UserCreateDTO dto : captor.getAllValues()) {
            assertNull(dto.getPassword());
            assertEquals(1, dto.getIsValid());
            assertEquals(List.of(2L), dto.getDeptIds());
            assertEquals(List.of(5L), dto.getRoleIds());
            assertFalse(dto.getStudentNo().isBlank());
        }
        ApiException reusedToken = assertThrows(ApiException.class,
                () -> service.confirm(token, "current-password", 7L, "secretary", 3));
        assertEquals(400, reusedToken.getStatus());
        assertTrue(reusedToken.getMessage().contains("已过期或已使用"));
        verify(valueOperations, times(2)).getAndDelete("test:user-import:7:" + token);
        verify(userService, times(2)).verifyCurrentPassword(7L, "current-password");
    }

    @Test
    void previewsReorderedRosterHeadersAndSkipsOnlyInvalidRows() throws Exception {
        // 202609181445 - 验证乱序九列表头、空白可选字段、重复学号及有效行令牌。
        stubRosterSelection();
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UploadedFile file = rosterWorkbook(false, false);

        UserRosterPreviewVO preview = service.previewRoster(file, 5L, 7L, 3);

        assertEquals(3, preview.total());
        assertEquals(2, preview.validCount());
        assertEquals(1, preview.invalidCount());
        assertEquals(List.of("额外信息"), preview.ignoredHeaders());
        assertEquals(2, preview.rows().get(0).rowNumber());
        assertEquals(4, preview.rows().get(2).rowNumber());
        assertTrue(preview.rows().get(2).errors().contains("学号在文件中重复"));
        assertNotNull(preview.importToken());
        ArgumentCaptor<String> snapshot = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("test:user-import:roster:v2:7:" + preview.importToken()),
                snapshot.capture(), eq(Duration.ofSeconds(600)));
        assertEquals(2, objectMapper.readTree(snapshot.getValue()).get("candidates").size());
        assertEquals(1, objectMapper.readTree(snapshot.getValue()).get("skippedRows").size());
        assertFalse(snapshot.getValue().contains("roleIds"));
        verify(userService, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void rejectsDuplicateRosterHeaderAsWholeFileError() throws Exception {
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(5, "采编部员")));
        ApiException error = assertThrows(ApiException.class,
                () -> service.previewRoster(rosterWorkbook(true, false), 5L, 7L, 3));
        assertEquals(400, error.getStatus());
        assertTrue(error.getMessage().contains("表头重复"));
    }

    @Test
    void rejectsNumericIdentifierCellsWithoutGuessingPrecision() throws Exception {
        stubRosterSelection();
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserRosterPreviewVO preview = service.previewRoster(rosterWorkbook(false, true), 5L, 7L, 3);
        assertTrue(preview.rows().get(0).errors().stream().anyMatch(error -> error.contains("文本格式")));
        assertEquals(2, preview.validCount());
    }

    @Test
    void reportsExistingStudentNumberWithoutRejectingOtherCandidates() throws Exception {
        stubRosterSelection();
        SysUser existing = new SysUser();
        existing.setStudentNo("202600000001");
        when(sysUserMapper.selectList(any())).thenReturn(List.of(existing), List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UserRosterPreviewVO preview = service.previewRoster(rosterWorkbook(false, false), 5L, 7L, 3);

        assertEquals(1, preview.validCount());
        assertEquals(2, preview.invalidCount());
        assertTrue(preview.rows().get(0).errors().contains("学号或登录账号已存在"));
        assertTrue(preview.rows().get(1).valid());
    }

    @Test
    void preventsAssigningElevatedRosterRole() throws Exception {
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(1, "主席")));

        ApiException error = assertThrows(ApiException.class,
                () -> service.previewRoster(rosterWorkbook(false, false), 1L, 7L, 3));

        assertEquals(403, error.getStatus());
        verify(sysUserMapper, never()).selectList(any());
    }

    @Test
    void rejectsRemovedMemberTypeHeader() throws Exception {
        // 202609181445 - 旧模板身份列不能被当作无关列悄悄忽略。
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(5, "采编部员")));

        ApiException error = assertThrows(ApiException.class,
                () -> service.previewRoster(rosterWorkbook(false, false, null, true), 5L, 7L, 3));

        assertEquals(400, error.getStatus());
        assertTrue(error.getMessage().contains("已移除"));
        verify(sysDeptMapper, never()).selectList(any());
    }

    @Test
    void rejectsDepartmentOutsideFourAllowedOptions() throws Exception {
        stubRosterSelection();
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UserRosterPreviewVO preview = service.previewRoster(
                rosterWorkbook(false, false, "宣传部", false), 5L, 7L, 3);

        assertFalse(preview.rows().get(0).valid());
        assertTrue(preview.rows().get(0).errors().stream()
                .anyMatch(reason -> reason.contains("只能填写采编部、摄影部、视觉部或视频部")));
        assertEquals(2, preview.validCount());
    }

    @Test
    void acceptsFourAllowedDepartmentsAndRechecksTheirNames() throws Exception {
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(5, "采编部员")));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(activeDept(),
                activeDept(3L, "摄影部"), activeDept(4L, "视觉部"), activeDept(5L, "视频部")));
        when(sysUserMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UserRosterPreviewVO preview = service.previewRoster(rosterWorkbook(false, false,
                "采编部、摄影部、视觉部、视频部", false), 5L, 7L, 3);

        assertTrue(preview.rows().get(0).valid());
        assertEquals(List.of("采编部", "摄影部", "视觉部", "视频部"),
                preview.rows().get(0).deptNames());
        when(sysDeptMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(activeDept(2L, "宣传部")));
        UserRosterSnapshot.Candidate candidate = new UserRosterSnapshot.Candidate(2, "测试人员",
                "202600000001", null, null, null, "440000200001010001", null, null, List.of(2L));

        ApiException error = assertThrows(ApiException.class,
                () -> service.validateRosterDepartments(candidate));
        assertEquals(400, error.getStatus());
        assertTrue(error.getMessage().contains("只能填写"));
    }

    @Test
    void consumesRosterTokenOnceOnlyForItsOperator() throws Exception {
        // 202609181147 - 验证人员名单令牌单次消费且与操作人及名单命名空间绑定。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        UserRosterSnapshot snapshot = new UserRosterSnapshot(7L, 5L, 1,
                List.of(new UserRosterSnapshot.Candidate(2, "测试人员", "202600000001",
                        null, null, null, "440000200001010001", null, null,
                        List.of())), List.of());
        when(valueOperations.getAndDelete("test:user-import:roster:v2:7:" + token))
                .thenReturn(objectMapper.writeValueAsString(snapshot), null);

        assertEquals(snapshot, service.consumeRosterSnapshot(token, 7L));
        ApiException replay = assertThrows(ApiException.class, () -> service.consumeRosterSnapshot(token, 7L));
        assertEquals(400, replay.getStatus());
        verify(valueOperations, times(2)).getAndDelete("test:user-import:roster:v2:7:" + token);
    }

    @Test
    void rejectsRoleChangeBetweenPreviewAndConfirmation() {
        when(sysRoleMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(SysRole.builder().id(5L).roleCode(5)
                        .roleName("采编部员").isValid(0).build()));

        ApiException error = assertThrows(ApiException.class, () -> service.validateRosterRole(5L, 3));

        assertEquals(400, error.getStatus());
        assertTrue(error.getMessage().contains("已禁用"));
    }

    private void stubRosterSelection() {
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(5, "采编部员")));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(activeDept()));
    }

    private UploadedFile rosterWorkbook(boolean duplicateHeader, boolean numericStudent) throws IOException {
        return rosterWorkbook(duplicateHeader, numericStudent, null, false);
    }

    private UploadedFile rosterWorkbook(boolean duplicateHeader, boolean numericStudent,
                                        String firstDepartment, boolean legacyMemberHeader) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("人员名单");
            String[] headers = {"学号", "姓名", "银行卡号", "团内部门", "身份证号码", "二级学院",
                    "年级专业班级", "手机号码", "宿舍地址", "额外信息"};
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            if (duplicateHeader) header.createCell(9).setCellValue("学号");
            if (legacyMemberHeader) header.createCell(9).setCellValue("正式部员/留任部员");
            String[] students = {"202600000001", "202600000002", "202600000001"};
            for (int i = 0; i < 3; i++) {
                var row = sheet.createRow(i + 1);
                if (numericStudent && i == 0) row.createCell(0).setCellValue(202600000001D);
                else row.createCell(0).setCellValue(students[i]);
                row.createCell(1).setCellValue("测试人员" + i);
                row.createCell(4).setCellValue("44000020000101000" + i);
                if (i == 0 && firstDepartment != null) row.createCell(3).setCellValue(firstDepartment);
            }
            workbook.write(output);
            return new MultipartUploadedFile(new MockMultipartFile("file", "人员名单.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()));
        }
    }

    private void stubSelection(int roleCode) {
        when(sysDeptMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeDept()));
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(roleCode, "部员")));
    }

    private SysDept activeDept() {
        return SysDept.builder().id(2L).deptName("采编部").deptCode("EDITORIAL").isValid(1).build();
    }

    private SysDept activeDept(Long id, String name) {
        return SysDept.builder().id(id).deptName(name).isValid(1).build();
    }

    private SysRole activeRole(int roleCode, String roleName) {
        return SysRole.builder().id(roleCode == 1 ? 1L : 5L).roleCode(roleCode)
                .roleName(roleName).isValid(1).build();
    }

    private UploadedFile workbook(String firstHeader, String secondHeader, List<List<String>> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output)
                .head(List.of(List.of(firstHeader), List.of(secondHeader)))
                .autoCloseStream(false)
                .sheet("人员")
                .doWrite(rows);
        return new MultipartUploadedFile(new MockMultipartFile("file", "人员批量注册.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()));
    }
}
