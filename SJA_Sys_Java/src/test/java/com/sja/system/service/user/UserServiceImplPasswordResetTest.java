package com.sja.system.service.user;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysUserDeptMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.dal.mysql.SysUserRoleMapper;
import com.sja.system.service.upload.AvatarStorageService;
import com.sja.system.service.system.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplPasswordResetTest {

    @Test
    void resetsAnotherUsersPasswordAndInvalidatesExistingSessions() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        OperationLogService operationLogService = mock(OperationLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = service(userMapper, operationLogService, passwordEncoder);
        SysUser target = new SysUser();
        target.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(passwordEncoder.encode("asdf123456")).thenReturn("encoded-reset-password");

        service.resetUserPassword(2L, 1L, "admin", "8");

        verify(passwordEncoder).encode("asdf123456");
        verify(userMapper).update(eq(null), any(UpdateWrapper.class));
        verify(operationLogService).log(eq(1L), eq("admin"), eq("8"), eq("UPDATE"),
                eq("USER"), eq(2L), any(String.class), eq(""));
    }

    @Test
    void refusesToResetOwnPasswordThroughManagementEndpoint() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserServiceImpl service = service(userMapper, mock(OperationLogService.class), mock(PasswordEncoder.class));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.resetUserPassword(1L, 1L, "admin", "8"));

        assertEquals(400, exception.getStatus());
        verify(userMapper, never()).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    void verifiesCurrentPasswordBeforeSensitiveBatchOperation() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = service(userMapper, mock(OperationLogService.class), passwordEncoder);
        SysUser currentUser = new SysUser();
        currentUser.setId(1L);
        currentUser.setIsValid(1);
        currentUser.setPassword("encoded-password");
        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);

        service.verifyCurrentPassword(1L, "correct-password");

        verify(passwordEncoder).matches("correct-password", "encoded-password");
    }

    @Test
    void rejectsSensitiveBatchOperationWhenCurrentPasswordIsWrong() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = service(userMapper, mock(OperationLogService.class), passwordEncoder);
        SysUser currentUser = new SysUser();
        currentUser.setId(1L);
        currentUser.setIsValid(1);
        currentUser.setPassword("encoded-password");
        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.verifyCurrentPassword(1L, "wrong-password"));

        assertEquals(403, exception.getStatus());
    }

    private UserServiceImpl service(SysUserMapper userMapper,
                                    OperationLogService operationLogService,
                                    PasswordEncoder passwordEncoder) {
        return new UserServiceImpl(userMapper, mock(SysRoleMapper.class), mock(SysDeptMapper.class),
                mock(SysUserRoleMapper.class), mock(SysUserDeptMapper.class), operationLogService,
                passwordEncoder, mock(AvatarStorageService.class));
    }
}
