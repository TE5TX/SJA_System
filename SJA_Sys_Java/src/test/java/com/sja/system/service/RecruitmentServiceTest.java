package com.sja.system.service;

import com.sja.system.common.ApiException;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.dto.RecruitmentRegistrationDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRecruitmentForm;
import com.sja.system.entity.SysRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRecruitmentFormMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.infrastructure.web.MultipartUploadedFile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecruitmentServiceTest {

    @Test
    void rejectsRegistrationWhileFormIsPaused() {
        Fixture fixture = fixture();
        SysRecruitmentForm state = state(false);
        when(fixture.formMapper.selectRegistrationStateForShare()).thenReturn(state);

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.register(registration(), photo()));

        assertEquals(409, exception.getStatus());
        verify(fixture.userService, never()).createUser(any(), any(), any(), any());
        verify(fixture.avatarStorageService, never()).store(any());
    }

    @Test
    void createsPublishedRegistrationAsVisitorInSelectedDepartment() {
        Fixture fixture = fixture();
        SysRecruitmentForm state = state(true);
        state.setPublishStartAt(LocalDateTime.now().minusMinutes(1));
        when(fixture.formMapper.selectRegistrationStateForShare()).thenReturn(state);

        SysDept department = new SysDept();
        department.setId(2L);
        department.setDeptCode("EDITORIAL");
        department.setIsValid(1);
        SysRole visitor = new SysRole();
        visitor.setId(13L);
        visitor.setRoleCode(13);
        visitor.setIsValid(1);
        when(fixture.deptMapper.selectOne(any())).thenReturn(department);
        when(fixture.roleMapper.selectOne(any())).thenReturn(visitor);
        when(fixture.userService.createUser(any(), any(), any(), any())).thenReturn(88L);

        fixture.service.register(registration(), null);

        ArgumentCaptor<UserCreateDTO> captor = ArgumentCaptor.forClass(UserCreateDTO.class);
        verify(fixture.userService).createUser(captor.capture(), eq(null),
                eq("测试新生"), eq("13"));
        UserCreateDTO created = captor.getValue();
        assertEquals("Test123456", created.getPassword());
        assertEquals(false, created.getRequirePasswordChange());
        assertEquals(null, created.getPhoto());
        assertEquals(java.util.List.of(2L), created.getDeptIds());
        assertEquals(java.util.List.of(13L), created.getRoleIds());
        assertEquals(1, created.getIsValid());
        verify(fixture.avatarStorageService, never()).store(any());
    }

    @Test
    void rejectsRegistrationWhenCaptchaWasNotConsumed() {
        Fixture fixture = fixture();
        when(fixture.loginCaptchaService.consume("captcha-token")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.register(registration(), null));

        // 202609181625 - 验证招新报名通过服务层消费验证码，不能绕过 HTTP Controller。
        assertEquals(400, exception.getStatus());
        verify(fixture.formMapper, never()).selectRegistrationStateForShare();
    }

    @Test
    void rejectsRegistrationOutsideTheConfiguredTimeWindow() {
        Fixture fixture = fixture();
        SysRecruitmentForm state = state(true);
        state.setPublishStartAt(LocalDateTime.now().plusMinutes(1));
        when(fixture.formMapper.selectRegistrationStateForShare()).thenReturn(state);

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.register(registration(), null));

        assertEquals(409, exception.getStatus());
        assertEquals("注册尚未开始", exception.getMessage());
        verify(fixture.userService, never()).createUser(any(), any(), any(), any());
    }

    @Test
    void rejectsStatusTimeRangeWhoseEndIsNotAfterItsStart() {
        Fixture fixture = fixture();
        when(fixture.formMapper.selectRegistrationStateForUpdate()).thenReturn(state(false));
        LocalDateTime start = LocalDateTime.now().plusHours(1);

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.updateStatus(true, start, start, 1L, "管理员", 0));

        assertEquals(400, exception.getStatus());
    }

    @Test
    void rejectsStatusChangesFromUnauthorizedRole() {
        Fixture fixture = fixture();

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.updateStatus(true, null, null, 9L, "部长", 4));

        assertEquals(403, exception.getStatus());
        verify(fixture.formMapper, never()).selectRegistrationStateForUpdate();
    }

    private Fixture fixture() {
        SysRecruitmentFormMapper formMapper = mock(SysRecruitmentFormMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        UserService userService = mock(UserService.class);
        AvatarStorageService avatarStorageService = mock(AvatarStorageService.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        LoginCaptchaService loginCaptchaService = mock(LoginCaptchaService.class);
        when(loginCaptchaService.consume(any())).thenReturn(true);
        RecruitmentService service = new RecruitmentService(formMapper, deptMapper, roleMapper,
                userService, avatarStorageService, operationLogService, loginCaptchaService);
        return new Fixture(service, formMapper, deptMapper, roleMapper,
                userService, avatarStorageService, loginCaptchaService);
    }

    private SysRecruitmentForm state(boolean published) {
        SysRecruitmentForm state = new SysRecruitmentForm();
        state.setId(1L);
        state.setIsPublished(published ? 1 : 0);
        return state;
    }

    private RecruitmentRegistrationDTO registration() {
        RecruitmentRegistrationDTO dto = new RecruitmentRegistrationDTO();
        dto.setRealName("测试新生");
        dto.setStudentNo("202600000001");
        dto.setPassword("Test123456");
        dto.setConfirmPassword("Test123456");
        dto.setGender(1);
        dto.setPhone("13800000000");
        dto.setIdCard("440000200001010001");
        dto.setCollege("计算机学院");
        dto.setClassName("26计科A班");
        dto.setDormAddress("1栋101");
        dto.setBankCard("6222000000000000");
        dto.setDeptCode("EDITORIAL");
        dto.setCaptchaToken("captcha-token");
        return dto;
    }

    private UploadedFile photo() {
        return new MultipartUploadedFile(
                new MockMultipartFile("photo", "avatar.png", "image/png", new byte[]{1}));
    }

    private record Fixture(
            RecruitmentService service,
            SysRecruitmentFormMapper formMapper,
            SysDeptMapper deptMapper,
            SysRoleMapper roleMapper,
            UserService userService,
            AvatarStorageService avatarStorageService,
            LoginCaptchaService loginCaptchaService) {
    }
}
