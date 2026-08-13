package com.sja.system.service.impl;

import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.service.AvatarStorageService;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.FeeContributorVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplFeeContributorTest {

    @Test
    void returnsDistinctSortedRoleCodesForAutomaticSubsidySelection() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, roleMapper, mock(SysDeptMapper.class),
                userRoleMapper, mock(SysUserDeptMapper.class), mock(OperationLogService.class),
                mock(PasswordEncoder.class), mock(AvatarStorageService.class));

        SysUser user = new SysUser();
        user.setId(8L);
        user.setRealName("多角色成员");
        user.setStudentNo("20260008");
        user.setIsValid(1);
        when(userMapper.selectList(any())).thenReturn(List.of(user));

        when(userRoleMapper.selectList(any())).thenReturn(Arrays.asList(
                userRole(8L, 40L), userRole(8L, 30L), userRole(8L, 40L)));
        when(roleMapper.selectList(any())).thenReturn(Arrays.asList(
                role(30L, 5), role(40L, 4)));

        List<FeeContributorVO> contributors = service.queryActiveFeeContributors();

        assertEquals(1, contributors.size());
        assertEquals(List.of(4, 5), contributors.get(0).getRoleCodes());
    }

    private SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private SysRole role(Long id, Integer code) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        return role;
    }
}
