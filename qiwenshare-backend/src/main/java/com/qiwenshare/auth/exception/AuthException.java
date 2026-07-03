package com.qiwenshare.auth.exception;

import lombok.Getter;

/**
 * 认证模块统一业务异常。
 *
 * <p>携带 {@link AuthErrorCode}，由 {@code GlobalExceptionHandler} 统一处理。
 * Service 层通过抛出此异常传递业务错误信息。</p>
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;

    /**
     * 构造认证异常。
     *
     * @param errorCode 错误码枚举
     */
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
