package com.qiwenshare.admin.vo;

import java.time.LocalDateTime;

/**
 * 系统参数响应体。
 *
 * @param id          主键
 * @param configKey   参数键名
 * @param configValue 参数值
 * @param description 参数描述
 * @param createTime  创建时间
 * @param updateTime  更新时间
 */
public record ConfigVO(
        Long id,
        String configKey,
        String configValue,
        String description,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
