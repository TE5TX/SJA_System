package com.sja.system.service.fee;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.authorization.RoleAuthorizationPolicy;
import com.sja.system.common.ApiException;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysUserDept;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 稿费模块数据范围和敏感操作权限服务。
 *
 * <p>角色规则复用统一授权策略，部门数据范围在此处集中查询，避免查询、审核和写入服务
 * 分别维护权限逻辑。</p>
 */
@Service
@RequiredArgsConstructor
// 202609201644 - 完成了稿费权限与部门数据范围的独立封装。
public class FeeAccessService {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserDeptMapper sysUserDeptMapper;

    public void ensureCanSubmit(Long userId, Integer roleCode, String workTypeCode) {
        String departmentCode = FeeWorkType.require(workTypeCode).departmentCode();
        Set<String> allowed = RoleAuthorizationPolicy.canSubmitAcrossDepartments(roleCode)
                ? FeeWorkType.allDepartmentCodes() : userDepartmentCodes(userId);
        if (!allowed.contains(departmentCode)) {
            throw ApiException.forbidden("不能向其他部门提交稿费任务");
        }
    }

    public void ensureCanView(Long userId, Integer roleCode, String workTypeCode) {
        if (!allowedDepartmentCodes(userId, roleCode)
                .contains(FeeWorkType.require(workTypeCode).departmentCode())) {
            throw ApiException.forbidden("不能访问其他部门的稿费任务");
        }
    }

    public Set<String> allowedDepartmentCodes(Long userId, Integer roleCode) {
        return RoleAuthorizationPolicy.canViewAllFeeTasks(roleCode)
                ? FeeWorkType.allDepartmentCodes() : userDepartmentCodes(userId);
    }

    public boolean canManageAmount(Integer roleCode) {
        return RoleAuthorizationPolicy.canManageFeeAmounts(roleCode);
    }

    public void ensureCanManageAmount(Integer roleCode, String action) {
        if (!canManageAmount(roleCode)) {
            throw ApiException.forbidden("只有管理员或秘书长可以" + action);
        }
    }

    public void ensureCanEdit(Integer roleCode) {
        if (!RoleAuthorizationPolicy.canEditFeeTasks(roleCode)) {
            throw ApiException.forbidden("没有权限修改稿费任务");
        }
    }

    private Set<String> userDepartmentCodes(Long userId) {
        QueryWrapper<SysUserDept> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        List<SysUserDept> relations = sysUserDeptMapper.selectList(query);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> departmentIds = relations.stream()
                .map(SysUserDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 202609210900 - 部门数据范围改为批量查询，避免一个用户关联多个部门时产生N+1查询。
        return sysDeptMapper.selectBatchIds(departmentIds).stream()
                .filter(Objects::nonNull)
                .map(SysDept::getDeptCode)
                .collect(Collectors.toSet());
    }
}
