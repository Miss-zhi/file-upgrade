package com.qiwenshare.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增系统参数请求体。
 *
 * @param configKey   参数键名
 * @param configValue 参数值
 * @param description 参数描述
 */
public record CreateConfigDTO(
        @NotBlank @Size(max = 100) String configKey,
        @NotBlank @Size(max = 500) String configValue,
        @Size(max = 200) String description
) {
}
