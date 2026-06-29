package com.qiwenshare.ufop.constant;

public enum FilePermissionEnum {
    DEFAULT(0, "默认"),
    PUBLIC(1, "公开"),
    PRIVATE(2, "私有");

    private final int code;
    private final String desc;

    FilePermissionEnum(int code, String desc) { this.code = code; this.desc = desc; }
    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
