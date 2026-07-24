package com.sja.system.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.Result;
import com.sja.system.dto.SujiaResolveDTO;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserDept;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.security.SecurityUtils;
import com.sja.system.vo.SujiaUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tools/sujia")
@RequiredArgsConstructor
public class SujiaController {

    private final SysUserMapper sysUserMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SecurityUtils securityUtils;

    @PostMapping("/resolve")
    @PreAuthorize("hasAuthority('tools:sujia:use')")
    public Result<List<SujiaUserVO>> resolve(@RequestBody @Valid SujiaResolveDTO dto,
                                             HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        return Result.success(resolveNames(dto.getNames(), operatorId, roleCode));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('tools:sujia:use')")
    public void export(@RequestBody @Valid SujiaResolveDTO dto,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            throw new RuntimeException("未授权");
        }

        try {
            List<SujiaUserVO> rows = resolveNames(dto.getNames(), operatorId, roleCode);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("素拓名单.xlsx", StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            List<List<String>> head = Arrays.asList(
                    Collections.singletonList("姓名"),
                    Collections.singletonList("学号")
            );
            List<List<String>> data = rows.stream()
                    .map(row -> Arrays.asList(row.getRealName(), row.getStudentNo() == null ? "" : row.getStudentNo()))
                    .collect(Collectors.toList());
            EasyExcel.write(response.getOutputStream()).head(head).sheet("素拓名单").doWrite(data);
        } catch (Exception e) {
            throw new RuntimeException("导出素拓名单失败: " + e.getMessage(), e);
        }
    }

    private List<SujiaUserVO> resolveNames(List<String> rawNames, Long operatorId, Integer roleCode) {
        Set<String> names = rawNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (names.isEmpty()) {
            return Collections.emptyList();
        }

        boolean canViewAll = isGlobalInfoManager(roleCode);
        List<Long> allowedUserIds = canViewAll ? null : getDeptUserIds(operatorId);
        List<SujiaUserVO> result = new ArrayList<>();

        for (String name : names) {
            QueryWrapper<SysUser> query = new QueryWrapper<>();
            query.eq("real_name", name).eq("is_valid", 1).orderByAsc("student_no");
            if (allowedUserIds != null) {
                if (allowedUserIds.isEmpty()) {
                    result.add(new SujiaUserVO(name, "", false));
                    continue;
                }
                query.in("id", allowedUserIds);
            }
            List<SysUser> users = sysUserMapper.selectList(query);
            if (users == null || users.isEmpty()) {
                result.add(new SujiaUserVO(name, "", false));
                continue;
            }
            for (SysUser user : users) {
                result.add(new SujiaUserVO(user.getRealName(), user.getStudentNo(), true));
            }
        }
        return result;
    }

    private boolean isGlobalInfoManager(Integer roleCode) {
        return roleCode != null && (roleCode == 0 || roleCode == 1 || roleCode == 2 || roleCode == 3);
    }

    private List<Long> getDeptUserIds(Long userId) {
        QueryWrapper<SysUserDept> myDeptQuery = new QueryWrapper<>();
        myDeptQuery.eq("user_id", userId);
        List<Long> deptIds = sysUserDeptMapper.selectList(myDeptQuery).stream()
                .map(SysUserDept::getDeptId)
                .distinct()
                .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<SysUserDept> deptUserQuery = new QueryWrapper<>();
        deptUserQuery.in("dept_id", deptIds);
        return sysUserDeptMapper.selectList(deptUserQuery).stream()
                .map(SysUserDept::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }
}
