package com.sja.system.controller.support;

import com.sja.system.application.model.TokenPair;
import com.sja.system.security.AuthenticationCookieNames;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 将应用层生成的 Access Token、Refresh Token 写入浏览器 Cookie。
 *
 * 这是 Controller（HTTP）这一层的辅助组件：
 * 只关心“怎么通过 HTTP 响应把 Cookie 发给浏览器”，
 * 不负责登录校验、Token 生成等认证业务。
 */
@Component // 交给 Spring 管理，其他类可通过构造器注入它
// 202609201535 - 完成了Access Token和Refresh Token统一写入HttpOnly Cookie的任务。
public class AuthenticationCookieWriter {

    /**
     * Access Token 的过期时长，单位为毫秒。
     * 值来自 application.yml / application.properties 中的 jwt.access-expiration。
     */
    private final long accessExpiration;

    /**
     * Refresh Token 的过期时长，单位为毫秒。
     * 值来自 jwt.refresh-expiration。
     */
    private final long refreshExpiration;

    /**
     * Cookie 是否只能通过 HTTPS 传输。
     *
     * true：浏览器仅在 HTTPS 请求中发送 Cookie，生产环境建议开启。
     * false：HTTP 本地开发环境也能使用。
     */
    private final boolean secure;

    /**
     * Spring 创建此组件时，会从配置文件读取三个配置项并传入。
     *
     * @Value("${sja.security.refresh-cookie-secure:false}")
     * 表示：若未配置该项，默认使用 false。
     */
    public AuthenticationCookieWriter(
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration,
            @Value("${sja.security.refresh-cookie-secure:false}") boolean secure) {
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.secure = secure;
    }

    /**
     * 将登录成功后生成的一对 Token 写入响应 Cookie。
     *
     * 浏览器收到响应后，会自动保存这两个 Cookie；
     * 后续请求匹配对应 path 时，浏览器会自动带上它们。
     *
     * @param response HTTP 响应对象
     * @param tokenPair 包含 accessToken 和 refreshToken 的对象
     */
    public void write(HttpServletResponse response, TokenPair tokenPair) {
        // Access Token：
        // Cookie 仅对 /api 及其子路径生效，例如 /api/v1/users。
        addCookie(
                response,
                AuthenticationCookieNames.ACCESS,
                tokenPair.getAccessToken(),
                "/api",
                accessExpiration
        );

        // Refresh Token：
        // 范围更小，只会发送给 /api/v1/auth 及其子路径。
        // 一般用于 /refresh、/logout 等认证接口。
        addCookie(
                response,
                AuthenticationCookieNames.REFRESH,
                tokenPair.getRefreshToken(),
                "/api/v1/auth",
                refreshExpiration
        );
    }

    /**
     * 清除浏览器已保存的认证 Cookie，通常在退出登录时调用。
     *
     * 清除 Cookie 的关键：
     * 1. Cookie 名称必须一致；
     * 2. path 必须与创建 Cookie 时一致；
     * 3. maxAge 设置为 0，浏览器会立即删除它。
     *
     * @param response HTTP 响应对象
     */
    public void clear(HttpServletResponse response) {
        // 用空值 + 过期时间 0 删除 Access Token Cookie
        addCookie(response, AuthenticationCookieNames.ACCESS, "", "/api", 0L);

        // 用空值 + 过期时间 0 删除 Refresh Token Cookie
        addCookie(response, AuthenticationCookieNames.REFRESH, "", "/api/v1/auth", 0L);
    }

    /**
     * 创建 Cookie 并通过 HTTP 响应头 Set-Cookie 发送给浏览器。
     *
     * @param response       HTTP 响应对象
     * @param name           Cookie 名称
     * @param value          Cookie 内容，即 Token 字符串
     * @param path           Cookie 生效的请求路径
     * @param maxAgeMillis   Cookie 存活时间，单位为毫秒；0 表示立即过期
     */
    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path,
            long maxAgeMillis
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)

                // 禁止 JavaScript 读取 Cookie，降低 XSS 窃取 Token 的风险
                .httpOnly(true)

                // true 时 Cookie 只能在 HTTPS 下传输
                .secure(secure)

                // 限制跨站请求携带 Cookie，降低 CSRF 风险
                .sameSite("Strict")

                // 限制 Cookie 发送的 URL 范围
                .path(path)

                // 毫秒转换为 Duration，再设置 Cookie 有效期
                .maxAge(Duration.ofMillis(maxAgeMillis))

                .build();

        // 向响应中加入：
        // Set-Cookie: cookie名称=token值; Path=...; HttpOnly; ...
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}