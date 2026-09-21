package com.sja.system.controller.system;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.service.system.SystemApplicationService;
import com.sja.system.application.vo.system.MenuTreeVO;
import com.sja.system.application.vo.system.OperationLogVO;
import com.sja.system.application.dto.system.OperationLogQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 系统管理只读接口协议适配器。
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
// 202609211059 - 完成了系统管理Controller的用例选择和日志授权参数下沉。
public class SystemController {

    private final SystemApplicationService systemApplicationService;
    /** 查询全部有效角色。 */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('sys:role:view')")
    public Result<List<SysRole>> getRoleList() {
        return Result.success(systemApplicationService.roles());
    }

    /** 查询全部有效部门。 */
    @GetMapping("/depts")
    @PreAuthorize("hasAuthority('sys:dept:view')")
    public Result<List<SysDept>> getDeptList() {
        return Result.success(systemApplicationService.departments());
    }

    /** 查询完整菜单树。 */
    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('sys:menu:view')")
    public Result<List<MenuTreeVO>> getMenuTree() {
        return Result.success(systemApplicationService.menus());
    }

    /** 分页查询当前角色允许查看的操作日志。 */
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('sys:log:view')")
    public Result<PageResult<OperationLogVO>> getOperationLogs(
            @Valid OperationLogQueryDTO query,
            CurrentActor actor) {
        return Result.success(systemApplicationService.logs(query, actor));
    }
}
