package com.qiwenshare.file.advice;

import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.util.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerAdvice {

    @ExceptionHandler(QiwenException.class)
    public RestResult<Void> handleQiwenException(QiwenException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return RestResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public RestResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return RestResult.fail("系统内部错误");
    }
}
