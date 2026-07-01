package com.sja.system.service;

import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.vo.MenuTreeVO;

import java.util.List;

/**
 * System configuration service interface.
 * Provides access to roles, departments, and menu trees.
 */
public interface SystemService {

    /**
     * Get all active roles ordered by sort order.
     *
     * @return list of roles
     */
    List<SysRole> getRoleList();

    /**
     * Get all active departments ordered by sort order.
     *
     * @return list of departments
     */
    List<SysDept> getDeptList();

    /**
     * Get the full menu tree (all valid menus).
     *
     * @return hierarchical menu tree
     */
    List<MenuTreeVO> getMenuTree();

    /**
     * Get the menu tree filtered by a list of role codes.
     * Only menus accessible to the given roles are returned.
     *
     * @param roleCodes list of role codes
     * @return filtered hierarchical menu tree
     */
    List<MenuTreeVO> getMenuTreeByRoleCodes(List<Integer> roleCodes);
}
