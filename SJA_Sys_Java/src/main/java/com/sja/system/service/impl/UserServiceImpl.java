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
import com.sja.system.dto.UserUpdateDTO;
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
import com.sja.system.service.AvatarStorageService;
import com.sja.system.service.UserService;
import com.sja.system.vo.UserVO;
import com.sja.system.vo.FeeContributorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User management service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_INITIAL_PASSWORD = "asdf123456";

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

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
        String phone = normalizeAndValidatePhone(dto.getPhone());

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

        // 身份证号
        if (dto.getIdCard() != null && !dto.getIdCard().equals(user.getIdCard())) {
            // 检查身份证号是否已被其他用户使用
            QueryWrapper<SysUser> idCardCheck = new QueryWrapper<>();
            idCardCheck.eq("id_card", dto.getIdCard()).ne("id", userId);
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
            studentNoCheck.and(query -> query.eq("student_no", dto.getStudentNo())
                            .or().eq("username", dto.getStudentNo()))
                    .ne("id", userId);
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
        if (!phone.equals(user.getPhone())) {
            ensurePhoneAvailable(phone, userId);
            update.setPhone(phone);
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
        if (update.getUsername() != null) {
            incrementTokenVersion(userId);
        }

        // 记录操作日志
        String logDetail = String.format("更新个人资料，修改字段: %s", changedFields.toString().trim());
        if (update.getUsername() != null && !update.getUsername().equals(user.getUsername())) {
            logDetail += String.format("（账号已从 [%s] 同步更新为 [%s]）", user.getUsername(), update.getUsername());
        }

        operationLogService.logChange(userId, user.getRealName(), "", "UPDATE",
                "USER", userId, logDetail,
                profileAuditSnapshot(user.getRealName(), user.getStudentNo(), user.getGender(),
                        user.getCollege(), user.getClassName()),
                profileAuditSnapshot(
                        update.getRealName() != null ? update.getRealName() : user.getRealName(),
                        update.getStudentNo() != null ? update.getStudentNo() : user.getStudentNo(),
                        update.getGender() != null ? update.getGender() : user.getGender(),
                        update.getCollege() != null ? update.getCollege() : user.getCollege(),
                        update.getClassName() != null ? update.getClassName() : user.getClassName()), "");

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
        sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .set("password", passwordEncoder.encode(dto.getNewPassword()))
                .set("must_change_password", 0)
                .setSql("token_version = token_version + 1"));

        operationLogService.log(userId, user.getRealName(), "", "UPDATE",
                "USER", userId, "修改密码", "");

        log.info("User {} changed password", userId);
    }

    @Override
    public void verifyCurrentPassword(Long userId, String currentPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getIsValid(), 1)) {
            throw ApiException.unauthorized("当前用户不存在或已禁用");
        }
        if (!StringUtils.hasText(currentPassword)
                || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw ApiException.forbidden("当前用户密码错误，操作未执行");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfilePhoto(Long userId, String photo, Long operatorId, String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        String newPhoto = StringUtils.hasText(photo) ? photo.trim() : null;
        if (Objects.equals(user.getPhoto(), newPhoto)) {
            return;
        }
        if (newPhoto != null) {
            avatarStorageService.validateAssignableAvatar(newPhoto, userId);
            avatarStorageService.deleteAfterRollback(newPhoto);
        }
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId).set("photo", newPhoto);
        sysUserMapper.update(null, updateWrapper);

        String action = StringUtils.hasText(photo) ? "上传照片" : "删除照片";
        String detail = String.format("%s: %s，原照片 [%s]，新照片 [%s]",
                action, user.getRealName(), user.getPhoto(), newPhoto);
        operationLogService.logChange(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, detail,
                String.format("{\"photo\":\"%s\"}", nullToEmpty(user.getPhoto())),
                String.format("{\"photo\":\"%s\"}", nullToEmpty(newPhoto)), "");
        avatarStorageService.deleteAfterCommit(user.getPhoto());
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
        List<SysUser> users = sysUserMapper.selectList(query);
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new QueryWrapper<SysUserRole>().in("user_id", userIds));
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, Integer> roleCodeById = roleIds.isEmpty()
                ? Collections.emptyMap()
                : sysRoleMapper.selectList(new QueryWrapper<SysRole>()
                        .select("id", "role_code")
                        .in("id", roleIds)
                        .eq("is_valid", 1))
                        .stream()
                        .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));
        Map<Long, List<Integer>> roleCodesByUserId = new HashMap<>();
        userRoles.forEach(userRole -> {
            Integer roleCode = roleCodeById.get(userRole.getRoleId());
            if (roleCode != null) {
                roleCodesByUserId.computeIfAbsent(userRole.getUserId(), ignored -> new ArrayList<>())
                        .add(roleCode);
            }
        });

        // 202608131602 - 修复了月度补贴必须手动输入人员名单而未使用系统角色数据的问题。
        return users.stream()
                .map(user -> new FeeContributorVO(
                        user.getId(), user.getRealName(), user.getStudentNo(), user.getIsValid(),
                        roleCodesByUserId.getOrDefault(user.getId(), Collections.emptyList()).stream()
                                .distinct().sorted().collect(Collectors.toList())))
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
            pageQuery.setIsValid(query.getIsValid());
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
        // 202608131454 - 新增人员时仅要求学号，并为其余系统必需字段设置安全默认值。
        String idCard = trimToNull(dto.getIdCard());
        if (idCard != null) idCard = idCard.toUpperCase();
        String phone = trimToNull(dto.getPhone());
        if (phone != null) phone = normalizeAndValidatePhone(phone);
        String password = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_INITIAL_PASSWORD;
        if (password.length() < 6 || password.length() > 50) {
            throw ApiException.badRequest("初始密码长度应为 6 到 50 个字符");
        }

        QueryWrapper<SysUser> studentNoCheck = new QueryWrapper<>();
        studentNoCheck.eq("student_no", studentNo).or().eq("username", studentNo);
        if (sysUserMapper.selectCount(studentNoCheck) > 0) {
            throw ApiException.conflict("该学号或登录账号已被其他用户使用");
        }

        if (idCard != null) ensureIdCardAvailable(idCard, null);
        if (phone != null) ensurePhoneAvailable(phone, null);

        List<Long> deptIds = optionalDistinctIds(dto.getDeptIds());
        List<Long> roleIds = optionalDistinctIds(dto.getRoleIds());
        validateDeptIds(deptIds);
        validateRoleIds(roleIds);

        SysUser user = new SysUser();
        user.setUsername(studentNo);
        user.setPassword(passwordEncoder.encode(password));
        // 202609011534 - 公开注册由用户自行设定密码，无需再强制首次登录修改。
        user.setMustChangePassword(Boolean.FALSE.equals(dto.getRequirePasswordChange()) ? 0 : 1);
        user.setRealName(StringUtils.hasText(dto.getRealName()) ? dto.getRealName().trim() : studentNo);
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        String photo = StringUtils.hasText(dto.getPhoto()) ? dto.getPhoto().trim() : null;
        if (photo != null) {
            avatarStorageService.validateAssignableAvatar(photo, null);
            avatarStorageService.deleteAfterRollback(photo);
        }
        user.setPhoto(photo);
        user.setIdCard(idCard);
        user.setStudentNo(studentNo);
        user.setCollege(trimToNull(dto.getCollege()));
        user.setClassName(trimToNull(dto.getClassName()));
        user.setDormAddress(trimToNull(dto.getDormAddress()));
        user.setPhone(phone);
        user.setBankCard(trimToNull(dto.getBankCard()));
        user.setIsValid(dto.getIsValid() == null ? 1 : dto.getIsValid());
        user.setRemark(trimToNull(dto.getRemark()));
        sysUserMapper.insert(user);

        updateUserDepts(user.getId(), deptIds);
        updateUserRoles(user.getId(), roleIds);
        // 202609011558 - 公开自助注册没有登录操作人时，使用新用户ID关联成功审计日志。
        Long auditUserId = operatorId == null ? user.getId() : operatorId;
        String auditOperatorName = StringUtils.hasText(operatorName) ? operatorName : user.getRealName();
        operationLogService.log(auditUserId, auditOperatorName, operatorRole, "CREATE",
                "USER", user.getId(),
                "新增人员: " + user.getRealName() + "（" + studentNo + "）", "");
        log.info("User {} created by operator {}", user.getId(), operatorId);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId, Long operatorId,
                                  String operatorName, String operatorRole) {
        if (Objects.equals(userId, operatorId)) {
            throw ApiException.badRequest("请通过个人管理修改自己的密码");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .set("password", passwordEncoder.encode(DEFAULT_INITIAL_PASSWORD))
                .set("must_change_password", 1)
                .setSql("token_version = token_version + 1"));
        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, "重置人员密码并要求下次登录修改", "");
        log.info("User {} password reset by operator {}", userId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserUpdateDTO dto, Long operatorId,
                           String operatorName, String operatorRole) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        validateCompleteUserUpdate(dto);
        String studentNo = dto.getStudentNo().trim();
        // 202608131443 - 允许有人员编辑权限的管理人员将非核心人员资料留空。
        String idCard = trimToNull(dto.getIdCard());
        if (idCard != null) idCard = idCard.toUpperCase();
        String phone = trimToNull(dto.getPhone());
        String bankCard = trimToNull(dto.getBankCard());
        String college = trimToNull(dto.getCollege());
        String className = trimToNull(dto.getClassName());
        String dormAddress = trimToNull(dto.getDormAddress());
        String remark = trimToNull(dto.getRemark());
        List<Long> deptIds = distinctIds(dto.getDeptIds(), "部门");
        List<Long> roleIds = distinctIds(dto.getRoleIds(), "角色");
        validateDeptIds(deptIds);
        validateRoleIds(roleIds);

        ensureStudentNoAvailable(studentNo, userId);
        if (idCard != null) ensureIdCardAvailable(idCard, userId);
        if (phone != null) ensurePhoneAvailable(phone, userId);

        List<Long> oldDeptIds = currentDeptIds(userId);
        List<Long> oldRoleIds = currentRoleIds(userId);
        boolean invalidateTokens = !studentNo.equals(user.getStudentNo())
                || !Objects.equals(dto.getIsValid(), user.getIsValid())
                || !sameIds(oldDeptIds, deptIds)
                || !sameIds(oldRoleIds, roleIds);

        String newPhoto = StringUtils.hasText(dto.getPhoto()) ? dto.getPhoto().trim() : null;
        if (newPhoto != null && !Objects.equals(newPhoto, user.getPhoto())) {
            avatarStorageService.validateAssignableAvatar(newPhoto, userId);
            avatarStorageService.deleteAfterRollback(newPhoto);
        }
        UpdateWrapper<SysUser> update = new UpdateWrapper<>();
        update.eq("id", userId)
                .set("real_name", dto.getRealName().trim())
                .set("gender", dto.getGender())
                .set("username", studentNo)
                .set("student_no", studentNo)
                .set("photo", newPhoto)
                .set("id_card", idCard)
                .set("phone", phone)
                .set("college", college)
                .set("class_name", className)
                .set("dorm_address", dormAddress)
                .set("bank_card", bankCard)
                .set("is_valid", dto.getIsValid())
                .set("remark", remark)
                .setSql(invalidateTokens, "token_version = token_version + 1");
        sysUserMapper.update(null, update);

        updateUserDepts(userId, deptIds);
        updateUserRoles(userId, roleIds);

        String detail = String.format(
                "修改人员信息: %s（%s -> %s）；部门 %s -> %s；角色 %s -> %s；状态 %s -> %s",
                dto.getRealName().trim(), user.getStudentNo(), studentNo,
                oldDeptIds, deptIds, oldRoleIds, roleIds, user.getIsValid(), dto.getIsValid());

        operationLogService.logChange(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, detail,
                userAuditSnapshot(user.getStudentNo(), user.getGender(), user.getCollege(),
                        user.getClassName(), oldDeptIds, oldRoleIds, user.getIsValid()),
                userAuditSnapshot(studentNo, dto.getGender(), college,
                        className, deptIds, roleIds, dto.getIsValid()), "");

        if (!Objects.equals(user.getPhoto(), newPhoto)) {
            avatarStorageService.deleteAfterCommit(user.getPhoto());
        }

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
                sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                        .eq("id", userId)
                        .set("is_valid", dto.getIsValid())
                        .setSql("token_version = token_version + 1"));
            } else if (roleIds != null) {
                incrementTokenVersion(userId);
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

        sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .set("is_valid", status)
                .setSql("token_version = token_version + 1"));

        String statusDesc = status == 1 ? "启用" : "禁用";
        operationLogService.log(operatorId, operatorName, operatorRole, "UPDATE",
                "USER", userId, statusDesc + "用户: " + user.getRealName(), "");

        log.info("User {} status updated to {} by operator {}", userId, status, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId, Long operatorId, String operatorName, String operatorRole) {
        if (Objects.equals(userId, operatorId)) {
            throw ApiException.badRequest("不能删除当前登录账号");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }

        try {
            // 202609011700 - 完成了人员永久删除，并保留与操作者相关的历史审计信息。
            operationLogService.detachUserReferences(userId);
            sysUserDeptMapper.delete(new QueryWrapper<SysUserDept>().eq("user_id", userId));
            sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("user_id", userId));
            int deleted = sysUserMapper.deleteById(userId);
            if (deleted != 1) {
                throw ApiException.notFound("用户不存在");
            }
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict("该人员已有稿费、作品或其他业务关联记录，不能永久删除，请改为禁用账号");
        }

        operationLogService.log(operatorId, operatorName, operatorRole, "DELETE",
                "USER", userId,
                "永久删除人员: " + user.getRealName() + "（账号: " + user.getUsername() + "）", "");
        avatarStorageService.deleteAfterCommit(user.getPhoto());
        log.info("User {} permanently deleted by operator {}", userId, operatorId);
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

    private String normalizeAndValidatePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim();
        if (!normalized.matches(PHONE_PATTERN)) {
            throw ApiException.badRequest("请输入正确的11位手机号");
        }
        return normalized;
    }

    // 202608121431 - 增加了新增和修改人员时的手机号唯一性校验。
    private void ensurePhoneAvailable(String phone, Long excludedUserId) {
        QueryWrapper<SysUser> phoneCheck = new QueryWrapper<>();
        phoneCheck.and(query -> query.eq("phone", phone).or().eq("username", phone));
        if (excludedUserId != null) {
            phoneCheck.ne("id", excludedUserId);
        }
        if (sysUserMapper.selectCount(phoneCheck) > 0) {
            throw ApiException.conflict("该手机号已被其他用户使用");
        }
    }

    private void validateCompleteUserUpdate(UserUpdateDTO dto) {
        if (dto == null
                || !StringUtils.hasText(dto.getRealName())
                || !StringUtils.hasText(dto.getStudentNo())
                ) {
            throw ApiException.badRequest("姓名和学号不能为空");
        }
        if (dto.getGender() == null || dto.getGender() < 0 || dto.getGender() > 2) {
            throw ApiException.badRequest("性别参数不正确");
        }
        if (dto.getIsValid() == null || (dto.getIsValid() != 0 && dto.getIsValid() != 1)) {
            throw ApiException.badRequest("账号状态只能为 0 或 1");
        }
    }

    private List<Long> optionalDistinctIds(List<Long> ids) {
        return ids == null ? Collections.emptyList() : ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void ensureStudentNoAvailable(String studentNo, Long excludedUserId) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.and(wrapper -> wrapper.eq("student_no", studentNo).or().eq("username", studentNo));
        if (excludedUserId != null) query.ne("id", excludedUserId);
        if (sysUserMapper.selectCount(query) > 0) {
            throw ApiException.conflict("该学号或登录账号已被其他用户使用");
        }
    }

    private void ensureIdCardAvailable(String idCard, Long excludedUserId) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("id_card", idCard);
        if (excludedUserId != null) query.ne("id", excludedUserId);
        if (sysUserMapper.selectCount(query) > 0) {
            throw ApiException.conflict("该身份证号已被其他用户使用");
        }
    }

    private List<Long> currentDeptIds(Long userId) {
        return sysUserDeptMapper.selectList(new QueryWrapper<SysUserDept>().eq("user_id", userId))
                .stream().map(SysUserDept::getDeptId).distinct().collect(Collectors.toList());
    }

    private List<Long> currentRoleIds(Long userId) {
        return sysUserRoleMapper.selectList(new QueryWrapper<SysUserRole>().eq("user_id", userId))
                .stream().map(SysUserRole::getRoleId).distinct().collect(Collectors.toList());
    }

    private boolean sameIds(List<Long> left, List<Long> right) {
        return left.size() == right.size() && left.containsAll(right) && right.containsAll(left);
    }

    private void incrementTokenVersion(Long userId) {
        sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .setSql("token_version = token_version + 1"));
    }

    private String userAuditSnapshot(String studentNo, Integer gender, String college,
                                     String className, List<Long> deptIds,
                                     List<Long> roleIds, Integer isValid) {
        return String.format(
                "{\"studentNo\":\"%s\",\"gender\":%s,\"college\":\"%s\","
                        + "\"className\":\"%s\",\"deptIds\":\"%s\","
                        + "\"roleIds\":\"%s\",\"isValid\":%s}",
                escapeAudit(studentNo), gender, escapeAudit(college), escapeAudit(className),
                deptIds, roleIds, isValid);
    }

    private String profileAuditSnapshot(String realName, String studentNo, Integer gender,
                                        String college, String className) {
        return String.format(
                "{\"realName\":\"%s\",\"studentNo\":\"%s\",\"gender\":%s,"
                        + "\"college\":\"%s\",\"className\":\"%s\","
                        + "\"sensitiveFields\":\"已脱敏\"}",
                escapeAudit(realName), escapeAudit(studentNo), gender,
                escapeAudit(college), escapeAudit(className));
    }

    private String escapeAudit(String value) {
        return nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
