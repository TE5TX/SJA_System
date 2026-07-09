package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sja.system.common.PageResult;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
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
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserService;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User management service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO getUserProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return buildUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        SysUser update = new SysUser();
        update.setId(userId);

        StringBuilder changedFields = new StringBuilder();

        // 真实姓名
        if (StringUtils.hasText(dto.getRealName()) && !dto.getRealName().equals(user.getRealName())) {
            update.setRealName(dto.getRealName());
            changedFields.append("realName; ");
        }

        // 性别
        if (dto.getGender() != null && !dto.getGender().equals(user.getGender())) {
            update.setGender(dto.getGender());
            changedFields.append("gender; ");
        }

        // 照片URL
        if (dto.getPhoto() != null && !dto.getPhoto().equals(user.getPhoto())) {
            update.setPhoto(dto.getPhoto());
            changedFields.append("photo; ");
        }

        // 身份证号
        if (dto.getIdCard() != null && !dto.getIdCard().equals(user.getIdCard())) {
            // 检查身份证号是否已被其他用户使用
            QueryWrapper<SysUser> idCardCheck = new QueryWrapper<>();
            idCardCheck.eq("id_card", dto.getIdCard()).ne("id", userId).eq("is_valid", 1);
            if (sysUserMapper.selectCount(idCardCheck) > 0) {
                throw new RuntimeException("该身份证号已被其他用户使用");
            }
            update.setIdCard(dto.getIdCard());
            changedFields.append("idCard; ");
        }

        // 学号（修改时自动同步更新 username 账号）
        if (StringUtils.hasText(dto.getStudentNo()) && !dto.getStudentNo().equals(user.getStudentNo())) {
            // 检查学号是否已被其他用户使用
            QueryWrapper<SysUser> studentNoCheck = new QueryWrapper<>();
            studentNoCheck.eq("student_no", dto.getStudentNo()).ne("id", userId).eq("is_valid", 1);
            if (sysUserMapper.selectCount(studentNoCheck) > 0) {
                throw new RuntimeException("该学号已被其他用户使用");
            }
            update.setStudentNo(dto.getStudentNo());
            // 核心逻辑：学号改变时，账号(username)自动同步为新的学号
            update.setUsername(dto.getStudentNo());
            changedFields.append("studentNo+username; ");
        }

        // 学院
        if (dto.getCollege() != null && !dto.getCollege().equals(user.getCollege())) {
            update.setCollege(dto.getCollege());
            changedFields.append("college; ");
        }

        // 班级
        if (dto.getClassName() != null && !dto.getClassName().equals(user.getClassName())) {
            update.setClassName(dto.getClassName());
            changedFields.append("className; ");
        }

        // 宿舍地址
        if (dto.getDormAddress() != null && !dto.getDormAddress().equals(user.getDormAddress())) {
            update.setDormAddress(dto.getDormAddress());
            changedFields.append("dormAddress; ");
        }

        // 手机号
        if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())) {
            update.setPhone(dto.getPhone());
            changedFields.append("phone; ");
        }

        // 银行卡号
        if (dto.getBankCard() != null && !dto.getBankCard().equals(user.getBankCard())) {
            update.setBankCard(dto.getBankCard());
            changedFields.append("bankCard; ");
        }

        // 如果没有需要更新的字段，直接返回
        if (changedFields.length() == 0) {
            return;
        }

        sysUserMapper.updateById(update);

        // 记录操作日志
        String logDetail = String.format("更新个人资料，修改字段: %s", changedFields.toString().trim());
        if (update.getUsername() != null && !update.getUsername().equals(user.getUsername())) {
            logDetail += String.format("（账号已从 [%s] 同步更新为 [%s]）", user.getUsername(), update.getUsername());
        }

        operationLogService.log(userId, user.getRealName(), "", "UPDATE",
                "USER", userId, logDetail, "");

        log.info("User {} updated profile, changed: {}", userId, changedFields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // Verify old password
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // Encode and set new password
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(update);

        operationLogService.log(userId, user.getRealName(), "", "UPDATE",
                "USER", userId, "修改密码", "");

        log.info("User {} changed password", userId);
    }

    @Override
    public PageResult<UserVO> queryUsers(UserQueryDTO query) {
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;

        // Determine which user IDs to filter by
        List<Long> filteredUserIds = null;

        // If deptCode is provided, find user IDs in that department
        if (StringUtils.hasText(query.getDeptCode())) {
            QueryWrapper<SysUserDept> deptQuery = new QueryWrapper<>();
            // First find dept by code
            QueryWrapper<SysDept> deptCodeQuery = new QueryWrapper<>();
            deptCodeQuery.eq("dept_code", query.getDeptCode()).eq("is_valid", 1);
            SysDept dept = sysDeptMapper.selectOne(deptCodeQuery);
            if (dept != null) {
                deptQuery.eq("dept_id", dept.getId());
                List<SysUserDept> userDepts = sysUserDeptMapper.selectList(deptQuery);
                filteredUserIds = userDepts.stream()
                        .map(SysUserDept::getUserId)
                        .collect(Collectors.toList());
                if (filteredUserIds.isEmpty()) {
                    return new PageResult<>(Collections.emptyList(), 0L, (long) pageNum, (long) pageSize);
                }
            } else {
                return new PageResult<>(Collections.emptyList(), 0L, (long) pageNum, (long) pageSize);
            }
        }

        // If roleCode is provided, find user IDs with that role
        if (query.getRoleCode() != null) {
            QueryWrapper<SysRole> roleQuery = new QueryWrapper<>();
            roleQuery.eq("role_code", query.getRoleCode()).eq("is_valid", 1);
            SysRole role = sysRoleMapper.selectOne(roleQuery);
            if (role != null) {
                QueryWrapper<SysUserRole> userRoleQuery = new QueryWrapper<>();
                userRoleQuery.eq("role_id", role.getId());
                List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleQuery);
                List<Long> roleUserIds = userRoles.stream()
                        .map(SysUserRole::getUserId)
                        .collect(Collectors.toList());

                if (filteredUserIds != null) {
                    // Intersect both filters
                    filteredUserIds.retainAll(roleUserIds);
                } else {
                    filteredUserIds = roleUserIds;
                }

                if (filteredUserIds.isEmpty()) {
                    return new PageResult<>(Collections.emptyList(), 0L, (long) pageNum, (long) pageSize);
                }
            } else {
                return new PageResult<>(Collections.emptyList(), 0L, (long) pageNum, (long) pageSize);
            }
        }

        // Build the main user query
        QueryWrapper<SysUser> userQuery = new QueryWrapper<>();
        userQuery.eq("is_valid", 1);

        if (StringUtils.hasText(query.getUsername())) {
            userQuery.like("username", query.getUsername());
        }
        if (StringUtils.hasText(query.getRealName())) {
            userQuery.like("real_name", query.getRealName());
        }
        if (StringUtils.hasText(query.getStudentNo())) {
            userQuery.like("student_no", query.getStudentNo());
        }
        if (StringUtils.hasText(query.getCollege())) {
            userQuery.like("college", query.getCollege());
        }
        if (filteredUserIds != null) {
            userQuery.in("id", filteredUserIds);
        }

        userQuery.orderByDesc("create_time");

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> resultPage = sysUserMapper.selectPage(page, userQuery);

        List<UserVO> voList = resultPage.getRecords().stream()
                .map(this::buildUserVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, resultPage.getTotal(), (long) pageNum, (long) pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, Map<String, Object> updates, Long operatorId,
                           String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // Build before-state snapshot for audit
        StringBuilder detail = new StringBuilder("修改用户信息: ");
        SysUser update = new SysUser();
        update.setId(userId);

        if (updates.containsKey("realName")) {
            String oldVal = user.getRealName();
            String newVal = (String) updates.get("realName");
            update.setRealName(newVal);
            detail.append(String.format("realName [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("gender")) {
            update.setGender((Integer) updates.get("gender"));
            detail.append("gender updated; ");
        }
        if (updates.containsKey("phone")) {
            String oldVal = user.getPhone();
            String newVal = (String) updates.get("phone");
            update.setPhone(newVal);
            detail.append(String.format("phone [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("studentNo")) {
            String oldVal = user.getStudentNo();
            String newVal = (String) updates.get("studentNo");
            update.setStudentNo(newVal);
            detail.append(String.format("studentNo [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("college")) {
            String oldVal = user.getCollege();
            String newVal = (String) updates.get("college");
            update.setCollege(newVal);
            detail.append(String.format("college [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("className")) {
            String oldVal = user.getClassName();
            String newVal = (String) updates.get("className");
            update.setClassName(newVal);
            detail.append(String.format("className [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("dormAddress")) {
            String oldVal = user.getDormAddress();
            String newVal = (String) updates.get("dormAddress");
            update.setDormAddress(newVal);
            detail.append(String.format("dormAddress [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("bankCard")) {
            String oldVal = user.getBankCard();
            String newVal = (String) updates.get("bankCard");
            update.setBankCard(newVal);
            detail.append(String.format("bankCard [%s]->[%s]; ", oldVal, newVal));
        }
        if (updates.containsKey("remark")) {
            update.setRemark((String) updates.get("remark"));
            detail.append("remark updated; ");
        }

        sysUserMapper.updateById(update);

        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, detail.toString(), "");

        log.info("User {} updated by operator {}", userId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status, Long operatorId,
                                 String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setIsValid(status);
        sysUserMapper.updateById(update);

        String statusDesc = status == 1 ? "启用" : "禁用";
        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, statusDesc + "用户: " + user.getRealName(), "");

        log.info("User {} status updated to {} by operator {}", userId, status, operatorId);
    }

    // ======================== Private helpers ========================

    /**
     * Build a UserVO from a SysUser entity, including role names and dept names.
     */
    private UserVO buildUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setGender(user.getGender());
        vo.setPhoto(user.getPhoto());
        vo.setStudentNo(user.getStudentNo());
        vo.setCollege(user.getCollege());
        vo.setClassName(user.getClassName());
        vo.setDormAddress(user.getDormAddress());
        vo.setPhone(user.getPhone());
        vo.setBankCard(user.getBankCard());
        vo.setIsValid(user.getIsValid());
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());

        // Load role names
        QueryWrapper<SysUserRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("user_id", user.getId());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(roleQuery);
        List<String> roleNames = new ArrayList<>();
        if (userRoles != null) {
            for (SysUserRole ur : userRoles) {
                SysRole role = sysRoleMapper.selectById(ur.getRoleId());
                if (role != null) {
                    roleNames.add(role.getRoleName());
                }
            }
        }
        vo.setRoleNames(roleNames);

        // Load department names
        QueryWrapper<SysUserDept> deptQuery = new QueryWrapper<>();
        deptQuery.eq("user_id", user.getId());
        List<SysUserDept> userDepts = sysUserDeptMapper.selectList(deptQuery);
        List<String> deptNames = new ArrayList<>();
        if (userDepts != null) {
            for (SysUserDept ud : userDepts) {
                SysDept dept = sysDeptMapper.selectById(ud.getDeptId());
                if (dept != null) {
                    deptNames.add(dept.getDeptName());
                }
            }
        }
        vo.setDeptNames(deptNames);

        return vo;
    }
}
