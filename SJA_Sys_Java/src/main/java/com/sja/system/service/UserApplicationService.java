package com.sja.system.service;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.UserExportFile;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.common.PageResult;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserBatchUpdateDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserDeleteDTO;
import com.sja.system.dto.UserExportDTO;
import com.sja.system.dto.UserImportConfirmDTO;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.dto.UserStatusUpdateDTO;
import com.sja.system.dto.UserUpdateDTO;
import com.sja.system.vo.AsyncTaskAcceptedVO;
import com.sja.system.vo.UserImportPreviewVO;
import com.sja.system.vo.UserImportResultVO;
import com.sja.system.vo.UserRosterImportResultVO;
import com.sja.system.vo.UserRosterPreviewVO;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 人员与个人资料应用用例协调服务。
 *
 * <p>统一处理敏感操作密码复核、异步资料写入、导入导出和审计操作人传递，
 * 避免 Controller 跨多个业务服务编排调用顺序。</p>
 */
@Service
@RequiredArgsConstructor
// 202609211059 - 完成了人员Controller中的队列、密码复核、导入导出和审计编排下沉。
public class UserApplicationService {

    private final UserService userService;
    private final UserImportService userImportService;
    private final UserRosterImportService userRosterImportService;
    private final UserExportService userExportService;
    private final AsyncTaskQueueService asyncTaskQueueService;

    public UserVO profile(CurrentActor actor) {
        return userService.getUserProfile(actor.userId());
    }

    public AsyncTaskAcceptedVO updateProfile(ProfileUpdateDTO dto, String idempotencyKey, CurrentActor actor) {
        return asyncTaskQueueService.enqueueProfile(actor.userId(), dto, idempotencyKey);
    }

    public void updateOwnPhoto(String photo, CurrentActor actor) {
        updatePhoto(actor.userId(), photo, actor);
    }

    public void updatePassword(PasswordUpdateDTO dto, CurrentActor actor) {
        userService.updatePassword(actor.userId(), dto);
    }

    public PageResult<UserVO> query(UserQueryDTO query) {
        return userService.queryUsers(query);
    }

    public UserVO managedDetail(Long userId) {
        return userService.getManagedUserDetail(userId);
    }

    public Long create(UserCreateDTO dto, CurrentActor actor) {
        return userService.createUser(dto, actor.userId(), actor.username(), actor.roleText());
    }

    public UserImportPreviewVO previewImport(UploadedFile file, List<Long> deptIds,
                                             List<Long> roleIds, CurrentActor actor) {
        return userImportService.preview(file, deptIds, roleIds, actor.userId(), actor.roleCode());
    }

    public UserExportFile basicImportTemplate() {
        return new UserExportFile("人员批量注册模板.xlsx", UserExportFile.XLSX_CONTENT_TYPE,
                userImportService.createBasicTemplate());
    }

    public UserExportFile rosterImportTemplate() {
        return new UserExportFile("人员批量导入模板.xlsx", UserExportFile.XLSX_CONTENT_TYPE,
                userImportService.createRosterTemplate());
    }

    public UserRosterPreviewVO previewRoster(UploadedFile file, Long roleId, CurrentActor actor) {
        return userImportService.previewRoster(file, roleId, actor.userId(), actor.roleCode());
    }

    public UserRosterImportResultVO confirmRoster(UserImportConfirmDTO dto, CurrentActor actor, String clientIp) {
        return userRosterImportService.confirm(dto.getImportToken(), dto.getCurrentPassword(),
                actor.userId(), actor.username(), actor.roleCode(), clientIp);
    }

    public UserImportResultVO confirmImport(UserImportConfirmDTO dto, CurrentActor actor) {
        return userImportService.confirm(dto.getImportToken(), dto.getCurrentPassword(),
                actor.userId(), actor.username(), actor.roleCode());
    }

    public Optional<UserExportFile> export(UserExportDTO dto, CurrentActor actor, String clientIp) {
        return userExportService.export(dto, actor.userId(), actor.username(), actor.roleCode(), clientIp);
    }

    /** 人员批量修改的密码复核和写入必须作为一个完整应用用例执行。 */
    public void batchUpdate(UserBatchUpdateDTO dto, CurrentActor actor) {
        userService.verifyCurrentPassword(actor.userId(), dto.getCurrentPassword());
        userService.batchUpdateUsers(dto, actor.userId(), actor.username(), actor.roleText());
    }

    public void update(Long userId, UserUpdateDTO dto, CurrentActor actor) {
        userService.updateUser(userId, dto, actor.userId(), actor.username(), actor.roleText());
    }

    /** 人员删除前的密码复核由应用服务统一保证，Controller 不参与调用顺序。 */
    public void delete(Long userId, UserDeleteDTO dto, CurrentActor actor) {
        userService.verifyCurrentPassword(actor.userId(), dto.getCurrentPassword());
        userService.deleteUser(userId, actor.userId(), actor.username(), actor.roleText());
    }

    public void resetPassword(Long userId, CurrentActor actor) {
        userService.resetUserPassword(userId, actor.userId(), actor.username(), actor.roleText());
    }

    public void updatePhoto(Long userId, String photo, CurrentActor actor) {
        userService.updateProfilePhoto(userId, photo, actor.userId(), actor.username(), actor.roleText());
    }

    public void updateStatus(Long userId, UserStatusUpdateDTO dto, CurrentActor actor) {
        userService.updateUserStatus(userId, dto.getStatus(), actor.userId(),
                actor.username(), actor.roleText());
    }
}
