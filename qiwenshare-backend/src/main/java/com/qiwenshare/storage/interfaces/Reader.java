package com.qiwenshare.storage.interfaces;

import java.io.InputStream;

/**
 * 读取操作接口（通用读取，不同于 Downloader 的流式下载）。
 */
public interface Reader {

    /**
     * 读取存储对象内容。
     *
     * @param storagePath 存储路径
     * @return 内容输入流（调用方负责关闭）
     */
    InputStream read(String storagePath);

    /**
     * 检查存储对象是否存在。
     *
     * @param storagePath 存储路径
     * @return 存在返回 true
     */
    boolean exists(String storagePath);
}
