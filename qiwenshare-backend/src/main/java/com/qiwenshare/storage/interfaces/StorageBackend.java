package com.qiwenshare.storage.interfaces;

/**
 * 存储后端组合接口，继承全部 7 个操作接口。
 *
 * <p>每种存储后端实现（Local/MinIO/AliyunOSS/Qiniu/FastDFS）
 * MUST 实现此接口。</p>
 */
public interface StorageBackend extends Uploader, Downloader, Copier, Deleter, Previewer, Reader, Writer {

    /**
     * 获取存储后端类型标识。
     *
     * @return 存储类型字符串（如 "local"、"minio"、"aliyun"、"qiniu"、"fastdfs"）
     */
    String getStorageType();

    /**
     * 检查存储后端连通性。
     *
     * @return 连通返回 true
     */
    boolean checkConnectivity();
}
