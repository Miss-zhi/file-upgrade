package com.qiwenshare.document.vo;

/**
 * OnlyOffice 编辑配置 VO。
 *
 * <p>继承预览配置，扩展编辑模式特有的权限字段。</p>
 */
public class EditConfigVO extends PreviewConfigVO {

    /** 是否执行了 COW 操作 */
    private boolean cowApplied;

    /** 是否经过格式转换 */
    private boolean convertApplied;

    /** 转换后的新扩展名（仅 convertApplied=true 时有值） */
    private String convertedExtension;

    public boolean isCowApplied() { return cowApplied; }
    public void setCowApplied(boolean cowApplied) { this.cowApplied = cowApplied; }

    public boolean isConvertApplied() { return convertApplied; }
    public void setConvertApplied(boolean convertApplied) { this.convertApplied = convertApplied; }

    public String getConvertedExtension() { return convertedExtension; }
    public void setConvertedExtension(String convertedExtension) { this.convertedExtension = convertedExtension; }
}
