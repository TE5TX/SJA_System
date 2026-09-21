package com.sja.system.service.tool;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.application.model.SujiaExportFile;
import com.sja.system.application.port.SujiaExportPort;
import com.sja.system.service.authorization.RoleAuthorizationPolicy;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.dataobject.SysUserDept;
import com.sja.system.dal.mysql.SysUserDeptMapper;
import com.sja.system.dal.mysql.SysUserMapper;
import com.sja.system.application.vo.tool.SujiaUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves Sujia names within the operator's permitted user scope and prepares exports.
 */
@Service
@RequiredArgsConstructor
public class SujiaService {

    private static final String EXPORT_FILE_NAME = "素拓名单.xlsx";
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final SysUserMapper sysUserMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SujiaExportPort sujiaExportPort;

    public List<SujiaUserVO> resolveNames(List<String> rawNames, Long operatorId, Integer roleCode) {
        Set<String> names = rawNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (names.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> allowedUserIds = canViewAll(roleCode) ? null : getDepartmentUserIds(operatorId);
        if (allowedUserIds != null && allowedUserIds.isEmpty()) {
            return names.stream().map(name -> unmatched(name)).toList();
        }

        QueryWrapper<SysUser> query = new QueryWrapper<SysUser>()
                .in("real_name", names)
                .eq("is_valid", 1)
                .orderByAsc("real_name", "student_no");
        if (allowedUserIds != null) {
            query.in("id", allowedUserIds);
        }
        // 202609101605 - 修复了素拓姓名逐个查询数据库并由Controller承担权限过滤的问题。
        Map<String, List<SysUser>> usersByName = sysUserMapper.selectList(query).stream()
                .collect(Collectors.groupingBy(SysUser::getRealName));

        List<SujiaUserVO> result = new ArrayList<>();
        for (String name : names) {
            List<SysUser> matches = usersByName.getOrDefault(name, Collections.emptyList());
            if (matches.isEmpty()) {
                result.add(unmatched(name));
            } else {
                matches.forEach(user -> result.add(
                        new SujiaUserVO(user.getRealName(), user.getStudentNo(), true)));
            }
        }
        return result;
    }

    public SujiaExportFile export(List<String> names, Long operatorId, Integer roleCode) {
        byte[] content = sujiaExportPort.write(resolveNames(names, operatorId, roleCode));
        return new SujiaExportFile(EXPORT_FILE_NAME, XLSX_CONTENT_TYPE, content);
    }

    private boolean canViewAll(Integer roleCode) {
        // 202609201146 - 素拓全员数据范围改为复用统一角色授权策略。
        return RoleAuthorizationPolicy.canViewAllPersonalInformation(roleCode);
    }

    private List<Long> getDepartmentUserIds(Long userId) {
        List<Long> departmentIds = sysUserDeptMapper.selectList(
                        new QueryWrapper<SysUserDept>().eq("user_id", userId)).stream()
                .map(SysUserDept::getDeptId)
                .distinct()
                .toList();
        if (departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysUserDeptMapper.selectList(
                        new QueryWrapper<SysUserDept>().in("dept_id", departmentIds)).stream()
                .map(SysUserDept::getUserId)
                .distinct()
                .toList();
    }

    private SujiaUserVO unmatched(String name) {
        return new SujiaUserVO(name, "", false);
    }
}
