package com.sja.system.controller.user;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.framework.web.support.DownloadResponseFactory;
import com.sja.system.application.dto.user.PasswordUpdateDTO;
import com.sja.system.application.dto.user.ProfileUpdateDTO;
import com.sja.system.application.dto.user.UserBatchUpdateDTO;
import com.sja.system.application.dto.user.UserCreateDTO;
import com.sja.system.application.dto.user.UserDeleteDTO;
import com.sja.system.application.dto.user.UserExportDTO;
import com.sja.system.application.dto.user.UserImportConfirmDTO;
import com.sja.system.application.dto.user.UserQueryDTO;
import com.sja.system.application.dto.user.UserUpdateDTO;
import com.sja.system.application.dto.user.UserStatusUpdateDTO;
import com.sja.system.application.dto.user.UserPhotoUpdateDTO;
import com.sja.system.framework.web.MultipartUploadedFile;
import com.sja.system.service.user.UserApplicationService;
import com.sja.system.application.vo.async.AsyncTaskAcceptedVO;
import com.sja.system.application.vo.user.UserImportPreviewVO;
import com.sja.system.application.vo.user.UserRosterPreviewVO;
import com.sja.system.application.vo.user.UserRosterImportResultVO;
import com.sja.system.application.vo.user.UserImportResultVO;
import com.sja.system.application.vo.user.UserVO;
import com.sja.system.application.vo.user.ImportArchiveVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 个人资料与人员管理协议适配器，仅负责HTTP参数绑定、鉴权和响应转换。
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
// 202609211059 - 完成了人员Controller业务编排下沉，仅保留HTTP参数与响应适配。
public class UserController {

    private final UserApplicationService userApplicationService;

    /** 查询当前登录人的个人资料。 */
    @GetMapping("/user/profile")
    public Result<UserVO> getProfile(CurrentActor actor) {
        return Result.success(userApplicationService.profile(actor));
    }

