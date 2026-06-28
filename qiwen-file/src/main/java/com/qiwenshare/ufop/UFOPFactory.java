package com.qiwenshare.ufop;

import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.*;
import com.qiwenshare.ufop.exception.UFOPException;

/**
 * UFOP 工厂 — 统一文件操作提供者
 * 根据存储类型返回对应的操作实现
 */
public class UFOPFactory {

    private final StorageType storageType;

    public UFOPFactory(StorageType storageType) {
        this.storageType = storageType;
    }

    /**
     * 获取当前存储类型的工厂实例
     */
    public static UFOPFactory getInstance(StorageType storageType) {
        return new UFOPFactory(storageType);
    }

    /**
     * 获取上传器
     */
    public Uploader getUploader() {
        throw new UFOPException("上传操作尚未实现: " + storageType);
    }

    /**
     * 获取下载器
     */
    public Downloader getDownloader() {
        throw new UFOPException("下载操作尚未实现: " + storageType);
    }

    /**
     * 获取删除器
     */
    public Deleter getDeleter() {
        throw new UFOPException("删除操作尚未实现: " + storageType);
    }

    /**
     * 获取读取器
     */
    public Reader getReader() {
        throw new UFOPException("读取操作尚未实现: " + storageType);
    }

    /**
     * 获取写入器
     */
    public Writer getWriter() {
        throw new UFOPException("写入操作尚未实现: " + storageType);
    }

    /**
     * 获取重命名器
     */
    public Renamer getRenamer() {
        throw new UFOPException("重命名操作尚未实现: " + storageType);
    }

    /**
     * 获取复制器
     */
    public Copier getCopier() {
        throw new UFOPException("复制操作尚未实现: " + storageType);
    }
}
