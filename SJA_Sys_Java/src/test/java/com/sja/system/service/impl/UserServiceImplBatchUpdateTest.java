package com.sja.system.service.impl;

import com.sja.system.dto.UserBatchUpdateDTO;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.AvatarStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplBatchUpdateTest {

    @Test
    void replacesRolesAndUpdatesStatusForEverySelectedUser() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        UserServiceImpl service = new UserServiceImpl(
                userMapper,
                roleMapper,
                mock(SysDeptMapper.class),
                userRoleMapper,
                mock(SysUserDeptMapper.class),
                operationLogService,
                mock(PasswordEncoder.class),
                mock(AvatarStorageService.class));

        SysRole role = new SysRole();
        role.setId(8L);
        role.setRoleName("部长");
        role.setIsValid(1);
        when(roleMapper.selectById(8L)).thenReturn(role);

        SysUser firstUser = new SysUser();
        firstUser.setId(11L);
        firstUser.setRealName("用户甲");
        SysUser secondUser = new SysUser();
        secondUser.setId(12L);
        secondUser.setRealName("用户乙");
        when(userMapper.selectById(11L)).thenReturn(firstUser);
        when(userMapper.selectById(12L)).thenReturn(secondUser);

        UserBatchUpdateDTO dto = new UserBatchUpdateDTO();
        dto.setUserIds(Arrays.asList(11L, 12L));
        dto.setRoleIds(Collections.singletonList(8L));
        dto.setIsValid(0);

        service.batchUpdateUsers(dto, 1L, "管理员", "0");

        verify(userRoleMapper, times(2)).delete(any());
        verify(userRoleMapper, times(2)).insert(any());
        verify(userMapper, times(2)).update(any(), any());
        verify(operationLogService, times(2)).log(
                eq(1L), eq("管理员"), eq("0"), eq("UPDATE"), eq("USER"), any(), any(), eq(""));
    }
}
