package com.qiwenshare.storage.interfaces;

import java.io.InputStream;

/**
 * 写入操作接口（通用写入，不同于 Uploader 的文件上传）。
 */
public interface Writer {

    /**
     * 写入数据到存储后端。
     *
     * @param storagePath 存储路径
     * @param inputStream 数据输入流
     * @return 实际存储路径
     */
    String write(String storagePath, InputStream inputStream);
}
