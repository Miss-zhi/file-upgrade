package com.qiwenshare.admin.common;

import lombok.Getter;

/**
 * Admin 模块错误码枚举。
 *
 * <p>每个错误码包含 HTTP 状态码和业务消息。
 * Service 层通过抛出 {@link AdminModuleException} 携带此枚举，
 * 由 {@code AdminGlobalExceptionHandler} 统一处理。</p>
 */
@Getter
public enum AdminErrorCode {

    // 用户管理错误
    USER_NOT_FOUND(404, "用户不存在"),
    CANNOT_DISABLE_SELF(400, "不能禁用自己的账号"),
    USER_DISABLED(403, "账户已被禁用"),

    // 配额管理错误
    INVALID_QUOTA(400, "配额值不合法"),

    // 系统配置错误
    CONFIG_KEY_DUPLICATE(400, "参数键名已存在"),
    CONFIG_NOT_FOUND(404, "系统参数不存在");

    private final int httpStatus;
    private final String message;

    AdminErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
