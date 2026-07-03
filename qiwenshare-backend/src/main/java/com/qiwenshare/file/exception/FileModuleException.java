package com.qiwenshare.file.exception;

import lombok.Getter;

/**
 * 文件模块统一业务异常。
 *
 * <p>携带 {@link FileErrorCode}，由全局异常处理器统一处理。
 * Service 层通过抛出此异常传递业务错误信息。</p>
 */
@Getter
public class FileModuleException extends RuntimeException {

    private final FileErrorCode errorCode;

    /**
     * 构造文件模块异常。
     *
     * @param errorCode 错误码枚举
     */
    public FileModuleException(FileErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
