package com.qiwenshare.storage.interfaces;

/**
 * 预览操作接口。
 */
public interface Previewer {

    /**
     * 获取文件预览 URL 或流。
     *
     * @param storagePath 存储路径
     * @return 预览 URL 或预览流标识
     */
    String getPreviewUrl(String storagePath);
}
