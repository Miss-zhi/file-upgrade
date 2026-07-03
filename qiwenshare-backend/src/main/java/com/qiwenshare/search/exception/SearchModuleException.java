package com.qiwenshare.search.exception;

import lombok.Getter;

/**
 * 搜索模块统一业务异常。
 *
 * <p>携带 {@link SearchErrorCode}，由全局异常处理器统一处理。</p>
 */
@Getter
public class SearchModuleException extends RuntimeException {

    private final SearchErrorCode errorCode;

    /**
     * 构造搜索模块异常。
     *
     * @param errorCode 错误码枚举
     */
    public SearchModuleException(SearchErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造搜索模块异常（带原始异常）。
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public SearchModuleException(SearchErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
