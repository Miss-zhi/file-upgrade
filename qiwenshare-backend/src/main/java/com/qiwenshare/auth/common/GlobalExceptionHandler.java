package com.qiwenshare.auth.common;

import com.qiwenshare.auth.exception.AuthException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>统一捕获 {@link AuthException}、参数校验异常、权限异常等，
 * 返回 {@link RestResult} 格式的 JSON 响应。Controller 和 Service 中
 * 不做 try-catch，异常统一由此处理器处理。</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理认证业务异常。
     *
     * @param e 认证异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<RestResult<Void>> handleAuthException(AuthException e) {
        int httpStatus = e.getErrorCode().getHttpStatus();
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage();
        return ResponseEntity.status(httpStatus)
                .body(new RestResult<>(-1, code, message, null));
    }

    /**
     * 处理 jakarta.validation 参数校验异常（@Valid 注解触发）。
     *
     * @param e 参数校验异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResult<String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new RestResult<>(-1, "VALIDATION_ERROR", message, null));
    }

    /**
     * 处理 ConstraintViolation 异常（@Validated 类级别校验触发）。
     *
     * @param e 约束违反异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RestResult<String>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new RestResult<>(-1, "VALIDATION_ERROR", message, null));
    }

    /**
     * 处理 Spring Security 访问拒绝异常。
     *
     * @param e 访问拒绝异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestResult<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RestResult<>(-1, "ACCESS_DENIED", "权限不足", null));
    }

    /**
     * 处理未预期的系统异常。
     *
     * @param e 未知异常
     * @return RestResult 格式的错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResult<Void>> handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new RestResult<>(-1, "INTERNAL_ERROR", "服务器内部错误", null));
    }
}
