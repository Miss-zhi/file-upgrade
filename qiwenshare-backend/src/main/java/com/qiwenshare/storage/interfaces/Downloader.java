package com.qiwenshare.storage.interfaces;

import java.io.InputStream;

/**
 * 下载操作接口。
 */
public interface Downloader {

    /**
     * 获取文件输入流。
     *
     * @param storagePath 存储路径
     * @return 文件输入流（调用方负责关闭）
     */
    InputStream download(String storagePath);

    /**
     * 获取文件输入流（支持 Range 断点续传）。
     *
     * @param storagePath 存储路径
     * @param start       起始字节偏移
     * @param end         结束字节偏移（含）
     * @return 文件输入流（调用方负责关闭）
     */
    InputStream downloadRange(String storagePath, long start, long end);

    /**
     * 获取文件大小。
     *
     * @param storagePath 存储路径
     * @return 文件大小（字节）
     */
    long getFileSize(String storagePath);
}
