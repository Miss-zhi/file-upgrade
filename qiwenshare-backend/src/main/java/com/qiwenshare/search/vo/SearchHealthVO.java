package com.qiwenshare.search.vo;

/**
 * 搜索健康检查 VO。
 *
 * @param available ES 是否可用
 * @param status    状态描述
 */
public record SearchHealthVO(
        boolean available,
        String status
) {
}
