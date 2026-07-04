package com.qiwenshare.file.exception;

import lombok.Getter;

/**
 * 文件模块错误码枚举。
 *
 * <p>每个错误码包含 HTTP 状态码和业务消息。
 * Controller 和 Service 通过抛出 {@link FileModuleException} 携带此枚举，
 * 由全局异常处理器统一处理。</p>
 */
@Getter
public enum FileErrorCode {

    // 上传错误
    UPLOAD_SIZE_EXCEEDED(413, "文件大小超过限制"),
    UPLOAD_QUOTA_EXCEEDED(507, "用户存储配额不足"),
    UPLOAD_FORMAT_REJECTED(415, "文件格式不允许"),
    UPLOAD_AUTH_FAILED(401, "认证失败"),
    UPLOAD_STORAGE_ERROR(503, "存储后端写入失败"),
    UPLOAD_NETWORK_ERROR(502, "网络中断"),
    UPLOAD_CHUNK_MISMATCH(400, "分片序号或 hash 不匹配"),
    UPLOAD_DUPLICATE(409, "同名文件已存在"),
    UPLOAD_TASK_NOT_FOUND(404, "上传任务不存在"),
    UPLOAD_TASK_MERGE_FAILED(500, "分片合并失败"),

    // 下载错误
    FILE_NOT_FOUND(404, "文件不存在"),
    FILE_ACCESS_DENIED(403, "无权访问该文件"),

    // 文件操作错误
    FILE_NAME_DUPLICATE(409, "同名文件已存在"),
    FOLDER_NOT_FOUND(404, "文件夹不存在"),
    MOVE_TO_SELF(400, "不能将文件夹移动到自身或其子目录下"),
    FILE_UPDATE_FAILED(500, "文件内容修改失败"),
    TEMPLATE_LOAD_FAILED(500, "新建文件失败：模板文件缺失或读取失败"),

    // 回收站错误
    RECOVERY_CONFLICT(409, "原路径存在同名文件，无法恢复"),

    // 分享错误
    SHARE_NOT_FOUND(404, "分享链接不存在"),
    SHARE_EXPIRED(410, "分享链接已过期"),
    SHARE_EXTRACT_CODE_WRONG(401, "提取码错误"),

    // 配额错误
    QUOTA_NOT_FOUND(404, "配额记录不存在");

    private final int httpStatus;
    private final String message;

    FileErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
