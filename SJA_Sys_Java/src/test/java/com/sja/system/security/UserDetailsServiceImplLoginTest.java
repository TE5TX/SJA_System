package com.sja.system.security;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysMenuMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysRoleMenuMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
