package com.sja.system.service.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysMenu;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.dal.dataobject.SysRoleMenu;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysMenuMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.dal.mysql.SysRoleMenuMapper;
import com.sja.system.service.system.SystemService;
import com.sja.system.application.vo.system.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * System configuration service implementation.
 * Manages roles, departments, and hierarchical menu trees.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysRole> getRoleList() {
        QueryWrapper<SysRole> query = new QueryWrapper<>();
        query.eq("is_valid", 1).orderByAsc("sort_order");
        return sysRoleMapper.selectList(query);
    }

    @Override
    public List<SysDept> getDeptList() {
        QueryWrapper<SysDept> query = new QueryWrapper<>();
        query.eq("is_valid", 1).orderByAsc("sort_order");
        return sysDeptMapper.selectList(query);
    }

    @Override
    public List<MenuTreeVO> getMenuTree() {
        QueryWrapper<SysMenu> query = new QueryWrapper<>();
        query.eq("is_valid", 1).orderByAsc("sort_order");
        List<SysMenu> allMenus = sysMenuMapper.selectList(query);
        return buildTree(allMenus, 0L);
    }

    @Override
    public List<MenuTreeVO> getMenuTreeByRoleCodes(List<Integer> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Find role IDs for the given role codes
        QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
        roleQuery.in("role_code", roleCodes).eq("is_valid", 1);
        List<SysRole> roles = sysRoleMapper.selectList(roleQuery);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        // 2. Find menu IDs associated with these roles
        QueryWrapper<SysRoleMenu> roleMenuQuery = new QueryWrapper<>();
        roleMenuQuery.in("role_id", roleIds);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuQuery);
        if (roleMenus.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // 3. Load the menus
        QueryWrapper<SysMenu> menuQuery = new QueryWrapper<>();
        menuQuery.in("id", menuIds).eq("is_valid", 1).orderByAsc("sort_order");
        List<SysMenu> menus = sysMenuMapper.selectList(menuQuery);

        // 4. Build tree
        return buildTree(menus, 0L);
    }

    // ======================== Private helpers ========================

    /**
     * Recursively build a menu tree from a flat list of menus.
     *
     * @param allMenus flat list of all menus to consider
     * @param parentId the parent ID to start from (0L for root)
     * @return list of MenuTreeVO with nested children
     */
    private List<MenuTreeVO> buildTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    MenuTreeVO node = toMenuTreeVO(m);
                    node.setChildren(buildTree(allMenus, m.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    private MenuTreeVO toMenuTreeVO(SysMenu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setPermissionCode(menu.getPermissionCode());
        vo.setMenuType(menu.getMenuType());
        vo.setParentId(menu.getParentId());
        vo.setPath(menu.getPath());
        vo.setSortOrder(menu.getSortOrder());
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
