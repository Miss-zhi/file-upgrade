package com.qiwenshare.file.common;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.file.exception.FileModuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 文件模块全局异常处理器。
 *
 * <p>处理 {@link FileModuleException}，返回 {@link RestResult} 格式的错误响应。</p>
 */
@RestControllerAdvice
@Slf4j
public class FileGlobalExceptionHandler {

    /**
     * 处理文件模块业务异常。
     *
     * @param e 文件模块异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(FileModuleException.class)
    public ResponseEntity<RestResult<Void>> handleFileModuleException(FileModuleException e) {
        int httpStatus = e.getErrorCode().getHttpStatus();
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage();
        log.debug("文件模块异常: code={}, message={}", code, message);
        return ResponseEntity.status(httpStatus)
                .body(new RestResult<>(-1, code, message, null));
    }
}
