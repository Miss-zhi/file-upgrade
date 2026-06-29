package com.qiwenshare.ufop;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.*;
import com.qiwenshare.ufop.operation.copy.product.*;
import com.qiwenshare.ufop.operation.delete.product.*;
import com.qiwenshare.ufop.operation.download.product.*;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.operation.preview.product.*;
import com.qiwenshare.ufop.operation.read.product.*;
import com.qiwenshare.ufop.operation.rename.product.*;
import com.qiwenshare.ufop.operation.upload.product.*;
import com.qiwenshare.ufop.operation.write.product.*;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * UFOP 工厂 — 根据配置的存储类型返回对应的操作实现
 * <p>
 * 注入全部 5 种存储（LOCAL / ALIYUN_OSS / FAST_DFS / MINIO / QINIU）× 8 种操作，
 * 通过 {@link StorageType} 路由到正确的实现。
 */
@Component
public class UFOPFactory {

    private final UFOPConfigProperties config;

    // ===== 操作 → 存储类型 → 实现 映射 =====
    private final Map<StorageType, Uploader> uploaders = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Downloader> downloaders = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Deleter> deleters = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Reader> readers = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Writer> writers = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Renamer> renamers = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Copier> copiers = new EnumMap<>(StorageType.class);
    private final Map<StorageType, Previewer> previewers = new EnumMap<>(StorageType.class);

    public UFOPFactory(UFOPConfigProperties config,
                       // ===== Uploader =====
                       LocalStorageUploader localStorageUploader,
                       AliyunOSSUploader aliyunOSSUploader,
                       FastDFSUploader fastDFSUploader,
                       MinioUploader minioUploader,
                       QiniuyunKodoUploader qiniuyunKodoUploader,
                       // ===== Downloader =====
                       LocalStorageDownloader localStorageDownloader,
                       AliyunOSSDownloader aliyunOSSDownloader,
                       FastDFSDownloader fastDFSDownloader,
                       MinioDownloader minioDownloader,
                       QiniuyunKodoDownloader qiniuyunKodoDownloader,
                       // ===== Deleter =====
                       LocalStorageDeleter localStorageDeleter,
                       AliyunOSSDeleter aliyunOSSDeleter,
                       FastDFSDeleter fastDFSDeleter,
                       MinioDeleter minioDeleter,
                       QiniuyunKodoDeleter qiniuyunKodoDeleter,
                       // ===== Reader =====
                       LocalStorageReader localStorageReader,
                       AliyunOSSReader aliyunOSSReader,
                       FastDFSReader fastDFSReader,
                       MinioReader minioReader,
                       QiniuyunKodoReader qiniuyunKodoReader,
                       // ===== Writer =====
                       LocalStorageWriter localStorageWriter,
                       AliyunOSSWriter aliyunOSSWriter,
                       FastDFSWriter fastDFSWriter,
                       MinioWriter minioWriter,
                       QiniuyunKodoWriter qiniuyunKodoWriter,
                       // ===== Renamer =====
                       LocalStorageRenamer localStorageRenamer,
                       AliyunOSSRenamer aliyunOSSRenamer,
                       FastDFSRenamer fastDFSRenamer,
                       MinioRenamer minioRenamer,
                       QiniuyunKodoRenamer qiniuyunKodoRenamer,
                       // ===== Copier =====
                       LocalStorageCopier localStorageCopier,
                       AliyunOSSCopier aliyunOSSCopier,
                       FastDFSCopier fastDFSCopier,
                       MinioCopier minioCopier,
                       QiniuyunKodoCopier qiniuyunKodoCopier,
                       // ===== Previewer =====
                       LocalStoragePreviewer localStoragePreviewer,
                       AliyunOSSPreviewer aliyunOSSPreviewer,
                       FastDFSPreviewer fastDFSPreviewer,
                       MinioPreviewer minioPreviewer,
                       QiniuyunKodoPreviewer qiniuyunKodoPreviewer) {
        this.config = config;

        // 注册 Uploader
        uploaders.put(StorageType.LOCAL, localStorageUploader);
        uploaders.put(StorageType.ALIYUN_OSS, aliyunOSSUploader);
        uploaders.put(StorageType.FAST_DFS, fastDFSUploader);
        uploaders.put(StorageType.MINIO, minioUploader);
        uploaders.put(StorageType.QINIU, qiniuyunKodoUploader);

        // 注册 Downloader
        downloaders.put(StorageType.LOCAL, localStorageDownloader);
        downloaders.put(StorageType.ALIYUN_OSS, aliyunOSSDownloader);
        downloaders.put(StorageType.FAST_DFS, fastDFSDownloader);
        downloaders.put(StorageType.MINIO, minioDownloader);
        downloaders.put(StorageType.QINIU, qiniuyunKodoDownloader);

        // 注册 Deleter
        deleters.put(StorageType.LOCAL, localStorageDeleter);
        deleters.put(StorageType.ALIYUN_OSS, aliyunOSSDeleter);
        deleters.put(StorageType.FAST_DFS, fastDFSDeleter);
        deleters.put(StorageType.MINIO, minioDeleter);
        deleters.put(StorageType.QINIU, qiniuyunKodoDeleter);

        // 注册 Reader
        readers.put(StorageType.LOCAL, localStorageReader);
        readers.put(StorageType.ALIYUN_OSS, aliyunOSSReader);
        readers.put(StorageType.FAST_DFS, fastDFSReader);
        readers.put(StorageType.MINIO, minioReader);
        readers.put(StorageType.QINIU, qiniuyunKodoReader);

        // 注册 Writer
        writers.put(StorageType.LOCAL, localStorageWriter);
        writers.put(StorageType.ALIYUN_OSS, aliyunOSSWriter);
        writers.put(StorageType.FAST_DFS, fastDFSWriter);
        writers.put(StorageType.MINIO, minioWriter);
        writers.put(StorageType.QINIU, qiniuyunKodoWriter);

        // 注册 Renamer
        renamers.put(StorageType.LOCAL, localStorageRenamer);
        renamers.put(StorageType.ALIYUN_OSS, aliyunOSSRenamer);
        renamers.put(StorageType.FAST_DFS, fastDFSRenamer);
        renamers.put(StorageType.MINIO, minioRenamer);
        renamers.put(StorageType.QINIU, qiniuyunKodoRenamer);

        // 注册 Copier
        copiers.put(StorageType.LOCAL, localStorageCopier);
        copiers.put(StorageType.ALIYUN_OSS, aliyunOSSCopier);
        copiers.put(StorageType.FAST_DFS, fastDFSCopier);
        copiers.put(StorageType.MINIO, minioCopier);
        copiers.put(StorageType.QINIU, qiniuyunKodoCopier);

        // 注册 Previewer
        previewers.put(StorageType.LOCAL, localStoragePreviewer);
        previewers.put(StorageType.ALIYUN_OSS, aliyunOSSPreviewer);
        previewers.put(StorageType.FAST_DFS, fastDFSPreviewer);
        previewers.put(StorageType.MINIO, minioPreviewer);
        previewers.put(StorageType.QINIU, qiniuyunKodoPreviewer);
    }

