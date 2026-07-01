package com.sja.system.controller;

import com.sja.system.common.PageResult;
import com.sja.system.common.Result;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.security.JwtUtils;
import com.sja.system.security.SecurityUtils;
import com.sja.system.service.OperationLogService;
import com.sja.system.service.UserService;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * User management controller.
 * Handles profile self-service and admin user management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

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
    public Result<PageResult<UserVO>> queryUsers(UserQueryDTO query) {
        PageResult<UserVO> result = userService.queryUsers(query);
        return Result.success(result);
    }

    /**
     * Admin: update a user's information.
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUser(@PathVariable Long id,
                                   @RequestBody Map<String, Object> updates,
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

    /**
     * Admin: enable or disable a user account.
     */
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('sys:users:edit')")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        Long operatorId = securityUtils.getCurrentUserId(request);
        String operatorName = securityUtils.getCurrentUsernameFromToken(request);
        Integer roleCode = securityUtils.getCurrentRoleCode(request);
        String operatorRole = roleCode != null ? String.valueOf(roleCode) : "";

        if (operatorId == null) {
            return Result.error(401, "未授权");
        }

        Integer status = (Integer) body.get("status");
        if (status == null) {
            return Result.error("缺少 status 参数");
        }

        userService.updateUserStatus(id, status, operatorId, operatorName, operatorRole);
        return Result.success();
    }
}
