package com.qiwenshare.admin.common;

import com.qiwenshare.auth.common.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Admin 模块全局异常处理器。
 *
 * <p>处理 {@link AdminModuleException}，返回 {@link RestResult} 格式的错误响应。</p>
 */
@RestControllerAdvice
@Slf4j
public class AdminGlobalExceptionHandler {

    /**
     * 处理 admin 模块业务异常。
     *
     * @param e admin 模块异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(AdminModuleException.class)
    public ResponseEntity<RestResult<Void>> handleAdminModuleException(AdminModuleException e) {
        int httpStatus = e.getErrorCode().getHttpStatus();
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage();
        log.debug("Admin 模块异常: code={}, message={}", code, message);
        return ResponseEntity.status(httpStatus)
                .body(new RestResult<>(-1, code, message, null));
    }
}
