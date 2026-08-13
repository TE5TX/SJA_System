package com.sja.system.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserBatchUpdateDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserExportDTO;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.dto.UserUpdateDTO;
import com.sja.system.dto.UserStatusUpdateDTO;
import com.sja.system.dto.UserPhotoUpdateDTO;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserService;
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
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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
    private final OperationLogService operationLogService;
    private final SecurityUtils securityUtils;

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
    public Result<Void> updateProfile(@RequestBody @Valid ProfileUpdateDTO dto, HttpServletRequest request) {
        Long userId = securityUtils.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未授权");
        }
        userService.updateProfile(userId, dto);
        return Result.success();
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
