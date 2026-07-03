package com.qiwenshare.admin.common;

import lombok.Getter;

/**
 * Admin 模块统一业务异常。
 *
 * <p>携带 {@link AdminErrorCode}，由 {@code AdminGlobalExceptionHandler} 统一处理。
 * Service 层通过抛出此异常传递业务错误信息。</p>
 */
@Getter
public class AdminModuleException extends RuntimeException {

    private final AdminErrorCode errorCode;

    /**
     * 构造 admin 模块异常。
     *
     * @param errorCode 错误码枚举
     */
    public AdminModuleException(AdminErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
