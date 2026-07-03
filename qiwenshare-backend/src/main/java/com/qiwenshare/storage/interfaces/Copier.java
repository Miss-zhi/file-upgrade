package com.qiwenshare.storage.interfaces;

/**
 * 复制操作接口。
 */
public interface Copier {

    /**
     * 在存储后端内复制文件。
     *
     * @param sourcePath      源路径
     * @param destinationPath 目标路径
     */
    void copy(String sourcePath, String destinationPath);
}
