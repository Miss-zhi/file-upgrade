package com.qiwenshare.file.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建分享请求体。
 *
 * @param userFileId   文件 ID
 * @param expireType   有效期类型（1-1天 7-7天 30-30天 0-永久），expireTime 为空时生效
 * @param expireTime   自定义过期时间（ISO-8601 格式，如 "2026-05-13T23:59:59"），优先于 expireType
 * @param shareType    是否需要提取码（1-需要 0-不需要），默认 1
 * @param extractCode  自定义提取码（4-6位字母数字），为空时服务端随机生成
 */
public record ShareCreateDTO(
        @NotNull Long userFileId,
        Integer expireType,
        String expireTime,
        Integer shareType,
        String extractCode
) {}
