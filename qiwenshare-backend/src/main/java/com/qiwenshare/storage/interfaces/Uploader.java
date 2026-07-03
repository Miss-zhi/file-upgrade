package com.qiwenshare.storage.interfaces;

import java.io.InputStream;

/**
 * 上传操作接口。
 */
public interface Uploader {

    /**
     * 上传文件到存储后端。
     *
     * @param inputStream 文件输入流
     * @param storagePath 存储路径
     * @param fileSize    文件大小（字节）
     * @return 实际存储路径
     */
    String upload(InputStream inputStream, String storagePath, long fileSize);
}
