package com.sja.system.service;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.PageResult;
import com.sja.system.dto.OperationLogQueryDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.vo.MenuTreeVO;
import com.sja.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统管理只读用例协调服务。
 */
@Service
@RequiredArgsConstructor
// 202609211059 - 完成了系统管理Controller的服务选择和日志授权参数下沉。
public class SystemApplicationService {

    private final SystemService systemService;
    private final OperationLogService operationLogService;

    public List<SysRole> roles() {
        return systemService.getRoleList();
    }

    public List<SysDept> departments() {
        return systemService.getDeptList();
    }

    public List<MenuTreeVO> menus() {
        return systemService.getMenuTree();
    }

    public PageResult<OperationLogVO> logs(OperationLogQueryDTO query, CurrentActor actor) {
        return operationLogService.getAuthorizedLogList(query, actor.roleCode());
    }
}
