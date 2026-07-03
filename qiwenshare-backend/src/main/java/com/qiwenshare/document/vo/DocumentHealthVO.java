package com.qiwenshare.document.vo;

/**
 * OnlyOffice 健康检查 VO。
 *
 * @param status    状态（UP / DOWN）
 * @param serverUrl OnlyOffice Document Server 地址
 * @param error     错误信息（status=DOWN 时有值）
 */
public record DocumentHealthVO(
        String status,
        String serverUrl,
        String error
) {
}
