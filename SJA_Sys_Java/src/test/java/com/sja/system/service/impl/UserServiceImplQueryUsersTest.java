package com.sja.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sja.system.common.PageResult;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRole;
import com.sja.system.entity.SysUser;
import com.sja.system.entity.SysUserDept;
import com.sja.system.entity.SysUserRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.mapper.SysUserDeptMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.mapper.SysUserRoleMapper;
import com.sja.system.service.AvatarStorageService;
import com.sja.system.service.OperationLogService;
import com.sja.system.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人员列表关联数据批量加载测试。
 */
class UserServiceImplQueryUsersTest {

    @Test
    void queryUsersLoadsRolesAndDepartmentsInBatchesInsteadOfPerUser() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, roleMapper, deptMapper,
                userRoleMapper, userDeptMapper, mock(OperationLogService.class),
                mock(PasswordEncoder.class), mock(AvatarStorageService.class));

        Page<SysUser> userPage = new Page<>(1, 2);
        userPage.setTotal(2);
        userPage.setRecords(List.of(user(1L, "甲"), user(2L, "乙")));
        when(userMapper.selectPage(any(Page.class), any())).thenReturn(userPage);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(
                userRole(1L, 11L), userRole(2L, 12L)));
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                role(11L, "部长", 1), role(12L, "部员", 2)));
        when(userDeptMapper.selectList(any())).thenReturn(List.of(
                userDept(1L, 21L), userDept(2L, 22L)));
        when(deptMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                department(21L, "采编部", "EDITORIAL"), department(22L, "摄影部", "PHOTOGRAPHY")));

        UserQueryDTO query = new UserQueryDTO();
        query.setPageNum(1);
        query.setPageSize(2);
        PageResult<UserVO> result = service.queryUsers(query);

        // 202609201030 - 验证人员列表角色、部门关联统一批量查询，避免 N+1 查询回归。
        assertEquals(2, result.getList().size());
        assertEquals(List.of("部长"), result.getList().get(0).getRoleNames());
        assertEquals(List.of("摄影部"), result.getList().get(1).getDeptNames());
        verify(userRoleMapper, times(1)).selectList(any());
        verify(roleMapper, times(1)).selectBatchIds(anyCollection());
        verify(userDeptMapper, times(1)).selectList(any());
        verify(deptMapper, times(1)).selectBatchIds(anyCollection());
    }

    @Test
    void queryUsersForExportLoadsUsersOnceAndPartitionsRelationQueries() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        UserServiceImpl service = new UserServiceImpl(userMapper, roleMapper, deptMapper,
                userRoleMapper, userDeptMapper, mock(OperationLogService.class),
                mock(PasswordEncoder.class), mock(AvatarStorageService.class));

        List<SysUser> users = LongStream.rangeClosed(1, 1001)
                .mapToObj(id -> user(id, "用户" + id))
                .collect(Collectors.toList());
        when(userMapper.selectList(any())).thenReturn(users);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(userDeptMapper.selectList(any())).thenReturn(List.of());

        List<UserVO> result = service.queryUsersForExport(new UserQueryDTO());

        // 202609201117 - 验证人员导出仅查询一次用户表，并对关联查询进行安全分片。
        assertEquals(1001, result.size());
        verify(userMapper, times(1)).selectList(any());
        verify(userMapper, never()).selectPage(any(Page.class), any());
        verify(userRoleMapper, times(2)).selectList(any());
        verify(userDeptMapper, times(2)).selectList(any());
    }

    private SysUser user(Long id, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRealName(realName);
        user.setIsValid(1);
        return user;
    }

    private SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }

    private SysUserDept userDept(Long userId, Long deptId) {
        SysUserDept relation = new SysUserDept();
        relation.setUserId(userId);
        relation.setDeptId(deptId);
        return relation;
    }

    private SysRole role(Long id, String roleName, Integer roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        return role;
    }

    private SysDept department(Long id, String deptName, String deptCode) {
        SysDept department = new SysDept();
        department.setId(id);
        department.setDeptName(deptName);
        department.setDeptCode(deptCode);
        return department;
    }
}
