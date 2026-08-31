package com.sja.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.dto.RecruitmentRegistrationDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.entity.SysDept;
import com.sja.system.entity.SysRecruitmentForm;
import com.sja.system.entity.SysRole;
import com.sja.system.mapper.SysDeptMapper;
import com.sja.system.mapper.SysRecruitmentFormMapper;
import com.sja.system.mapper.SysRoleMapper;
import com.sja.system.vo.RecruitmentStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private static final long FORM_ID = 1L;
    private static final int VISITOR_ROLE_CODE = 13;
    private static final String INITIAL_PASSWORD = "asdf123456";
    private static final Set<Integer> MANAGER_ROLE_CODES = Set.of(0, 1, 3);
    private static final Set<String> ALLOWED_DEPT_CODES =
            Set.of("EDITORIAL", "PHOTOGRAPHY", "VISUAL", "VIDEO");

    private final SysRecruitmentFormMapper recruitmentFormMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final UserService userService;
    private final AvatarStorageService avatarStorageService;
    private final OperationLogService operationLogService;

    public RecruitmentStatusVO getStatus() {
        return toStatus(requireState(recruitmentFormMapper.selectById(FORM_ID)));
    }

    @Transactional(rollbackFor = Exception.class)
    public RecruitmentStatusVO updateStatus(boolean published, Long operatorId,
                                            String operatorName, Integer roleCode) {
        assertManager(roleCode);
        SysRecruitmentForm state = requireState(recruitmentFormMapper.selectRegistrationStateForUpdate());
        LocalDateTime now = LocalDateTime.now();
        boolean changed = !Objects.equals(state.getIsPublished(), published ? 1 : 0);

        state.setIsPublished(published ? 1 : 0);
        state.setUpdatedBy(operatorId);
        state.setUpdateTime(now);
        if (published && changed) {
            state.setPublishedBy(operatorId);
            state.setPublishedAt(now);
        }
        if (recruitmentFormMapper.updateById(state) != 1) {
            throw new IllegalStateException("招新表单状态更新失败");
        }

        operationLogService.log(operatorId, operatorName, String.valueOf(roleCode), "UPDATE",
                "RECRUITMENT", FORM_ID, published ? "发布招新注册表单" : "暂停招新注册表单", "");
        return toStatus(state);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(RecruitmentRegistrationDTO dto, MultipartFile photo) {
        // 202608312329 - 共享锁允许并发报名，并确保暂停操作与报名提交互斥。
        SysRecruitmentForm state = requireState(recruitmentFormMapper.selectRegistrationStateForShare());
        if (!Objects.equals(state.getIsPublished(), 1)) {
            throw ApiException.conflict("招新注册当前已暂停");
        }

        String deptCode = dto.getDeptCode().trim().toUpperCase();
        if (!ALLOWED_DEPT_CODES.contains(deptCode)) {
            throw ApiException.badRequest("所属部门不正确");
        }
        SysDept dept = deptMapper.selectOne(new QueryWrapper<SysDept>()
                .eq("dept_code", deptCode)
                .eq("is_valid", 1));
        if (dept == null) {
            throw ApiException.badRequest("所选部门不存在或已禁用");
        }
        SysRole visitorRole = roleMapper.selectOne(new QueryWrapper<SysRole>()
                .eq("role_code", VISITOR_ROLE_CODE)
                .eq("is_valid", 1));
        if (visitorRole == null) {
            throw ApiException.serviceUnavailable("游客角色未正确配置");
        }

        String photoUrl = avatarStorageService.store(photo);
        avatarStorageService.deleteAfterRollback(photoUrl);

        UserCreateDTO user = new UserCreateDTO();
        user.setRealName(dto.getRealName());
        user.setStudentNo(dto.getStudentNo());
        user.setPassword(INITIAL_PASSWORD);
        user.setGender(dto.getGender());
        user.setPhoto(photoUrl);
        user.setIdCard(dto.getIdCard());
        user.setPhone(dto.getPhone());
        user.setCollege(dto.getCollege());
        user.setClassName(dto.getClassName());
        user.setDormAddress(dto.getDormAddress());
        user.setBankCard(dto.getBankCard());
        user.setDeptIds(List.of(dept.getId()));
        user.setRoleIds(List.of(visitorRole.getId()));
        user.setIsValid(1);
        user.setRemark("通过招新注册表单创建");
        try {
            userService.createUser(user, null, "招新注册表单", String.valueOf(VISITOR_ROLE_CODE));
        } catch (DuplicateKeyException exception) {
            // 202609010054 - 并发重复报名最终以数据库唯一约束为准，并返回明确的业务错误。
            throw ApiException.conflict("学号、手机号或身份证号已被注册");
        }
    }

    private void assertManager(Integer roleCode) {
        if (roleCode == null || !MANAGER_ROLE_CODES.contains(roleCode)) {
            throw ApiException.forbidden("只有主席、秘书长和管理员可以管理招新表单");
        }
    }

    private SysRecruitmentForm requireState(SysRecruitmentForm state) {
        if (state == null) {
            throw ApiException.serviceUnavailable("招新表单尚未初始化，请先执行数据库升级脚本");
        }
        return state;
    }

    private RecruitmentStatusVO toStatus(SysRecruitmentForm state) {
        RecruitmentStatusVO status = new RecruitmentStatusVO();
        status.setPublished(Objects.equals(state.getIsPublished(), 1));
        status.setPublishedAt(state.getPublishedAt());
        status.setUpdatedAt(state.getUpdateTime());
        return status;
    }
}
