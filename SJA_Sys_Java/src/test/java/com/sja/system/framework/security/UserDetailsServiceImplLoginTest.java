package com.sja.system.framework.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sja.system.application.port.ActiveUserAccessPort.ActiveUserAccess;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.mysql.SysMenuMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysRoleMenuMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.dal.mysql.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplLoginTest {

    @Test
    void loadsCanonicalAccountByPhoneNumber() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, userRoleMapper,
                roleMapper, roleMenuMapper, menuMapper);

        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("2026000000001");
        user.setPassword("encoded-password");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, user);
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        UserDetails details = service.loadUserByUsername(" 13800000000 ");

        assertEquals("2026000000001", details.getUsername());
        assertEquals("encoded-password", details.getPassword());
    }

    @Test
    void exposesActiveUserThroughTheApplicationPort() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userMapper, userRoleMapper,
                mock(SysRoleMapper.class), mock(SysRoleMenuMapper.class), mock(SysMenuMapper.class));
        SysUser user = new SysUser();
        user.setId(9L);
        user.setUsername("2026000000001");
        user.setRealName("测试用户");
        user.setPassword("encoded-password");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(userRoleMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        // 202609101503 - 完成了有效用户权限端口适配结果的测试。
        ActiveUserAccess access = service.findActiveUserById(9L).orElseThrow();

        assertEquals("2026000000001", access.username());
        assertEquals("测试用户", access.realName());
        assertTrue(access.hasAuthority("ROLE_13"));
    }
}
