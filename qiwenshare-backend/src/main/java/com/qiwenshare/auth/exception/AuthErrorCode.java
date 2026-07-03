package com.qiwenshare.auth.exception;

import lombok.Getter;

/**
 * 认证模块错误码枚举。
 *
 * <p>每个错误码包含 HTTP 状态码和业务消息。
 * Controller 和 Service 通过抛出 {@link AuthException} 携带此枚举，
 * 由 {@code GlobalExceptionHandler} 统一处理。</p>
 */
@Getter
public enum AuthErrorCode {

    AUTH_INVALID_CREDENTIALS(401, "手机号或密码错误"),
    AUTH_USER_DISABLED(403, "账户已被禁用"),
    AUTH_ACCOUNT_LOCKED(423, "账户已锁定，请15分钟后重试"),
    AUTH_NOT_AUTHENTICATED(401, "请先登录"),
    AUTH_TOKEN_EXPIRED(401, "Token 已过期"),
    AUTH_TOKEN_REVOKED(401, "Token 已被撤销"),
    AUTH_TOKEN_INVALID(401, "Token 无效"),
    AUTH_OLD_PASSWORD_WRONG(401, "原密码错误"),
    REFRESH_TOKEN_EXPIRED(401, "登录已过期，请重新登录"),
    TOKEN_REUSE_DETECTED(401, "检测到 token 重用，所有会话已失效"),
    INVALID_TOKEN(401, "Token 无效"),
    USERNAME_EXISTS(400, "用户名已存在"),
    TELEPHONE_EXISTS(400, "手机号已注册"),
    TELEPHONE_INVALID(400, "手机号格式不正确"),
    PASSWORD_WEAK(400, "密码需包含大小写字母和数字，长度8-30位"),
    PASSWORD_SAME(400, "新密码不能与旧密码相同"),
    USER_NOT_FOUND(404, "用户不存在"),
    CANNOT_DISABLE_SELF(400, "不能禁用自己的账号"),
    ACCESS_DENIED(403, "权限不足");

    private final int httpStatus;
    private final String message;

    AuthErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
