package com.sja.system.framework.security;

/**
 * 认证 Cookie 名称的唯一来源，避免过滤器与 Web 适配层使用不一致的常量。
 */
// 202609201535 - 完成了认证Cookie名称集中管理的任务。
public final class AuthenticationCookieNames {

    public static final String ACCESS = "sja_access_token";
    public static final String REFRESH = "sja_refresh_token";

    private AuthenticationCookieNames() {
    }
}
