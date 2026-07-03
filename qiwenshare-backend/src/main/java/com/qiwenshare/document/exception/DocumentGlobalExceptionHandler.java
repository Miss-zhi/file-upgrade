package com.qiwenshare.document.exception;

import com.qiwenshare.auth.common.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 文档模块全局异常处理器。
 *
 * <p>处理 {@link DocumentModuleException}，根据 {@link DocumentErrorCode#getHttpStatus()}
 * 返回正确的 HTTP 状态码。同时处理参数校验、类型转换和兜底异常。</p>
 */
@RestControllerAdvice
@Slf4j
public class DocumentGlobalExceptionHandler {

    /**
     * 处理文档模块业务异常。
     *
     * @param e 文档模块异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(DocumentModuleException.class)
    public ResponseEntity<RestResult<Void>> handleDocumentModuleException(DocumentModuleException e) {
        int httpStatus = e.getErrorCode().getHttpStatus();
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage();
        log.debug("文档模块异常: code={}, httpStatus={}, message={}", code, httpStatus, message);
        return ResponseEntity.status(httpStatus)
                .body(RestResult.<Void>error(code, message));
    }

    /**
     * 处理参数校验异常（{@code @Valid} 触发）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResult<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.debug("参数校验失败: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResult.<Void>error("VALIDATION_ERROR", message));
    }

    /**
     * 处理类型转换异常（如路径参数 Long 解析失败）。
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<RestResult<Void>> handleNumberFormatException(NumberFormatException e) {
        log.debug("数字格式异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResult.<Void>error("INVALID_NUMBER", "参数格式错误: " + e.getMessage()));
    }

    /**
     * 兜底异常处理，捕获所有未明确处理的异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResult<Void>> handleException(Exception e) {
        log.error("未预期异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResult.<Void>error("INTERNAL_ERROR", "服务器内部错误"));
    }
}
