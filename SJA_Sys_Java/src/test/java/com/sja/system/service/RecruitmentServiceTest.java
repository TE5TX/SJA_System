package com.sja.system.service;

import com.sja.system.common.ApiException;
import com.sja.system.dto.RecruitmentRegistrationDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRecruitmentForm;
import com.sja.system.entity.SysRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRecruitmentFormMapper;
import com.sja.system.mapper.SysRoleMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

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
        when(fixture.formMapper.selectRegistrationStateForShare()).thenReturn(state(true));

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
        when(fixture.avatarStorageService.store(any())).thenReturn("/api/v1/upload/avatar/test.jpg");
        when(fixture.userService.createUser(any(), any(), any(), any())).thenReturn(88L);

        fixture.service.register(registration(), photo());

        ArgumentCaptor<UserCreateDTO> captor = ArgumentCaptor.forClass(UserCreateDTO.class);
        verify(fixture.userService).createUser(captor.capture(), eq(null),
                eq("招新注册表单"), eq("13"));
        UserCreateDTO created = captor.getValue();
        assertEquals("asdf123456", created.getPassword());
        assertEquals(java.util.List.of(2L), created.getDeptIds());
        assertEquals(java.util.List.of(13L), created.getRoleIds());
        assertEquals(1, created.getIsValid());
        verify(fixture.avatarStorageService).deleteAfterRollback("/api/v1/upload/avatar/test.jpg");
    }

    @Test
    void rejectsStatusChangesFromUnauthorizedRole() {
        Fixture fixture = fixture();

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.updateStatus(true, 9L, "部长", 4));

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
        RecruitmentService service = new RecruitmentService(formMapper, deptMapper, roleMapper,
                userService, avatarStorageService, operationLogService);
        return new Fixture(service, formMapper, deptMapper, roleMapper,
                userService, avatarStorageService);
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

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "avatar.png", "image/png", new byte[]{1});
    }

    private record Fixture(
            RecruitmentService service,
            SysRecruitmentFormMapper formMapper,
            SysDeptMapper deptMapper,
            SysRoleMapper roleMapper,
            UserService userService,
            AvatarStorageService avatarStorageService) {
    }
}
