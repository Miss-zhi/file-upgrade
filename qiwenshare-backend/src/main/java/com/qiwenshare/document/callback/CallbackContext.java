package com.qiwenshare.document.callback;

import com.qiwenshare.document.dto.CallbackBodyDTO;

/**
 * 回调处理上下文。
 *
 * <p>封装回调请求体和解析出的业务参数。</p>
 */
public class CallbackContext {

    private final CallbackBodyDTO body;
    private final Long userFileId;
    private final Long userId;

    /** 处理结果（error=0 表示成功） */
    private int errorCode;

    public CallbackContext(CallbackBodyDTO body, Long userFileId, Long userId) {
        this.body = body;
        this.userFileId = userFileId;
        this.userId = userId;
        this.errorCode = 0;
    }

    public CallbackBodyDTO getBody() { return body; }
    public Long getUserFileId() { return userFileId; }
    public Long getUserId() { return userId; }

    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

    /**
     * 标记处理失败。
     */
    public void markError() {
        this.errorCode = 1;
    }
}
