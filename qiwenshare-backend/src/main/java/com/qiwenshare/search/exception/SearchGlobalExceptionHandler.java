package com.qiwenshare.search.exception;

import com.qiwenshare.auth.common.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 搜索模块全局异常处理器。
 *
 * <p>处理 {@link SearchModuleException}，根据 {@link SearchErrorCode#getHttpStatus()}
 * 返回正确的 HTTP 状态码（如 503、400），而非笼统的 500。</p>
 */
@RestControllerAdvice
@Slf4j
public class SearchGlobalExceptionHandler {

    /**
     * 处理搜索模块业务异常。
     *
     * @param e 搜索模块异常
     * @return RestResult 格式的错误响应，HTTP 状态码由 errorCode 决定
     */
    @ExceptionHandler(SearchModuleException.class)
    public ResponseEntity<RestResult<Void>> handleSearchModuleException(SearchModuleException e) {
        int httpStatus = e.getErrorCode().getHttpStatus();
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage();
        log.debug("搜索模块异常: code={}, httpStatus={}, message={}", code, httpStatus, message);
        return ResponseEntity.status(httpStatus)
                .body(RestResult.<Void>error(code, message));
    }
}
