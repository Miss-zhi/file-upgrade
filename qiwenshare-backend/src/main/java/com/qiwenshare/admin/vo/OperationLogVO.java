package com.qiwenshare.admin.vo;

import java.time.LocalDateTime;

/**
 * 操作日志响应体。
 *
 * @param id            主键
 * @param userId        操作者业务 ID
 * @param username      操作者用户名
 * @param module        模块名
 * @param action        操作类型
 * @param description   操作描述
 * @param requestMethod HTTP 方法
 * @param requestUri    请求 URI
 * @param requestParams 请求参数 JSON
 * @param responseCode  响应状态码
 * @param errorMessage  异常信息
 * @param ipAddress     客户端 IP
 * @param userAgent     User-Agent
 * @param executionTime 执行耗时 ms
 * @param createTime    记录时间
 */
public record OperationLogVO(
        Long id,
        String userId,
        String username,
        String module,
        String action,
        String description,
        String requestMethod,
        String requestUri,
        String requestParams,
        Integer responseCode,
        String errorMessage,
        String ipAddress,
        String userAgent,
        Long executionTime,
        LocalDateTime createTime
) {
}
