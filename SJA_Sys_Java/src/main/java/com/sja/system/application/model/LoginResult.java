package com.sja.system.application.model;

import com.sja.system.vo.LoginVO;

/**
 * 登录响应数据以及由 Web 适配层写入 HttpOnly Cookie 的认证令牌。
 */
// 202609201535 - 完成了Access Token与Refresh Token均由服务层签发、Web层写入安全Cookie的任务。
public record LoginResult(LoginVO response, TokenPair tokenPair) {
}
