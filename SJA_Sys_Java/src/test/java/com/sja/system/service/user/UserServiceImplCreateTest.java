package com.sja.system.service.user;

import com.sja.system.common.ApiException;
import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.dataobject.SysUserDept;
import com.sja.system.dal.dataobject.SysUserRole;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysUserDeptMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.dal.mysql.SysUserRoleMapper;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.upload.AvatarStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class UserServiceImplCreateTest {

    @Test
    void createsUserWithEncryptedPasswordAndRelations() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, roleMapper, deptMapper,
                userRoleMapper, userDeptMapper, operationLogService, passwordEncoder,
                mock(AvatarStorageService.class));

        SysDept dept = new SysDept();
        dept.setId(2L);
        dept.setIsValid(1);
        SysRole role = new SysRole();
        role.setId(15L);
        role.setIsValid(1);
        when(deptMapper.selectById(2L)).thenReturn(dept);
        when(roleMapper.selectById(15L)).thenReturn(role);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        });

        UserCreateDTO dto = createDto();
        Long userId = service.createUser(dto, 1L, "admin", "8");

        assertEquals(99L, userId);
        verify(passwordEncoder).encode("123456");
        verify(userMapper).insert(any(SysUser.class));
        verify(userDeptMapper).insert(any(SysUserDept.class));
        verify(userRoleMapper).insert(any(SysUserRole.class));
        verify(operationLogService).log(eq(1L), eq("admin"), eq("8"), eq("CREATE"),
                eq("USER"), eq(99L), any(String.class), eq(""));
    }

    @Test
    void rejectsDuplicatePhoneBeforeCreatingUser() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, roleMapper, deptMapper,
                userRoleMapper, userDeptMapper, operationLogService, passwordEncoder,
                mock(AvatarStorageService.class));
        when(userMapper.selectCount(any())).thenReturn(0L, 0L, 1L);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createUser(createDto(), 1L, "admin", "8"));

        assertEquals(409, exception.getStatus());
        assertEquals("该手机号已被其他用户使用", exception.getMessage());
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void createsUserWithOnlyStudentNumberAndSafeDefaults() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, mock(SysRoleMapper.class),
                mock(SysDeptMapper.class), mock(SysUserRoleMapper.class), mock(SysUserDeptMapper.class),
                mock(OperationLogService.class), passwordEncoder, mock(AvatarStorageService.class));
        when(passwordEncoder.encode("asdf123456")).thenReturn("encoded-default-password");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setId(100L);
            return 1;
        });

        UserCreateDTO dto = new UserCreateDTO();
        dto.setStudentNo("2026000000002");
        Long userId = service.createUser(dto, 1L, "admin", "8");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCaptor.capture());
        SysUser created = userCaptor.getValue();
        assertEquals(100L, userId);
        assertEquals("2026000000002", created.getRealName());
        assertEquals(0, created.getGender());
        assertEquals(1, created.getIsValid());
        assertEquals(1, created.getMustChangePassword());
        assertEquals("encoded-default-password", created.getPassword());
        verify(passwordEncoder).encode("asdf123456");
    }

    @Test
    void usesNewUserIdForSelfRegistrationAuditWhenOperatorIsAnonymous() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, mock(SysRoleMapper.class),
                mock(SysDeptMapper.class), mock(SysUserRoleMapper.class), mock(SysUserDeptMapper.class),
                operationLogService, passwordEncoder, mock(AvatarStorageService.class));
        when(passwordEncoder.encode("Test123456")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setId(101L);
            return 1;
        });

        UserCreateDTO dto = new UserCreateDTO();
        dto.setRealName("公开注册用户");
        dto.setStudentNo("2026000000003");
        dto.setPassword("Test123456");

        service.createUser(dto, null, "公开注册用户", "13");

        verify(operationLogService).log(eq(101L), eq("公开注册用户"), eq("13"), eq("CREATE"),
                eq("USER"), eq(101L), any(String.class), eq(""));
    }

    private UserCreateDTO createDto() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setRealName("测试人员");
        dto.setGender(1);
        dto.setStudentNo("2026000000001");
        dto.setPassword("123456");
        dto.setIdCard("440000200001010001");
        dto.setPhone("13800000000");
        dto.setCollege("计算机学院");
        dto.setClassName("26计科A班");
        dto.setDormAddress("1栋101");
        dto.setBankCard("6222000000000000");
        dto.setDeptIds(Collections.singletonList(2L));
        dto.setRoleIds(Collections.singletonList(15L));
        dto.setIsValid(1);
        dto.setRemark("正式部员");
        return dto;
    }
}
