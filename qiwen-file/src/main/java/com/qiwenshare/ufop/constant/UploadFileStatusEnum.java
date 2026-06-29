package com.qiwenshare.ufop.constant;

public enum UploadFileStatusEnum {
    UPLOADING(0, "上传中"),
    SUCCESS(1, "成功"),
    FAIL(2, "失败");

    private final int code;
    private final String desc;

    UploadFileStatusEnum(int code, String desc) { this.code = code; this.desc = desc; }
    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
