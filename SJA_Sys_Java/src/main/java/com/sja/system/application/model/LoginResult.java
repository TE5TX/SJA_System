package com.sja.system.application.model;

import com.sja.system.vo.LoginVO;

/**
 * Login response data and the refresh token that the web adapter writes to an HTTP-only cookie.
 */
public record LoginResult(LoginVO response, String refreshToken) {
}
