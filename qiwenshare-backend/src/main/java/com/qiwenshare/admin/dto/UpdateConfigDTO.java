package com.qiwenshare.admin.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改系统参数请求体。
 *
 * @param configValue 参数值（可选）
 * @param description 参数描述（可选）
 */
public record UpdateConfigDTO(
        @Size(max = 500) String configValue,
        @Size(max = 200) String description
) {
}
