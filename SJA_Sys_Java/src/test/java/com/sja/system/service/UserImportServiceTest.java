package com.sja.system.service;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserImportCandidateDTO;
import com.sja.system.dto.UserImportSnapshot;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.vo.UserImportPreviewVO;
import com.sja.system.vo.UserImportResultVO;
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

    private void stubSelection(int roleCode) {
        when(sysDeptMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeDept()));
        when(sysRoleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeRole(roleCode, "部员")));
    }

    private SysDept activeDept() {
        return SysDept.builder().id(2L).deptName("采编部").deptCode("EDITORIAL").isValid(1).build();
    }

    private SysRole activeRole(int roleCode, String roleName) {
        return SysRole.builder().id(roleCode == 1 ? 1L : 5L).roleCode(roleCode)
                .roleName(roleName).isValid(1).build();
    }

    private MockMultipartFile workbook(String firstHeader, String secondHeader, List<List<String>> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output)
                .head(List.of(List.of(firstHeader), List.of(secondHeader)))
                .autoCloseStream(false)
                .sheet("人员")
                .doWrite(rows);
        return new MockMultipartFile("file", "人员批量注册.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    }
}
