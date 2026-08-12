package com.sja.system.service.impl;

import com.sja.system.common.ApiException;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserDept;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.service.OperationLogService;
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
                userRoleMapper, userDeptMapper, operationLogService, passwordEncoder);

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
                userRoleMapper, userDeptMapper, operationLogService, passwordEncoder);
        when(userMapper.selectCount(any())).thenReturn(0L, 0L, 1L);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.createUser(createDto(), 1L, "admin", "8"));

        assertEquals(409, exception.getStatus());
        assertEquals("该手机号已被其他用户使用", exception.getMessage());
        verify(userMapper, never()).insert(any(SysUser.class));
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
