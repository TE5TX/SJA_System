package com.sja.system.service.user;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.UserExportFile;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.application.model.BufferedUploadedFile;
import com.sja.system.common.PageResult;
import com.sja.system.application.dto.user.PasswordUpdateDTO;
import com.sja.system.application.dto.user.ProfileUpdateDTO;
import com.sja.system.application.dto.user.UserBatchUpdateDTO;
import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.application.dto.user.UserDeleteDTO;
import com.sja.system.application.dto.user.UserExportDTO;
import com.sja.system.application.dto.user.UserImportConfirmDTO;
import com.sja.system.application.dto.user.UserQueryDTO;
import com.sja.system.application.dto.user.UserStatusUpdateDTO;
import com.sja.system.application.dto.user.UserUpdateDTO;
import com.sja.system.application.vo.async.AsyncTaskAcceptedVO;
import com.sja.system.application.vo.user.UserImportPreviewVO;
import com.sja.system.application.vo.user.UserImportResultVO;
import com.sja.system.application.vo.user.UserRosterImportResultVO;
import com.sja.system.application.vo.user.UserRosterPreviewVO;
import com.sja.system.application.vo.user.UserVO;
import com.sja.system.application.vo.user.ImportArchiveVO;
import com.sja.system.service.async.AsyncTaskQueueService;
import com.sja.system.service.storage.ImportArchiveService;
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
    private final ImportArchiveService importArchiveService;

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
        BufferedUploadedFile captured = BufferedUploadedFile.capture(file, 2L * 1024 * 1024);
        UserImportPreviewVO preview = userImportService.preview(captured, deptIds, roleIds,
                actor.userId(), actor.roleCode());
        // 202609291708 - 可确认的预览必须先保存原始XLSX，归档失败则不向前端发放令牌。
        importArchiveService.savePreview(captured, preview.getImportToken(), ImportArchiveService.BASIC,
                actor, preview.getTotal());
        return preview;
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
        BufferedUploadedFile captured = BufferedUploadedFile.capture(file, 2L * 1024 * 1024);
        UserRosterPreviewVO preview = userImportService.previewRoster(captured, roleId,
                actor.userId(), actor.roleCode());
        importArchiveService.savePreview(captured, preview.importToken(), ImportArchiveService.ROSTER,
                actor, preview.total());
        return preview;
    }

    public UserRosterImportResultVO confirmRoster(UserImportConfirmDTO dto, CurrentActor actor, String clientIp) {
        importArchiveService.requirePreview(dto.getImportToken(), actor, ImportArchiveService.ROSTER);
        try {
            UserRosterImportResultVO result = userRosterImportService.confirm(dto.getImportToken(), dto.getCurrentPassword(),
                    actor.userId(), actor.username(), actor.roleCode(), clientIp);
            importArchiveService.finishRoster(dto.getImportToken(), actor, result.importedCount(),
                    result.skippedCount(), result.unprocessedCount(), result.aborted(), result.abortReason());
            return result;
        } catch (RuntimeException exception) {
            // 202609291708 - 确认异常只记录待核查信息，不将可能部分成功的名单误标为失败。
            importArchiveService.noteFailedAttempt(dto.getImportToken(), actor, ImportArchiveService.ROSTER, exception);
            throw exception;
        }
    }

    public UserImportResultVO confirmImport(UserImportConfirmDTO dto, CurrentActor actor) {
        importArchiveService.requirePreview(dto.getImportToken(), actor, ImportArchiveService.BASIC);
        try {
            UserImportResultVO result = userImportService.confirm(dto.getImportToken(), dto.getCurrentPassword(),
                    actor.userId(), actor.username(), actor.roleCode());
            importArchiveService.finishBasic(dto.getImportToken(), actor, result.getImportedCount());
            return result;
        } catch (RuntimeException exception) {
            importArchiveService.noteFailedAttempt(dto.getImportToken(), actor, ImportArchiveService.BASIC, exception);
            throw exception;
        }
    }

    /** 查询和下载归档由应用层统一交给归档服务进行角色及完整性校验。 */
    // 202609291708 - 完成了人员导入原件列表和受控下载用例。
    public PageResult<ImportArchiveVO> importArchives(int pageNum, int pageSize, CurrentActor actor) {
        var page = importArchiveService.page(pageNum, pageSize, actor);
        var views = page.getList().stream().map(item -> new ImportArchiveVO(item.id(),
                item.originalName(), item.sha256(), item.fileSize(), item.importType(),
                item.operatorName(), item.status(), item.totalCount(), item.importedCount(),
                item.skippedCount(), item.unprocessedCount(), item.message(),
                item.createdAt(), item.confirmedAt())).toList();
        return new PageResult<>(views, page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    public UserExportFile importArchive(String id, CurrentActor actor) {
        var file = importArchiveService.download(id, actor);
        return new UserExportFile(file.fileName(), UserExportFile.XLSX_CONTENT_TYPE, file.content());
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
