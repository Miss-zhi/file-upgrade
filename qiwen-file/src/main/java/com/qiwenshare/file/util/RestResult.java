package com.qiwenshare.file.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestResult<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;
    private long total;

    // ==================== 静态工厂方法 ====================

    public static <T> RestResult<T> success() {
        RestResult<T> result = new RestResult<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> RestResult<T> success(T data) {
        RestResult<T> result = new RestResult<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> RestResult<T> success(T data, long total) {
        RestResult<T> result = new RestResult<>();
        result.setSuccess(true);
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        result.setTotal(total);
        return result;
    }

    public static <T> RestResult<T> fail() {
        RestResult<T> result = new RestResult<>();
        result.setSuccess(false);
        result.setCode(500);
        result.setMessage("操作失败");
        return result;
    }

    public static <T> RestResult<T> fail(String message) {
        RestResult<T> result = new RestResult<>();
        result.setSuccess(false);
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> RestResult<T> fail(int code, String message) {
        RestResult<T> result = new RestResult<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
