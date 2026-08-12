package com.sja.system.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.entity.SysMenu;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysRoleMenu;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.SysMenuMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysRoleMenuMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService that loads user information from the database
 * and builds Spring Security authorities from the user's roles and menu permissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        String identifier = loginIdentifier == null ? "" : loginIdentifier.trim();

        // 202608121431 - 增加了使用手机号和密码登录系统的功能。
        QueryWrapper<SysUser> userQuery = new QueryWrapper<>();
        userQuery.eq("username", identifier).eq("is_valid", 1);
        SysUser user = sysUserMapper.selectOne(userQuery);

        if (user == null && StringUtils.hasText(identifier) && identifier.matches(PHONE_PATTERN)) {
            QueryWrapper<SysUser> phoneQuery = new QueryWrapper<>();
            phoneQuery.eq("phone", identifier).eq("is_valid", 1);
            user = sysUserMapper.selectOne(phoneQuery);
        }

        if (user == null) {
            log.warn("User not found or inactive: {}", identifier);
            throw new UsernameNotFoundException("用户不存在或已被禁用: " + identifier);
        }

        // 2. Find user-role associations
        QueryWrapper<SysUserRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("user_id", user.getId());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(roleQuery);

        // 3. Load role details and build authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (userRoles != null && !userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());

            for (Long roleId : roleIds) {
                SysRole role = sysRoleMapper.selectById(roleId);
                if (role != null && role.getIsValid() != null && role.getIsValid() == 1) {
                    // Add ROLE_{roleCode} authority (for role-based checks)
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));

                    // Load permission codes from sys_menu via sys_role_menu
                    QueryWrapper<SysRoleMenu> rmQuery = new QueryWrapper<>();
                    rmQuery.eq("role_id", roleId);
                    List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(rmQuery);

                    for (SysRoleMenu rm : roleMenus) {
                        SysMenu menu = sysMenuMapper.selectById(rm.getMenuId());
                        if (menu != null && menu.getIsValid() != null && menu.getIsValid() == 1
                                && menu.getPermissionCode() != null && !menu.getPermissionCode().isEmpty()) {
                            authorities.add(new SimpleGrantedAuthority(menu.getPermissionCode()));
                        }
                    }
                }
            }
        }

        // Deduplicate authorities
        authorities = authorities.stream().distinct().collect(Collectors.toList());

        // If no authorities found, add a default
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_13"));
        }

        log.debug("Loaded user: {}, authorities: {}", user.getUsername(), authorities);

        return new User(user.getUsername(), user.getPassword(), authorities);
    }
}
