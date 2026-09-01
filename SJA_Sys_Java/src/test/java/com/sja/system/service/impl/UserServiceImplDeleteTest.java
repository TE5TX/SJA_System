package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.service.AvatarStorageService;
import com.sja.system.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplDeleteTest {

    @Test
    void permanentlyDeletesAnotherUserAndRetainsAuditHistory() {
        Fixture fixture = new Fixture();
        SysUser target = targetUser();
        when(fixture.userMapper.selectById(2L)).thenReturn(target);
        when(fixture.userMapper.deleteById(2L)).thenReturn(1);

        fixture.service.deleteUser(2L, 1L, "管理员", "0");

        verify(fixture.operationLogService).detachUserReferences(2L);
        verify(fixture.userDeptMapper).delete(any(QueryWrapper.class));
        verify(fixture.userRoleMapper).delete(any(QueryWrapper.class));
        verify(fixture.userMapper).deleteById(2L);
        verify(fixture.operationLogService).log(eq(1L), eq("管理员"), eq("0"), eq("DELETE"),
                eq("USER"), eq(2L), any(String.class), eq(""));
        verify(fixture.avatarStorageService).deleteAfterCommit("/api/v1/files/avatars/photo.jpg");
    }

    @Test
    void refusesToDeleteCurrentLoginAccount() {
        Fixture fixture = new Fixture();

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.deleteUser(1L, 1L, "管理员", "0"));

        assertEquals(400, exception.getStatus());
        verify(fixture.userMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void returnsConflictWhenBusinessRecordsStillReferenceUser() {
        Fixture fixture = new Fixture();
        when(fixture.userMapper.selectById(2L)).thenReturn(targetUser());
        when(fixture.userMapper.deleteById(2L))
                .thenThrow(new DataIntegrityViolationException("foreign key constraint"));

        ApiException exception = assertThrows(ApiException.class,
                () -> fixture.service.deleteUser(2L, 1L, "管理员", "0"));

        assertEquals(409, exception.getStatus());
        verify(fixture.operationLogService, never()).log(eq(1L), any(), any(), eq("DELETE"),
                eq("USER"), eq(2L), any(), any());
    }

    private static SysUser targetUser() {
        SysUser target = new SysUser();
        target.setId(2L);
        target.setUsername("2026000000002");
        target.setRealName("待删除人员");
        target.setPhoto("/api/v1/files/avatars/photo.jpg");
        return target;
    }

    private static final class Fixture {
        private final SysUserMapper userMapper = mock(SysUserMapper.class);
        private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        private final SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        private final OperationLogService operationLogService = mock(OperationLogService.class);
        private final AvatarStorageService avatarStorageService = mock(AvatarStorageService.class);
        private final UserServiceImpl service = new UserServiceImpl(
                userMapper, mock(SysRoleMapper.class), mock(SysDeptMapper.class),
                userRoleMapper, userDeptMapper, operationLogService,
                mock(PasswordEncoder.class), avatarStorageService);
    }
}
