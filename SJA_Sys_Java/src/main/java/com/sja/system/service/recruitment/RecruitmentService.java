package com.sja.system.service.recruitment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.service.authorization.KnownRole;
import com.sja.system.service.authorization.RoleAuthorizationPolicy;
import com.sja.system.common.ApiException;
import com.sja.system.application.dto.recruitment.RecruitmentRegistrationDTO;
import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.dal.dataobject.SysDept;
import com.sja.system.dal.dataobject.SysRecruitmentForm;
import com.sja.system.dal.dataobject.SysRole;
import com.sja.system.dal.mysql.SysDeptMapper;
import com.sja.system.dal.mysql.SysRecruitmentFormMapper;
import com.sja.system.dal.mysql.SysRoleMapper;
import com.sja.system.application.vo.recruitment.RecruitmentStatusVO;
import com.sja.system.service.auth.LoginCaptchaService;
import com.sja.system.service.system.OperationLogService;
import com.sja.system.service.upload.AvatarStorageService;
import com.sja.system.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private static final long FORM_ID = 1L;
    private static final Set<String> ALLOWED_DEPT_CODES =
            Set.of("EDITORIAL", "PHOTOGRAPHY", "VISUAL", "VIDEO");

    private final SysRecruitmentFormMapper recruitmentFormMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final UserService userService;
    private final AvatarStorageService avatarStorageService;
    private final OperationLogService operationLogService;
    private final LoginCaptchaService loginCaptchaService;

    public RecruitmentStatusVO getStatus() {
        return toStatus(requireState(recruitmentFormMapper.selectById(FORM_ID)));
    }

    @Transactional(rollbackFor = Exception.class)
    public RecruitmentStatusVO updateStatus(boolean published, LocalDateTime publishStartAt,
                                            LocalDateTime publishEndAt, Long operatorId,
                                            String operatorName, Integer roleCode) {
        assertManager(roleCode);
        SysRecruitmentForm state = requireState(recruitmentFormMapper.selectRegistrationStateForUpdate());
        LocalDateTime now = LocalDateTime.now();
        boolean changed = !Objects.equals(state.getIsPublished(), published ? 1 : 0);

        // 202609011534 - 发布状态与开放时间段一起保存，防止仅靠前端限制注册时间。
        LocalDateTime effectiveStartAt = published && publishStartAt == null ? now : publishStartAt;
        validateSchedule(effectiveStartAt, publishEndAt);

        state.setIsPublished(published ? 1 : 0);
        state.setPublishStartAt(effectiveStartAt);
        state.setPublishEndAt(publishEndAt);
        state.setUpdatedBy(operatorId);
        state.setUpdateTime(now);
        if (published && changed) {
            state.setPublishedBy(operatorId);
            state.setPublishedAt(now);
        }
        if (recruitmentFormMapper.updateById(state) != 1) {
            throw new IllegalStateException("招新表单状态更新失败");
        }

        String detail = published ? "发布招新注册表单" : "暂停招新注册表单";
        operationLogService.log(operatorId, operatorName, String.valueOf(roleCode), "UPDATE",
                "RECRUITMENT", FORM_ID, detail, "开放时间=" + effectiveStartAt + "；结束时间=" + publishEndAt);
        return toStatus(state);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(RecruitmentRegistrationDTO dto, UploadedFile photo) {
        // 202609181625 - 将招新验证码消费下沉至报名用例，避免其他调用入口绕过人机校验。
        if (!loginCaptchaService.consume(dto.getCaptchaToken().trim())) {
            throw ApiException.badRequest("验证码无效或已过期，请重新验证");
        }
        // 202608312329 - 共享锁允许并发报名，并确保暂停操作与报名提交互斥。
        SysRecruitmentForm state = requireState(recruitmentFormMapper.selectRegistrationStateForShare());
        assertRegistrationOpen(state, LocalDateTime.now());

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw ApiException.badRequest("两次输入的密码不一致");
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
                .eq("role_code", KnownRole.VISITOR.code())
                .eq("is_valid", 1));
        if (visitorRole == null) {
            throw ApiException.serviceUnavailable("游客角色未正确配置");
        }

        String photoUrl = null;
        if (photo != null && !photo.empty()) {
            photoUrl = avatarStorageService.store(photo);
            avatarStorageService.deleteAfterRollback(photoUrl);
        }

        UserCreateDTO user = new UserCreateDTO();
        user.setRealName(dto.getRealName());
        user.setStudentNo(dto.getStudentNo());
        user.setPassword(dto.getPassword());
        user.setRequirePasswordChange(false);
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
            userService.createUser(user, null, dto.getRealName().trim(),
                    String.valueOf(KnownRole.VISITOR.code()));
        } catch (DuplicateKeyException exception) {
            // 202609010054 - 并发重复报名最终以数据库唯一约束为准，并返回明确的业务错误。
            throw ApiException.conflict("学号、手机号或身份证号已被注册");
        }
    }

    private void assertManager(Integer roleCode) {
        if (!RoleAuthorizationPolicy.canManageRecruitment(roleCode)) {
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
        status.setPublishStartAt(state.getPublishStartAt());
        status.setPublishEndAt(state.getPublishEndAt());
        status.setAcceptingRegistration(isRegistrationOpen(state, LocalDateTime.now()));
        status.setUpdatedAt(state.getUpdateTime());
        return status;
    }

    private void validateSchedule(LocalDateTime publishStartAt, LocalDateTime publishEndAt) {
        if (publishStartAt != null && publishEndAt != null && !publishEndAt.isAfter(publishStartAt)) {
            throw ApiException.badRequest("结束时间必须晚于开放时间");
        }
    }

    private void assertRegistrationOpen(SysRecruitmentForm state, LocalDateTime now) {
        if (!Objects.equals(state.getIsPublished(), 1)) {
            throw ApiException.conflict("注册当前已暂停");
        }
        if (state.getPublishStartAt() != null && now.isBefore(state.getPublishStartAt())) {
            throw ApiException.conflict("注册尚未开始");
        }
        if (state.getPublishEndAt() != null && !now.isBefore(state.getPublishEndAt())) {
            throw ApiException.conflict("注册已结束");
        }
    }

    private boolean isRegistrationOpen(SysRecruitmentForm state, LocalDateTime now) {
        return Objects.equals(state.getIsPublished(), 1)
                && (state.getPublishStartAt() == null || !now.isBefore(state.getPublishStartAt()))
                && (state.getPublishEndAt() == null || now.isBefore(state.getPublishEndAt()));
    }
}
