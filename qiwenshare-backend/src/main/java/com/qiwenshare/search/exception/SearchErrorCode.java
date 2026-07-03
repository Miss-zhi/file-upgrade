package com.qiwenshare.search.exception;

import lombok.Getter;

/**
 * 搜索模块错误码枚举。
 *
 * <p>每个错误码包含 HTTP 状态码和业务消息。
 * 由全局异常处理器统一处理。</p>
 */
@Getter
public enum SearchErrorCode {

    /**
     * 搜索服务不可用（ES 连接失败或查询异常）。
     */
    SEARCH_UNAVAILABLE(503, "搜索服务暂不可用，请稍后再试"),

    /**
     * 索引写入失败。
     */
    INDEX_FAILED(500, "索引写入失败"),

    /**
     * 搜索关键词无效（空或超长）。
     */
    INVALID_KEYWORD(400, "搜索关键词无效"),

    /**
     * 全量重建失败。
     */
    REBUILD_FAILED(500, "全量重建索引失败");

    private final int httpStatus;
    private final String message;

    SearchErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
