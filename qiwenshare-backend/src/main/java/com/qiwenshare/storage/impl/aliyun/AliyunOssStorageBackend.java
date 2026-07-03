package com.qiwenshare.storage.impl.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.qiwenshare.storage.interfaces.StorageBackend;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 存储后端实现。
 *
 * <p>使用阿里云 OSS SDK 执行文件操作，OSS 客户端作为单例注入。
 * SDK 类型不泄露到 StorageBackend 接口层。</p>
 */
@Slf4j
public class AliyunOssStorageBackend implements StorageBackend {

    private final OSS ossClient;
    private final String bucket;
    private final long presignedUrlExpiry;

    public AliyunOssStorageBackend(OSS ossClient, String bucket, long presignedUrlExpiry) {
        this.ossClient = ossClient;
        this.bucket = bucket;
        this.presignedUrlExpiry = presignedUrlExpiry;
    }

    @Override
    public String getStorageType() {
        return "aliyun";
    }

    @Override
    public boolean checkConnectivity() {
        try {
            return ossClient.doesBucketExist(bucket);
        } catch (Exception e) {
            log.warn("Aliyun OSS 连通性检查失败", e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String storagePath, long fileSize) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (fileSize > 0) {
                metadata.setContentLength(fileSize);
            }
            ossClient.putObject(bucket, storagePath, inputStream, metadata);
            log.debug("Aliyun OSS 上传成功: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("Aliyun OSS 上传失败: {}", storagePath, e);
            throw new UncheckedIOException("Aliyun OSS 上传失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public InputStream download(String storagePath) {
        try {
            OSSObject ossObject = ossClient.getObject(bucket, storagePath);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("Aliyun OSS 下载失败: {}", storagePath, e);
            throw new UncheckedIOException("Aliyun OSS 下载失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public InputStream downloadRange(String storagePath, long start, long end) {
        try {
            com.aliyun.oss.model.GetObjectRequest request =
                    new com.aliyun.oss.model.GetObjectRequest(bucket, storagePath);
            request.setRange(start, end);
            OSSObject ossObject = ossClient.getObject(request);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("Aliyun OSS 断点下载失败: {} range=[{}-{}]", storagePath, start, end, e);
            throw new UncheckedIOException("Aliyun OSS 断点下载失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucket, storagePath);
            return metadata.getContentLength();
        } catch (Exception e) {
            throw new UncheckedIOException("Aliyun OSS 获取文件大小失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public void copy(String sourcePath, String destinationPath) {
        try {
            ossClient.copyObject(bucket, sourcePath, bucket, destinationPath);
            log.debug("Aliyun OSS 复制成功: {} -> {}", sourcePath, destinationPath);
        } catch (Exception e) {
            throw new UncheckedIOException("Aliyun OSS 复制失败: " + sourcePath + " -> " + destinationPath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            ossClient.deleteObject(bucket, storagePath);
            log.debug("Aliyun OSS 删除成功: {}", storagePath);
        } catch (Exception e) {
            throw new UncheckedIOException("Aliyun OSS 删除失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public String getPreviewUrl(String storagePath) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + presignedUrlExpiry * 1000);
            URL url = ossClient.generatePresignedUrl(bucket, storagePath, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("Aliyun OSS 生成预签名 URL 失败: {}", storagePath, e);
            return null;
        }
    }

    @Override
    public InputStream read(String storagePath) {
        return download(storagePath);
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            return ossClient.doesObjectExist(bucket, storagePath);
        } catch (Exception e) {
            throw new UncheckedIOException("Aliyun OSS 检查文件存在失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public String write(String storagePath, InputStream inputStream) {
        return upload(inputStream, storagePath, -1);
    }
}