    /** 返回当前配置存储对应的 Uploader，默认 LOCAL */
    public Uploader getUploader() {
        return uploaders.getOrDefault(config.getStorageType(), uploaders.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Uploader */
    public Uploader getUploader(StorageType type) {
        return uploaders.getOrDefault(type, uploaders.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Downloader */
    public Downloader getDownloader() {
        return downloaders.getOrDefault(config.getStorageType(), downloaders.get(StorageType.LOCAL));
    }

    public Downloader getDownloader(StorageType type) {
        return downloaders.getOrDefault(type, downloaders.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Deleter */
    public Deleter getDeleter() {
        return deleters.getOrDefault(config.getStorageType(), deleters.get(StorageType.LOCAL));
    }

    public Deleter getDeleter(StorageType type) {
        return deleters.getOrDefault(type, deleters.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Reader */
    public Reader getReader() {
        return readers.getOrDefault(config.getStorageType(), readers.get(StorageType.LOCAL));
    }

    public Reader getReader(StorageType type) {
        return readers.getOrDefault(type, readers.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Writer */
    public Writer getWriter() {
        return writers.getOrDefault(config.getStorageType(), writers.get(StorageType.LOCAL));
    }

    public Writer getWriter(StorageType type) {
        return writers.getOrDefault(type, writers.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Renamer */
    public Renamer getRenamer() {
        return renamers.getOrDefault(config.getStorageType(), renamers.get(StorageType.LOCAL));
    }

    public Renamer getRenamer(StorageType type) {
        return renamers.getOrDefault(type, renamers.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Copier */
    public Copier getCopier() {
        return copiers.getOrDefault(config.getStorageType(), copiers.get(StorageType.LOCAL));
    }

    public Copier getCopier(StorageType type) {
        return copiers.getOrDefault(type, copiers.get(StorageType.LOCAL));
    }

    /** 返回当前配置存储对应的 Previewer */
    public Previewer getPreviewer() {
        return previewers.getOrDefault(config.getStorageType(), previewers.get(StorageType.LOCAL));
    }

    public Previewer getPreviewer(StorageType type) {
        return previewers.getOrDefault(type, previewers.get(StorageType.LOCAL));
    }
}
