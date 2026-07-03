package com.qiwenshare.document.exception;

import lombok.Getter;

/**
 * 文档模块业务异常。
 *
 * <p>携带 {@link DocumentErrorCode}，由全局异常处理器统一处理。</p>
 */
@Getter
public class DocumentModuleException extends RuntimeException {

    private final DocumentErrorCode errorCode;

    /**
     * 构造文档模块异常。
     *
     * @param errorCode 错误码枚举
     */
    public DocumentModuleException(DocumentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造文档模块异常（带原始异常）。
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public DocumentModuleException(DocumentErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
