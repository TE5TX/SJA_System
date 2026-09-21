package com.sja.system.framework.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sja.system.common.Constants;
import com.sja.system.common.Result;
import com.sja.system.dal.dataobject.SysUser;
import com.sja.system.dal.mysql.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// 202608101042 - 修复了首次登录可以跳过修改初始密码的问题。
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.equals("/api/v1/user/profile/password")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            QueryWrapper<SysUser> query = new QueryWrapper<>();
            query.select("id", "must_change_password")
                    .eq("username", authentication.getName())
                    .eq("is_valid", 1);
            SysUser user = sysUserMapper.selectOne(query);
            if (user != null && Integer.valueOf(1).equals(user.getMustChangePassword())) {
                response.setStatus(Constants.FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.error(Constants.FORBIDDEN, "首次登录请先修改密码")));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
