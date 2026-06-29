package com.qiwenshare.file;

import com.qiwenshare.ufop.UFOPFactory;
import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.*;
import com.qiwenshare.ufop.operation.copy.product.AliyunOSSCopier;
import com.qiwenshare.ufop.operation.copy.product.FastDFSCopier;
import com.qiwenshare.ufop.operation.copy.product.MinioCopier;
import com.qiwenshare.ufop.operation.copy.product.QiniuyunKodoCopier;
import com.qiwenshare.ufop.operation.delete.product.AliyunOSSDeleter;
import com.qiwenshare.ufop.operation.delete.product.FastDFSDeleter;
import com.qiwenshare.ufop.operation.delete.product.MinioDeleter;
import com.qiwenshare.ufop.operation.delete.product.QiniuyunKodoDeleter;
import com.qiwenshare.ufop.operation.download.product.AliyunOSSDownloader;
import com.qiwenshare.ufop.operation.download.product.FastDFSDownloader;
import com.qiwenshare.ufop.operation.download.product.MinioDownloader;
import com.qiwenshare.ufop.operation.download.product.QiniuyunKodoDownloader;
import com.qiwenshare.ufop.operation.read.product.AliyunOSSReader;
import com.qiwenshare.ufop.operation.read.product.FastDFSReader;
import com.qiwenshare.ufop.operation.read.product.MinioReader;
import com.qiwenshare.ufop.operation.read.product.QiniuyunKodoReader;
import com.qiwenshare.ufop.operation.rename.product.AliyunOSSRenamer;
import com.qiwenshare.ufop.operation.rename.product.FastDFSRenamer;
import com.qiwenshare.ufop.operation.rename.product.MinioRenamer;
import com.qiwenshare.ufop.operation.rename.product.QiniuyunKodoRenamer;
import com.qiwenshare.ufop.operation.upload.product.AliyunOSSUploader;
import com.qiwenshare.ufop.operation.upload.product.FastDFSUploader;
import com.qiwenshare.ufop.operation.upload.product.MinioUploader;
import com.qiwenshare.ufop.operation.upload.product.QiniuyunKodoUploader;
import com.qiwenshare.ufop.operation.write.product.AliyunOSSWriter;
import com.qiwenshare.ufop.operation.write.product.FastDFSWriter;
import com.qiwenshare.ufop.operation.write.product.MinioWriter;
import com.qiwenshare.ufop.operation.write.product.QiniuyunKodoWriter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UfopRemoteTest {

    @Autowired(required = false)
    private AliyunOSSUploader aliyunUploader;

    @Autowired(required = false)
    private MinioUploader minioUploader;

    @Autowired(required = false)
    private FastDFSUploader fastDFSUploader;

    @Autowired(required = false)
    private QiniuyunKodoUploader qiniuUploader;

    @Autowired(required = false)
    private AliyunOSSDownloader aliyunDownloader;

    @Autowired(required = false)
    private MinioDownloader minioDownloader;

    @Autowired(required = false)
    private AliyunOSSDeleter aliyunDeleter;

    @Autowired(required = false)
    private MinioDeleter minioDeleter;

    @Autowired(required = false)
    private AliyunOSSReader aliyunReader;

    @Autowired(required = false)
    private FastDFSReader fastDFSReader;

    @Autowired(required = false)
    private AliyunOSSWriter aliyunWriter;

    @Autowired(required = false)
    private MinioWriter minioWriter;

    @Autowired(required = false)
    private AliyunOSSCopier aliyunCopier;

    @Autowired(required = false)
    private FastDFSCopier fastDFSCopier;

    @Autowired(required = false)
    private AliyunOSSRenamer aliyunRenamer;

    @Autowired(required = false)
    private QiniuyunKodoRenamer qiniuRenamer;

    @Autowired
    private List<Uploader> allUploaders;

    @Autowired
    private List<Downloader> allDownloaders;

    @Autowired
    private List<Deleter> allDeleters;

    @Autowired
    private List<Reader> allReaders;

    @Autowired
    private List<Writer> allWriters;

    @Autowired
    private List<Copier> allCopiers;

    @Autowired
    private List<Renamer> allRenamers;

    @Autowired
    private UFOPFactory factory;

    @Test
    @DisplayName("所有 5 种存储的 Uploader 已注入")
    void testAllUploadersInjected() {
        assertEquals(5, allUploaders.size(), "应有 Local + 4 远程 = 5 个 Uploader");
    }

    @Test
    @DisplayName("所有 5 种存储的 Downloader 已注入")
    void testAllDownloadersInjected() {
        assertEquals(5, allDownloaders.size());
    }

    @Test
    @DisplayName("所有 5 种存储的 Deleter 已注入")
    void testAllDeletersInjected() {
        assertEquals(5, allDeleters.size());
    }

    @Test
    @DisplayName("所有 5 种存储的 Reader 已注入")
    void testAllReadersInjected() {
        assertEquals(5, allReaders.size());
    }

    @Test
    @DisplayName("所有 5 种存储的 Writer 已注入")
    void testAllWritersInjected() {
        assertEquals(5, allWriters.size());
    }

    @Test
    @DisplayName("所有 5 种存储的 Copier 已注入")
    void testAllCopiersInjected() {
        assertEquals(5, allCopiers.size());
    }

    @Test
    @DisplayName("所有 5 种存储的 Renamer 已注入")
    void testAllRenamersInjected() {
        assertEquals(5, allRenamers.size());
    }

    @Test
    @DisplayName("AliyunOSS Bean getStorageType")
    void testAliyunStorageType() {
        assertNotNull(aliyunUploader);
        assertEquals(StorageType.ALIYUN_OSS, aliyunUploader.getStorageType());
        assertEquals(StorageType.ALIYUN_OSS, aliyunDownloader.getStorageType());
        assertEquals(StorageType.ALIYUN_OSS, aliyunDeleter.getStorageType());
        assertEquals(StorageType.ALIYUN_OSS, aliyunReader.getStorageType());
        assertEquals(StorageType.ALIYUN_OSS, aliyunWriter.getStorageType());
        assertEquals(StorageType.ALIYUN_OSS, aliyunCopier.getStorageType());
        assertEquals(StorageType.ALIYUN_OSS, aliyunRenamer.getStorageType());
    }

    @Test
    @DisplayName("Minio Bean getStorageType")
    void testMinioStorageType() {
        assertEquals(StorageType.MINIO, minioUploader.getStorageType());
        assertEquals(StorageType.MINIO, minioDownloader.getStorageType());
        assertEquals(StorageType.MINIO, minioDeleter.getStorageType());
        assertEquals(StorageType.MINIO, minioWriter.getStorageType());
    }

    @Test
    @DisplayName("FastDFS Bean getStorageType")
    void testFastDFSStorageType() {
        assertEquals(StorageType.FAST_DFS, fastDFSUploader.getStorageType());
        assertEquals(StorageType.FAST_DFS, fastDFSReader.getStorageType());
        assertEquals(StorageType.FAST_DFS, fastDFSCopier.getStorageType());
    }

    @Test
    @DisplayName("Qiniuyun Bean getStorageType")
    void testQiniuStorageType() {
        assertEquals(StorageType.QINIU, qiniuUploader.getStorageType());
        assertEquals(StorageType.QINIU, qiniuRenamer.getStorageType());
    }

    @Test
    @DisplayName("UFOPFactory 默认返回本地存储")
    void testFactoryReturnsLocal() {
        assertNotNull(factory.getUploader());
    }
}
