package com.qiwenshare.document.exception;

import lombok.Getter;

/**
 * 文档模块错误码枚举。
 *
 * <p>每个错误码包含 HTTP 状态码和业务消息。
 * 由全局异常处理器统一处理。</p>
 */
@Getter
public enum DocumentErrorCode {

    /** 无权限访问文档 */
    DOC_ACCESS_DENIED(403, "无权访问该文档"),

    /** 文件记录不存在或无关联文件（如文件夹） */
    DOC_FILE_NOT_FOUND(404, "文件不存在或不可预览"),

    /** 文件大小超过 OnlyOffice 限制 */
    DOC_FILE_TOO_LARGE(413, "文件大小超过文档服务限制"),

    /** 格式转换失败 */
    DOC_CONVERT_FAILED(500, "文档格式转换失败"),

    /** 版本不存在 */
    DOC_VERSION_NOT_FOUND(404, "文档版本不存在"),

    /** OnlyOffice 服务不可用 */
    DOC_SERVER_UNAVAILABLE(503, "文档服务暂不可用，请稍后再试"),

    /** 回调鉴权失败 */
    DOC_CALLBACK_AUTH_FAILED(403, "回调鉴权失败"),

    /** 预览下载基础 URL 未配置 */
    DOC_PREVIEW_NOT_CONFIGURED(500, "文档预览服务配置不完整，请联系管理员");

    private final int httpStatus;
    private final String message;

    DocumentErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
