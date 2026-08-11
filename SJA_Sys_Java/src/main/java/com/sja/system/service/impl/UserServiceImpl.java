package com.sja.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sja.system.common.PageResult;
import com.sja.system.common.ApiException;
import com.sja.system.common.MaskingUtils;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserBatchUpdateDTO;
import com.sja.system.dto.UserCreateDTO;
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
import com.sja.system.vo.FeeContributorVO;
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
import java.util.Objects;
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
            throw ApiException.notFound("用户不存在");
        }
        return buildUserVO(user);
    }

    @Override
    public UserVO getManagedUserDetail(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.badRequest("用户不存在");
        }
        return buildUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
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
                throw ApiException.conflict("该身份证号已被其他用户使用");
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
                throw ApiException.conflict("该学号已被其他用户使用");
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
            throw ApiException.notFound("用户不存在");
        }

        // Verify old password
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw ApiException.badRequest("原密码错误");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw ApiException.badRequest("新密码不能与原密码相同");
        }

        // Encode and set new password
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        update.setMustChangePassword(0);
        sysUserMapper.updateById(update);

        operationLogService.log(userId, user.getRealName(), "", "UPDATE",
                "USER", userId, "修改密码", "");

        log.info("User {} changed password", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfilePhoto(Long userId, String photo, Long operatorId, String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        String newPhoto = StringUtils.hasText(photo) ? photo : null;
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId).set("photo", newPhoto);
        sysUserMapper.update(null, updateWrapper);

        String action = StringUtils.hasText(photo) ? "上传照片" : "删除照片";
        String detail = String.format("%s: %s，原照片 [%s]，新照片 [%s]",
                action, user.getRealName(), user.getPhoto(), newPhoto);
        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, detail, "");
    }

    @Override
    public PageResult<UserVO> queryUsers(UserQueryDTO query) {
        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        // 202608111533 - 修复了人员管理列表缺少200条分页选项且服务端未限制分页大小的问题。
        int pageSize = query.getPageSize() != null
                ? Math.min(200, Math.max(1, query.getPageSize())) : 10;

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
        if (query.getIsValid() != null) {
            userQuery.eq("is_valid", query.getIsValid());
        }
        if (filteredUserIds != null) {
            userQuery.in("id", filteredUserIds);
        }

        userQuery.orderByDesc("create_time");

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> resultPage = sysUserMapper.selectPage(page, userQuery);

        List<UserVO> voList = resultPage.getRecords().stream()
                .map(this::buildUserVO)
                .peek(MaskingUtils::maskUser)
                .collect(Collectors.toList());

        return new PageResult<>(voList, resultPage.getTotal(), (long) pageNum, (long) pageSize);
    }

    @Override
    public List<FeeContributorVO> queryActiveFeeContributors() {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.select("id", "real_name", "student_no", "is_valid")
                .eq("is_valid", 1)
                .orderByAsc("real_name", "id");
        return sysUserMapper.selectList(query).stream()
                .map(user -> new FeeContributorVO(
                        user.getId(), user.getRealName(), user.getStudentNo(), user.getIsValid()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserVO> queryUsersForExport(UserQueryDTO query) {
        final int exportPageSize = 500;
        List<UserVO> users = new ArrayList<>();
        int pageNum = 1;
        long total;

        do {
            UserQueryDTO pageQuery = new UserQueryDTO();
            pageQuery.setUsername(query.getUsername());
            pageQuery.setRealName(query.getRealName());
            pageQuery.setStudentNo(query.getStudentNo());
            pageQuery.setCollege(query.getCollege());
            pageQuery.setDeptCode(query.getDeptCode());
            pageQuery.setRoleCode(query.getRoleCode());
            pageQuery.setPageNum(pageNum);
            pageQuery.setPageSize(exportPageSize);

            PageResult<UserVO> result = queryUsers(pageQuery);
            users.addAll(result.getList());
            total = result.getTotal();
            pageNum++;
        } while (users.size() < total);

        return users;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO dto, Long operatorId,
                           String operatorName, String operatorRole) {
        String studentNo = dto.getStudentNo().trim();
        String idCard = dto.getIdCard().trim();

        QueryWrapper<SysUser> studentNoCheck = new QueryWrapper<>();
        studentNoCheck.eq("student_no", studentNo).or().eq("username", studentNo);
        if (sysUserMapper.selectCount(studentNoCheck) > 0) {
            throw ApiException.conflict("该学号或登录账号已被其他用户使用");
        }

        QueryWrapper<SysUser> idCardCheck = new QueryWrapper<>();
        idCardCheck.eq("id_card", idCard);
        if (sysUserMapper.selectCount(idCardCheck) > 0) {
            throw ApiException.conflict("该身份证号已被其他用户使用");
        }

        List<Long> deptIds = distinctIds(dto.getDeptIds(), "部门");
        List<Long> roleIds = distinctIds(dto.getRoleIds(), "角色");
        validateDeptIds(deptIds);
        validateRoleIds(roleIds);

        SysUser user = new SysUser();
        user.setUsername(studentNo);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setMustChangePassword(1);
        user.setRealName(dto.getRealName().trim());
        user.setGender(dto.getGender());
        user.setPhoto(StringUtils.hasText(dto.getPhoto()) ? dto.getPhoto().trim() : null);
        user.setIdCard(idCard);
        user.setStudentNo(studentNo);
        user.setCollege(dto.getCollege().trim());
        user.setClassName(dto.getClassName().trim());
        user.setDormAddress(dto.getDormAddress().trim());
        user.setPhone(dto.getPhone().trim());
        user.setBankCard(dto.getBankCard().trim());
        user.setIsValid(dto.getIsValid());
        user.setRemark(dto.getRemark().trim());
        sysUserMapper.insert(user);

        updateUserDepts(user.getId(), deptIds);
        updateUserRoles(user.getId(), roleIds);
        operationLogService.log(operatorId, operatorName, operatorRole, "CREATE",
                "USER", user.getId(),
                "新增人员: " + user.getRealName() + "（" + studentNo + "）", "");
        log.info("User {} created by operator {}", user.getId(), operatorId);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, Map<String, Object> updates, Long operatorId,
                           String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        // Build before-state snapshot for audit
        StringBuilder detail = new StringBuilder("修改用户信息: ");
        SysUser update = new SysUser();
        update.setId(userId);
        boolean hasUserFieldUpdate = false;

        if (updates.containsKey("realName")) {
            String oldVal = user.getRealName();
            String newVal = (String) updates.get("realName");
            update.setRealName(newVal);
            detail.append(String.format("realName [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("photo")) {
            String oldVal = user.getPhoto();
            String newVal = (String) updates.get("photo");
            update.setPhoto(newVal);
            detail.append(String.format("photo [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("idCard")) {
            String oldVal = user.getIdCard();
            String newVal = (String) updates.get("idCard");
            if (StringUtils.hasText(newVal) && !newVal.equals(oldVal)) {
                QueryWrapper<SysUser> idCardCheck = new QueryWrapper<>();
                idCardCheck.eq("id_card", newVal).ne("id", userId).eq("is_valid", 1);
                if (sysUserMapper.selectCount(idCardCheck) > 0) {
                    throw ApiException.conflict("该身份证号已被其他用户使用");
                }
            }
            update.setIdCard(newVal);
            detail.append(String.format("idCard [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("gender")) {
            update.setGender(toInteger(updates.get("gender")));
            detail.append("gender updated; ");
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("phone")) {
            String oldVal = user.getPhone();
            String newVal = (String) updates.get("phone");
            update.setPhone(newVal);
            detail.append(String.format("phone [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("studentNo")) {
            String oldVal = user.getStudentNo();
            String newVal = (String) updates.get("studentNo");
            if (StringUtils.hasText(newVal) && !newVal.equals(oldVal)) {
                QueryWrapper<SysUser> studentNoCheck = new QueryWrapper<>();
                studentNoCheck.eq("student_no", newVal).ne("id", userId).eq("is_valid", 1);
                if (sysUserMapper.selectCount(studentNoCheck) > 0) {
                    throw ApiException.conflict("该学号已被其他用户使用");
                }
                update.setStudentNo(newVal);
                update.setUsername(newVal);
                detail.append(String.format("studentNo [%s]->[%s]; username synced; ", oldVal, newVal));
                hasUserFieldUpdate = true;
            }
        }
        if (updates.containsKey("college")) {
            String oldVal = user.getCollege();
            String newVal = (String) updates.get("college");
            update.setCollege(newVal);
            detail.append(String.format("college [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("className")) {
            String oldVal = user.getClassName();
            String newVal = (String) updates.get("className");
            update.setClassName(newVal);
            detail.append(String.format("className [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("dormAddress")) {
            String oldVal = user.getDormAddress();
            String newVal = (String) updates.get("dormAddress");
            update.setDormAddress(newVal);
            detail.append(String.format("dormAddress [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("bankCard")) {
            String oldVal = user.getBankCard();
            String newVal = (String) updates.get("bankCard");
            update.setBankCard(newVal);
            detail.append(String.format("bankCard [%s]->[%s]; ", oldVal, newVal));
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("remark")) {
            update.setRemark((String) updates.get("remark"));
            detail.append("remark updated; ");
            hasUserFieldUpdate = true;
        }
        if (updates.containsKey("isValid")) {
            update.setIsValid(toInteger(updates.get("isValid")));
            detail.append("isValid updated; ");
            hasUserFieldUpdate = true;
        }

        if (hasUserFieldUpdate) {
            sysUserMapper.updateById(update);
        }

        if (updates.containsKey("deptIds")) {
            List<Long> deptIds = toLongList(updates.get("deptIds"));
            updateUserDepts(userId, deptIds);
            detail.append("deptIds updated; ");
        }

        if (updates.containsKey("roleIds")) {
            List<Long> roleIds = toLongList(updates.get("roleIds"));
            updateUserRoles(userId, roleIds);
            detail.append("roleIds updated; ");
        }

        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, detail.toString(), "");

        log.info("User {} updated by operator {}", userId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateUsers(UserBatchUpdateDTO dto, Long operatorId,
                                 String operatorName, String operatorRole) {
        if (dto.getRoleIds() == null && dto.getIsValid() == null) {
            throw ApiException.badRequest("请至少选择一项需要修改的内容");
        }
        if (dto.getRoleIds() != null && dto.getRoleIds().isEmpty()) {
            throw ApiException.badRequest("角色不能为空");
        }

        List<Long> userIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            throw ApiException.badRequest("请至少选择一名人员");
        }

        List<Long> roleIds = dto.getRoleIds() == null ? null : dto.getRoleIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> roleNames = new ArrayList<>();
        if (roleIds != null) {
            if (roleIds.isEmpty()) {
                throw ApiException.badRequest("角色不能为空");
            }
            for (Long roleId : roleIds) {
                SysRole role = sysRoleMapper.selectById(roleId);
                if (role == null || !Objects.equals(role.getIsValid(), 1)) {
                    throw ApiException.badRequest("角色不存在或已禁用: " + roleId);
                }
                roleNames.add(role.getRoleName());
            }
        }

        for (Long userId : userIds) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                throw ApiException.notFound("用户不存在: " + userId);
            }
            if (roleIds != null) {
                updateUserRoles(userId, roleIds);
            }
            if (dto.getIsValid() != null) {
                SysUser statusUpdate = new SysUser();
                statusUpdate.setId(userId);
                statusUpdate.setIsValid(dto.getIsValid());
                sysUserMapper.updateById(statusUpdate);
            }

            StringBuilder detail = new StringBuilder("批量修改人员信息: ");
            if (roleIds != null) {
                detail.append("角色 -> ").append(roleNames).append("; ");
            }
            if (dto.getIsValid() != null) {
                detail.append("账号状态 -> ")
                        .append(dto.getIsValid() == 1 ? "启用" : "禁用")
                        .append("; ");
            }
            operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                    "USER", userId, detail.toString(), "");
        }

        log.info("Batch updated {} users by operator {}", userIds.size(), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status, Long operatorId,
                                 String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
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
        vo.setIdCard(user.getIdCard());
        vo.setStudentNo(user.getStudentNo());
        vo.setCollege(user.getCollege());
        vo.setClassName(user.getClassName());
        vo.setDormAddress(user.getDormAddress());
        vo.setPhone(user.getPhone());
        vo.setBankCard(user.getBankCard());
        vo.setIsValid(user.getIsValid());
        vo.setMustChangePassword(user.getMustChangePassword());
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());

        // Load role names
        QueryWrapper<SysUserRole> roleQuery = new QueryWrapper<>();
        roleQuery.eq("user_id", user.getId());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(roleQuery);
        List<String> roleNames = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();
        List<Integer> roleCodes = new ArrayList<>();
        if (userRoles != null) {
            for (SysUserRole ur : userRoles) {
                SysRole role = sysRoleMapper.selectById(ur.getRoleId());
                if (role != null) {
                    roleIds.add(role.getId());
                    roleCodes.add(role.getRoleCode());
                    roleNames.add(role.getRoleName());
                }
            }
        }
        vo.setRoleNames(roleNames);
        vo.setRoleIds(roleIds);
        vo.setRoleCodes(roleCodes);

        // Load department names
        QueryWrapper<SysUserDept> deptQuery = new QueryWrapper<>();
        deptQuery.eq("user_id", user.getId());
        List<SysUserDept> userDepts = sysUserDeptMapper.selectList(deptQuery);
        List<String> deptNames = new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();
        List<String> deptCodes = new ArrayList<>();
        if (userDepts != null) {
            for (SysUserDept ud : userDepts) {
                SysDept dept = sysDeptMapper.selectById(ud.getDeptId());
                if (dept != null) {
                    deptIds.add(dept.getId());
                    deptCodes.add(dept.getDeptCode());
                    deptNames.add(dept.getDeptName());
                }
            }
        }
        vo.setDeptNames(deptNames);
        vo.setDeptIds(deptIds);
        vo.setDeptCodes(deptCodes);

        return vo;
    }

    private void updateUserDepts(Long userId, List<Long> deptIds) {
        QueryWrapper<SysUserDept> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("user_id", userId);
        sysUserDeptMapper.delete(deleteQuery);

        for (Long deptId : deptIds) {
            if (deptId == null) {
                continue;
            }
            SysDept dept = sysDeptMapper.selectById(deptId);
            if (dept == null || !Objects.equals(dept.getIsValid(), 1)) {
                throw ApiException.badRequest("部门不存在或已禁用: " + deptId);
            }
            SysUserDept userDept = new SysUserDept();
            userDept.setUserId(userId);
            userDept.setDeptId(deptId);
            sysUserDeptMapper.insert(userDept);
        }
    }

    private List<Long> distinctIds(List<Long> ids, String fieldName) {
        List<Long> result = ids == null ? Collections.emptyList() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw ApiException.badRequest("请至少选择一个" + fieldName);
        }
        return result;
    }

    private void validateDeptIds(List<Long> deptIds) {
        for (Long deptId : deptIds) {
            SysDept dept = sysDeptMapper.selectById(deptId);
            if (dept == null || !Objects.equals(dept.getIsValid(), 1)) {
                throw ApiException.badRequest("部门不存在或已禁用: " + deptId);
            }
        }
    }

    private void validateRoleIds(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null || !Objects.equals(role.getIsValid(), 1)) {
                throw ApiException.badRequest("角色不存在或已禁用: " + roleId);
            }
        }
    }

    private void updateUserRoles(Long userId, List<Long> roleIds) {
        QueryWrapper<SysUserRole> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("user_id", userId);
        sysUserRoleMapper.delete(deleteQuery);

        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null || !Objects.equals(role.getIsValid(), 1)) {
                throw ApiException.badRequest("角色不存在或已禁用: " + roleId);
            }
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List)) {
            return Collections.singletonList(toLong(value));
        }
        List<Object> values = (List<Object>) value;
        return values.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? Long.valueOf(text) : null;
    }
}