    /** 接收当前登录人的资料修改异步任务。 */
    @PutMapping("/user/profile")
    public ResponseEntity<Result<AsyncTaskAcceptedVO>> updateProfile(
            @RequestBody @Valid ProfileUpdateDTO dto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            CurrentActor actor) {
        AsyncTaskAcceptedVO task = userApplicationService.updateProfile(dto, idempotencyKey, actor);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success("个人资料修改任务已进入队列", task));
    }

    @PutMapping("/user/profile/photo")
    public Result<Void> updateProfilePhoto(@RequestBody @Valid UserPhotoUpdateDTO body, CurrentActor actor) {
        userApplicationService.updateOwnPhoto(body.getPhoto(), actor);
        return Result.success();
    }

    @DeleteMapping("/user/profile/photo")
    public Result<Void> deleteProfilePhoto(CurrentActor actor) {
        userApplicationService.updateOwnPhoto(null, actor);
        return Result.success();
    }

    /** 修改当前登录人的密码。 */
    @PutMapping("/user/profile/password")
    public Result<Void> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto, CurrentActor actor) {
        userApplicationService.updatePassword(dto, actor);
        return Result.success();
    }

    /** 按条件分页查询人员。 */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('sys:users:view')")
    public Result<PageResult<UserVO>> queryUsers(@Valid UserQueryDTO query) {
        return Result.success(userApplicationService.query(query));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<UserVO> getManagedUserDetail(@PathVariable Long id) {
        return Result.success(userApplicationService.managedDetail(id));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Long> createUser(@RequestBody @Valid UserCreateDTO dto,
                                   CurrentActor actor) {
        return Result.success("人员新增成功", userApplicationService.create(dto, actor));
    }

    // 202608210907 - 增加了管理员、主席和秘书长可用的XLSX批量注册预览与确认接口。
    @PostMapping("/users/import/preview")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public Result<UserImportPreviewVO> previewUserImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("deptIds") List<Long> deptIds,
            @RequestParam("roleIds") List<Long> roleIds,
            CurrentActor actor) {
        // 202609211059 - 完成了Web上传对象向应用层文件模型的适配。
        return Result.success(userApplicationService.previewImport(
                new MultipartUploadedFile(file), deptIds, roleIds, actor));
    }

    @GetMapping("/users/import/template")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public ResponseEntity<byte[]> downloadUserImportTemplate() {
        return DownloadResponseFactory.create(userApplicationService.basicImportTemplate());
    }

    // 202609181445 - 提供九列名单模板接口，并复用现有批量注册模板的 HTTP 响应处理。
    @GetMapping("/users/import/roster/template")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public ResponseEntity<byte[]> downloadUserRosterImportTemplate() {
        return DownloadResponseFactory.create(userApplicationService.rosterImportTemplate());
    }

    // 202609181445 - 提供独立的九列名单解析预览接口，保持原两列批量注册接口行为不变。
    @PostMapping("/users/import/roster/preview")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public Result<UserRosterPreviewVO> previewUserRosterImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roleId") Long roleId,
            CurrentActor actor) {
        return Result.success(userApplicationService.previewRoster(
                new MultipartUploadedFile(file), roleId, actor));
    }

    // 202609181445 - 提供九列名单的一次性确认接口，与旧版两列确认流程隔离。
    @PostMapping("/users/import/roster/confirm")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public Result<UserRosterImportResultVO> confirmUserRosterImport(
            @RequestBody @Valid UserImportConfirmDTO dto, CurrentActor actor,
            HttpServletRequest request) {
        return Result.success("人员名单导入处理完成",
                userApplicationService.confirmRoster(dto, actor, request.getRemoteAddr()));
    }

    @PostMapping("/users/import/confirm")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public Result<UserImportResultVO> confirmUserImport(
            @RequestBody @Valid UserImportConfirmDTO dto,
            CurrentActor actor) {
        return Result.success("人员批量注册成功", userApplicationService.confirmImport(dto, actor));
    }

    // 202609291708 - 提供受权限保护的原始XLSX归档查询和下载；Controller仅转换HTTP响应。
    @GetMapping("/users/import/archives")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public Result<PageResult<ImportArchiveVO>> getImportArchives(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize, CurrentActor actor) {
        return Result.success(userApplicationService.importArchives(pageNum, pageSize, actor));
    }

    @GetMapping("/users/import/archives/{id}/file")
    @PreAuthorize("@roleAuthorization.canImportUsers(authentication)")
    public ResponseEntity<byte[]> downloadImportArchive(@PathVariable String id, CurrentActor actor) {
        ResponseEntity<byte[]> file = DownloadResponseFactory.create(userApplicationService.importArchive(id, actor));
        // 202609291708 - 原始名单包含个人信息，下载响应禁止浏览器和中间代理缓存。
        return ResponseEntity.ok().headers(file.getHeaders())
                .cacheControl(CacheControl.noStore()).body(file.getBody());
    }

    @PostMapping("/users/export")
    @PreAuthorize("hasAuthority('sys:users:view')")
    public ResponseEntity<byte[]> exportUsers(@RequestBody @Valid UserExportDTO dto,
                                              CurrentActor actor, HttpServletRequest request) {
        var exportFile = userApplicationService.export(dto, actor, request.getRemoteAddr());
        if (exportFile.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return DownloadResponseFactory.create(exportFile.get());
    }

    @PutMapping("/users/batch")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> batchUpdateUsers(@RequestBody @Valid UserBatchUpdateDTO dto,
                                         CurrentActor actor) {
        userApplicationService.batchUpdate(dto, actor);
        return Result.success();
    }

    /** 修改受管理人员的信息。 */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUser(@PathVariable Long id,
                                   @RequestBody @Valid UserUpdateDTO updates,
                                   CurrentActor actor) {
        userApplicationService.update(id, updates, actor);
        return Result.success();
    }

    // 202609011700 - 完成了人员管理密码二次确认后的人员永久删除功能。
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> deleteUser(@PathVariable Long id,
                                   @RequestBody @Valid UserDeleteDTO body,
                                   CurrentActor actor) {
        userApplicationService.delete(id, body, actor);
        return Result.success("人员删除成功", null);
    }

    @PutMapping("/users/{id}/password/reset")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> resetUserPassword(@PathVariable Long id, CurrentActor actor) {
        userApplicationService.resetPassword(id, actor);
        return Result.success();
    }

    @PutMapping("/users/{id}/photo")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUserPhoto(@PathVariable Long id,
                                        @RequestBody @Valid UserPhotoUpdateDTO body,
                                        CurrentActor actor) {
        userApplicationService.updatePhoto(id, body.getPhoto(), actor);
        return Result.success();
    }

    @DeleteMapping("/users/{id}/photo")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> deleteUserPhoto(@PathVariable Long id, CurrentActor actor) {
        userApplicationService.updatePhoto(id, null, actor);
        return Result.success();
    }

    /** 启用或停用受管理人员账号。 */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestBody @Valid UserStatusUpdateDTO body,
                                         CurrentActor actor) {
        userApplicationService.updateStatus(id, body, actor);
        return Result.success();
    }

}
