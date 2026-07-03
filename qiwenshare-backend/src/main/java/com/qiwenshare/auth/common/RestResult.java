package com.qiwenshare.auth.common;

/**
 * 统一 API 响应包装。
 *
 * <p>{@code code = 0} 表示成功，{@code code} 为错误码字符串表示失败。
 * 所有 Controller 返回值 MUST 使用此类型包装。</p>
 *
 * @param code    业务状态码，0 表示成功
 * @param message 提示信息
 * @param data    响应数据，失败时为 null
 * @param <T>     数据类型
 */
public record RestResult<T>(int code, String errorCode, String message, T data) {

    /**
     * 成功响应（带数据）。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return RestResult 实例
     */
    public static <T> RestResult<T> success(T data) {
        return new RestResult<>(0, null, "success", data);
    }

    /**
     * 成功响应（带自定义消息和数据）。
     *
     * @param message 提示信息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return RestResult 实例
     */
    public static <T> RestResult<T> success(String message, T data) {
        return new RestResult<>(0, null, message, data);
    }

    /**
     * 成功响应（无数据）。
     *
     * @param message 提示信息
     * @return RestResult 实例
     */
    public static <T> RestResult<T> success(String message) {
        return new RestResult<>(0, null, message, null);
    }

    /**
     * 失败响应。
     *
     * @param errorCode 错误码（如 VALIDATION_ERROR、USERNAME_EXISTS）
     * @param message   错误信息（用户可读的具体描述）
     * @param <T>       数据类型
     * @return RestResult 实例
     */
    public static <T> RestResult<T> error(String errorCode, String message) {
        return new RestResult<>(-1, errorCode, message, null);
    }
}
