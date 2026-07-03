package com.qiwenshare.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证配置属性。
 *
 * <p>从 {@code auth.*} 配置项绑定。</p>
 *
 * @param loginFailMax         登录失败最大次数，默认 5
 * @param lockDurationMinutes  锁定时长（分钟），默认 15
 * @param cookieSecure         cookie Secure 标志，prod 环境为 true
 * @param cookieSameSite       cookie SameSite 属性，默认 Lax
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        int loginFailMax,
        int lockDurationMinutes,
        boolean cookieSecure,
        String cookieSameSite
) {

    public AuthProperties {
        if (loginFailMax <= 0) {
            loginFailMax = 5;
        }
        if (lockDurationMinutes <= 0) {
            lockDurationMinutes = 15;
        }
        if (cookieSameSite == null) {
            cookieSameSite = "Lax";
        }
    }
}
