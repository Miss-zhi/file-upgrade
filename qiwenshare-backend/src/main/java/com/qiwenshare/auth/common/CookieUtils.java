package com.qiwenshare.auth.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * HTTP Cookie 工具类。
 *
 * <p>构建和清除 httpOnly cookie。{@code Secure} 标志根据当前 profile 决定：
 * dev 环境为 false，prod 环境为 true。{@code SameSite} 默认 Lax。</p>
 */
@Component
public class CookieUtils {

    @Value("${auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${auth.cookie-same-site:Lax}")
    private String cookieSameSite;

    /**
     * 构建 httpOnly cookie。
     *
     * @param name   cookie 名称
     * @param value  cookie 值（JWT token）
     * @param maxAge 有效期（秒）
     * @param path   cookie 路径
     * @return ResponseCookie 实例
     */
    public ResponseCookie buildCookie(String name, String value, long maxAge, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    /**
     * 构建 httpOnly cookie（默认路径 /）。
     *
     * @param name   cookie 名称
     * @param value  cookie 值
     * @param maxAge 有效期（秒）
     * @return ResponseCookie 实例
     */
    public ResponseCookie buildCookie(String name, String value, long maxAge) {
        return buildCookie(name, value, maxAge, "/");
    }

    /**
     * 清除 cookie（Max-Age=0）。
     *
     * @param name cookie 名称
     * @param path cookie 路径
     * @return ResponseCookie 实例（已过期）
     */
    public ResponseCookie clearCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(path)
                .maxAge(0)
                .build();
    }

    /**
     * 清除 cookie（默认路径 /）。
     *
     * @param name cookie 名称
     * @return ResponseCookie 实例（已过期）
     */
    public ResponseCookie clearCookie(String name) {
        return clearCookie(name, "/");
    }
}
