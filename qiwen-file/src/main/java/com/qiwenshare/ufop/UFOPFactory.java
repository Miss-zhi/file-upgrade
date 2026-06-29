package com.qiwenshare.ufop;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.*;
import com.qiwenshare.ufop.operation.preview.*;
import com.qiwenshare.ufop.operation.preview.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UFOP 工厂 — 根据配置返回存储实现
 */
@Component
@RequiredArgsConstructor
public class UFOPFactory {

    private final UFOPConfigProperties config;
    private final LocalStorageUploader uploader;
    private final LocalStorageDownloader downloader;
    private final LocalStorageDeleter deleter;
    private final LocalStorageReader reader;
    private final LocalStorageWriter writer;
    private final LocalStorageRenamer renamer;
    private final LocalStorageCopier copier;
    private final LocalStoragePreviewer previewer;

    public Uploader getUploader() { return uploader; }
    public Downloader getDownloader() { return downloader; }
    public Deleter getDeleter() { return deleter; }
    public Reader getReader() { return reader; }
    public Writer getWriter() { return writer; }
    public Renamer getRenamer() { return renamer; }
    public Copier getCopier() { return copier; }
    public Previewer getPreviewer() { return previewer; }
}
