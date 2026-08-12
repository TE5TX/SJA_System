package com.sja.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sja.system.common.Result;
import com.sja.system.common.ApiException;
import com.sja.system.dto.LoginDTO;
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
import com.sja.system.security.JwtUtils;
import com.sja.system.service.SystemService;
import com.sja.system.service.UserService;
import com.sja.system.vo.LoginVO;
import com.sja.system.vo.MenuTreeVO;
import com.sja.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication controller handling login, logout, and token refresh.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserDeptMapper sysUserDeptMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SystemService systemService;
    private final UserService userService;

    /**
     * User login endpoint.
     * Authenticates credentials, generates JWT, and returns user info with menu tree.
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        String loginIdentifier = dto.getUsername().trim();
        try {
            // 1. Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, dto.getPassword())
            );

            // 手机号认证成功后，authentication.getName() 是数据库中的真实登录账号。
            QueryWrapper<SysUser> userQuery = new QueryWrapper<>();
            userQuery.eq("username", authentication.getName()).eq("is_valid", 1);
            SysUser user = sysUserMapper.selectOne(userQuery);

            if (user == null) {
                throw ApiException.unauthorized("用户不存在或已被禁用");
            }

            // 3. Load user roles
            QueryWrapper<SysUserRole> roleQuery = new QueryWrapper<>();
            roleQuery.eq("user_id", user.getId());
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(roleQuery);

            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());

            // Find highest (lowest number = highest privilege) role code.
            // Default to visitor to avoid treating ordinary members as secretary.
            Integer highestRoleCode = 13;
            List<Integer> roleCodes = new ArrayList<>();
            if (!roleIds.isEmpty()) {
                QueryWrapper<SysRole> rQuery = new QueryWrapper<>();
                rQuery.in("id", roleIds).eq("is_valid", 1);
                List<SysRole> roles = sysRoleMapper.selectList(rQuery);
                for (SysRole role : roles) {
                    roleCodes.add(role.getRoleCode());
                    if (role.getRoleCode() < highestRoleCode) {
                        highestRoleCode = role.getRoleCode();
                    }
                }
            }

            // 4. Generate JWT token
            String token = jwtUtils.generateToken(user.getId(), user.getUsername(), highestRoleCode);

            // 5. Build user VO
            UserVO userVO = userService.getUserProfile(user.getId());

            // 6. Build menu tree based on user's role codes
            List<MenuTreeVO> menus;
            if (roleCodes.isEmpty()) {
                menus = systemService.getMenuTree();
            } else {
                menus = systemService.getMenuTreeByRoleCodes(roleCodes);
            }

            // 7. Build response
            LoginVO loginVO = new LoginVO();
            loginVO.setToken(token);
            loginVO.setUserInfo(userVO);
            loginVO.setMenus(menus);
            loginVO.setMustChangePassword(Integer.valueOf(1).equals(user.getMustChangePassword()));

            log.info("User logged in: {}", user.getUsername());
            return Result.success(loginVO);

        } catch (BadCredentialsException e) {
            log.warn("Login failed - bad credentials for user: {}", loginIdentifier);
            throw ApiException.unauthorized("用户名或密码错误");
        } catch (AuthenticationException e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw ApiException.unauthorized("认证失败");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            throw new RuntimeException("登录失败，请稍后重试", e);
        }
    }

    /**
     * Logout endpoint. Since JWT is stateless, this just returns success.
     * The frontend should clear the stored token.
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("登出成功");
    }

    /**
     * Refresh the JWT token. Validates the current token and issues a new one.
     */
    @PostMapping("/refresh")
    public Result<String> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw ApiException.unauthorized("未提供有效的认证令牌");
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.validateToken(token)) {
            throw ApiException.unauthorized("令牌已过期或无效");
        }

        Long userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);
        Integer roleCode = jwtUtils.getRoleCode(token);

        String newToken = jwtUtils.generateToken(userId, username, roleCode);
        return Result.success(newToken);
    }
}
