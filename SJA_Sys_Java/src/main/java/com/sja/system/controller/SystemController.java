package com.sja.system.controller;

import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.common.ApiException;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.SystemService;
import com.sja.system.vo.MenuTreeVO;
import com.sja.system.vo.OperationLogVO;
import com.sja.system.dto.OperationLogQueryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * System management controller.
 * Provides access to roles, departments, menus, and operation logs.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;
    private final OperationLogService operationLogService;
    private final SecurityUtils securityUtils;

    /**
     * Get all active roles.
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('sys:role:view')")
    public Result<List<SysRole>> getRoleList() {
        List<SysRole> roles = systemService.getRoleList();
        return Result.success(roles);
    }

    /**
     * Get all active departments.
     */
    @GetMapping("/depts")
    @PreAuthorize("hasAuthority('sys:dept:view')")
    public Result<List<SysDept>> getDeptList() {
        List<SysDept> depts = systemService.getDeptList();
        return Result.success(depts);
    }

    /**
     * Get the full menu tree.
     */
    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('sys:menu:view')")
    public Result<List<MenuTreeVO>> getMenuTree() {
        List<MenuTreeVO> menus = systemService.getMenuTree();
        return Result.success(menus);
    }

    /**
     * Get paginated operation logs.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('sys:log:view')")
    public Result<PageResult<OperationLogVO>> getOperationLogs(
            @Valid OperationLogQueryDTO query,
            HttpServletRequest request) {
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (roleCode == null || (roleCode != 0 && roleCode != 3)) {
            throw ApiException.forbidden("只有管理员或秘书长可以查看操作日志");
        }
        PageResult<OperationLogVO> result = operationLogService.getLogList(query);
        return Result.success(result);
    }
}
