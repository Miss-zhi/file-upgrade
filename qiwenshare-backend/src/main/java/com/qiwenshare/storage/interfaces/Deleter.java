package com.qiwenshare.storage.interfaces;

/**
 * 删除操作接口。
 */
public interface Deleter {

    /**
     * 删除存储后端中的文件。
     *
     * @param storagePath 存储路径
     */
    void delete(String storagePath);
}
