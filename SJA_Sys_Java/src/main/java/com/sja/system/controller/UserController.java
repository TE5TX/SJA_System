package com.sja.system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserBatchUpdateDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserDeleteDTO;
import com.sja.system.dto.UserExportDTO;
import com.sja.system.dto.UserImportConfirmDTO;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.dto.UserUpdateDTO;
import com.sja.system.dto.UserStatusUpdateDTO;
import com.sja.system.dto.UserPhotoUpdateDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.AsyncTaskQueueService;
import com.sja.system.service.UserService;
import com.sja.system.service.UserImportService;
import com.sja.system.service.UserRosterImportService;
import com.sja.system.vo.AsyncTaskAcceptedVO;
import com.sja.system.vo.UserImportPreviewVO;
import com.sja.system.vo.UserRosterPreviewVO;
import com.sja.system.vo.UserRosterImportResultVO;
import com.sja.system.vo.UserImportResultVO;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * User management controller.
 * Handles profile self-service and admin user management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private static final Map<String, ExportField> EXPORT_FIELDS = createExportFields();

    private final UserService userService;
    private final UserImportService userImportService;
    private final UserRosterImportService userRosterImportService;
    private final OperationLogService operationLogService;
    private final SecurityUtils securityUtils;
    private final AsyncTaskQueueService asyncTaskQueueService;

    /**
     * Get the current authenticated user's profile.
     */
    @GetMapping("/user/profile")
    public Result<UserVO> getProfile(HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        UserVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    /**
     * Update the current authenticated user's profile fields.
     */
    @PutMapping("/user/profile")
    public ResponseEntity<Result<AsyncTaskAcceptedVO>> updateProfile(
            @RequestBody @Valid ProfileUpdateDTO dto,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(401, "未授权"));
        }
        // 202608192148 - 修复了大量用户同时修改个人资料时数据库连接被瞬间占满的问题。
        AsyncTaskAcceptedVO task = asyncTaskQueueService.enqueueProfile(
                userId, dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Result.success("个人资料修改任务已进入队列", task));
    }

    @PutMapping("/user/profile/photo")
    public Result<Void> updateProfilePhoto(@RequestBody @Valid UserPhotoUpdateDTO body, HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        userService.updateProfilePhoto(userId, body.getPhoto(), userId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success();
    }

    @DeleteMapping("/user/profile/photo")
    public Result<Void> deleteProfilePhoto(HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        userService.updateProfilePhoto(userId, null, userId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success();
    }

    /**
     * Change the current authenticated user's password.
     */
    @PutMapping("/user/profile/password")
    public Result<Void> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto, HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        userService.updatePassword(userId, dto);
        return Result.success();
    }

    /**
     * Admin: query users with filters and pagination.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('sys:users:view')")
    public Result<PageResult<UserVO>> queryUsers(@Valid UserQueryDTO query) {
        PageResult<UserVO> result = userService.queryUsers(query);
        return Result.success(result);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<UserVO> getManagedUserDetail(@PathVariable Long id) {
        return Result.success(userService.getManagedUserDetail(id));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Long> createUser(@RequestBody @Valid UserCreateDTO dto,
                                   HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }

        Long userId = userService.createUser(dto, operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success("人员新增成功", userId);
    }

    // 202608210907 - 增加了管理员、主席和秘书长可用的XLSX批量注册预览与确认接口。
    @PostMapping("/users/import/preview")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public Result<UserImportPreviewVO> previewUserImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("deptIds") List<Long> deptIds,
            @RequestParam("roleIds") List<Long> roleIds,
            HttpServletRequest request) {
        return Result.success(userImportService.preview(file, deptIds, roleIds,
                securityUtils.getCurrentUserId(request), securityUtils.getCurrentRoleCode(request)));
    }

    @GetMapping("/users/import/template")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public void downloadUserImportTemplate(HttpServletResponse response) throws IOException {
        writeUserImportTemplate(response, "人员批量注册模板.xlsx", userImportService.createBasicTemplate());
    }

    // 202609181445 - 提供九列名单模板接口，并复用现有批量注册模板的 HTTP 响应处理。
    @GetMapping("/users/import/roster/template")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public void downloadUserRosterImportTemplate(HttpServletResponse response) throws IOException {
        writeUserImportTemplate(response, "人员批量导入模板.xlsx", userImportService.createRosterTemplate());
    }

    // 202609181445 - 提供独立的九列名单解析预览接口，保持原两列批量注册接口行为不变。
    @PostMapping("/users/import/roster/preview")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public Result<UserRosterPreviewVO> previewUserRosterImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roleId") Long roleId,
            HttpServletRequest request) {
        return Result.success(userImportService.previewRoster(file, roleId,
                securityUtils.getCurrentUserId(request), securityUtils.getCurrentRoleCode(request)));
    }

    // 202609181445 - 提供九列名单的一次性确认接口，与旧版两列确认流程隔离。
    @PostMapping("/users/import/roster/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public Result<UserRosterImportResultVO> confirmUserRosterImport(
            @RequestBody @Valid UserImportConfirmDTO dto, HttpServletRequest request) {
        return Result.success("人员名单导入处理完成", userRosterImportService.confirm(
                dto.getImportToken(), dto.getCurrentPassword(),
                securityUtils.getCurrentUserId(request),
                securityUtils.getCurrentUsernameFromToken(request),
                securityUtils.getCurrentRoleCode(request), request.getRemoteAddr()));
    }

    private void writeUserImportTemplate(HttpServletResponse response, String fileName, byte[] content)
            throws IOException {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.getOutputStream().write(content);
    }

    @PostMapping("/users/import/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_0', 'ROLE_1', 'ROLE_3')")
    public Result<UserImportResultVO> confirmUserImport(
            @RequestBody @Valid UserImportConfirmDTO dto,
            HttpServletRequest request) {
        return Result.success("人员批量注册成功", userImportService.confirm(
                dto.getImportToken(), dto.getCurrentPassword(),
                securityUtils.getCurrentUserId(request),
                securityUtils.getCurrentUsernameFromToken(request),
                securityUtils.getCurrentRoleCode(request)));
    }

    @PostMapping("/users/export")
    @PreAuthorize("hasAuthority('sys:users:view')")
    public void exportUsers(@RequestBody @Valid UserExportDTO dto,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        Long operatorId = securityUtils.getCurrentUserId(request);
        if (operatorId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未授权");
            return;
        }

        List<ExportField> selectedFields = dto.getFields().stream()
                .map(EXPORT_FIELDS::get)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (selectedFields.size() != dto.getFields().stream().distinct().count()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "包含不支持的导出字段");
            return;
        }

        List<UserVO> users = userService.queryUsersForExport(dto);
        if (users.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        String fileName = "人员信息_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        List<List<String>> head = selectedFields.stream()
                .map(field -> Collections.singletonList(field.label))
                .collect(Collectors.toList());
        List<List<String>> data = users.stream()
                .map(user -> selectedFields.stream()
                        .map(field -> field.valueExtractor.apply(user))
                        .map(value -> value == null ? "" : value)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                .sheet("人员信息")
                .doWrite(data);

        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        operationLogService.log(operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "",
                "EXPORT", "USER", null,
                "导出人员信息，共 " + users.size() + " 条，字段：" + dto.getFields(),
                request.getRemoteAddr());
    }

    @PutMapping("/users/batch")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> batchUpdateUsers(@RequestBody @Valid UserBatchUpdateDTO dto,
                                         HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }

        // 202608131617 - 为人员信息批量修改增加了当前用户密码二次校验。
        userService.verifyCurrentPassword(operatorId, dto.getCurrentPassword());
        userService.batchUpdateUsers(dto, operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success();
    }

    /**
     * Admin: update a user's information.
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUser(@PathVariable Long id,
                                    @RequestBody @Valid UserUpdateDTO updates,
                                   HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        String operatorName = securityUtils.getCurrentUsernameFromToken(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        String operatorRole = roleCode != null ? String.valueOf(roleCode) : "";

        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        userService.updateUser(id, updates, operatorId, operatorName, operatorRole);
        return Result.success();
    }

    // 202609011700 - 完成了人员管理密码二次确认后的人员永久删除功能。
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> deleteUser(@PathVariable Long id,
                                   @RequestBody @Valid UserDeleteDTO body,
                                   HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }

        userService.verifyCurrentPassword(operatorId, body.getCurrentPassword());
        userService.deleteUser(id, operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success("人员删除成功", null);
    }

    @PutMapping("/users/{id}/password/reset")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> resetUserPassword(@PathVariable Long id, HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        userService.resetUserPassword(id, operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success();
    }

    @PutMapping("/users/{id}/photo")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUserPhoto(@PathVariable Long id,
                                         @RequestBody @Valid UserPhotoUpdateDTO body,
                                        HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        userService.updateProfilePhoto(id, body.getPhoto(), operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success();
    }

    @DeleteMapping("/users/{id}/photo")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> deleteUserPhoto(@PathVariable Long id, HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        if (operatorId == null) {
            return Result.error(401, "未授权");
        }
        userService.updateProfilePhoto(id, null, operatorId,
                securityUtils.getCurrentUsernameFromToken(request),
                roleCode != null ? String.valueOf(roleCode) : "");
        return Result.success();
    }

    /**
     * Admin: enable or disable a user account.
     */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestBody @Valid UserStatusUpdateDTO body,
                                         HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        String operatorName = securityUtils.getCurrentUsernameFromToken(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        String operatorRole = roleCode != null ? String.valueOf(roleCode) : "";

        if (operatorId == null) {
            return Result.error(401, "未授权");
        }

        userService.updateUserStatus(id, body.getStatus(), operatorId, operatorName, operatorRole);
        return Result.success();
    }

    private static Map<String, ExportField> createExportFields() {
        Map<String, ExportField> fields = new LinkedHashMap<>();
        fields.put("realName", new ExportField("姓名", UserVO::getRealName));
        fields.put("gender", new ExportField("性别", user -> formatGender(user.getGender())));
        fields.put("studentNo", new ExportField("学号", UserVO::getStudentNo));
        fields.put("phone", new ExportField("手机号", UserVO::getPhone));
        fields.put("idCard", new ExportField("身份证号", UserVO::getIdCard));
        fields.put("bankCard", new ExportField("银行卡号", UserVO::getBankCard));
        fields.put("college", new ExportField("学院", UserVO::getCollege));
        fields.put("className", new ExportField("班级", UserVO::getClassName));
        fields.put("dormAddress", new ExportField("宿舍地址", UserVO::getDormAddress));
        fields.put("deptNames", new ExportField("部门", user -> join(user.getDeptNames())));
        fields.put("roleNames", new ExportField("角色", user -> join(user.getRoleNames())));
        fields.put("isValid", new ExportField("状态", user -> user.getIsValid() != null && user.getIsValid() == 1 ? "启用" : "禁用"));
        fields.put("photo", new ExportField("照片地址", UserVO::getPhoto));
        fields.put("remark", new ExportField("备注", UserVO::getRemark));
        return Collections.unmodifiableMap(fields);
    }

    private static String formatGender(Integer gender) {
        if (gender == null || gender == 0) {
            return "未知";
        }
        return gender == 1 ? "男" : "女";
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join("、", values);
    }

    private static final class ExportField {
        private final String label;
        private final Function<UserVO, String> valueExtractor;

        private ExportField(String label, Function<UserVO, String> valueExtractor) {
            this.label = label;
            this.valueExtractor = valueExtractor;
        }
    }
}
