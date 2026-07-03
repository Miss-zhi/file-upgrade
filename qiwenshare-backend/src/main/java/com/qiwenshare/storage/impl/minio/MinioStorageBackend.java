package com.qiwenshare.storage.impl.minio;

import com.qiwenshare.storage.interfaces.StorageBackend;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * MinIO S3 兼容存储后端实现。
 *
 * <p>使用 MinIO Java SDK 执行文件操作，MinioClient 作为单例注入。</p>
 */
@Slf4j
public class MinioStorageBackend implements StorageBackend {

    private final MinioClient minioClient;
    private final String bucket;
    private final long presignedUrlExpiry;

    public MinioStorageBackend(MinioClient minioClient, String bucket, long presignedUrlExpiry) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.presignedUrlExpiry = presignedUrlExpiry;
    }

    @Override
    public String getStorageType() {
        return "minio";
    }

    @Override
    public boolean checkConnectivity() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            log.warn("MinIO 连通性检查失败", e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String storagePath, long fileSize) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .stream(inputStream, fileSize, fileSize >= 0 ? -1 : 5 * 1024 * 1024)
                    .build());
            log.debug("MinIO 上传成功: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("MinIO 上传失败: {}", storagePath, e);
            throw new UncheckedIOException("MinIO 上传失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public InputStream download(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 下载失败: {}", storagePath, e);
            throw new UncheckedIOException("MinIO 下载失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public InputStream downloadRange(String storagePath, long start, long end) {
        try {
            long length = end - start + 1;
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .offset(start)
                    .length(length)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 断点下载失败: {} range=[{}-{}]", storagePath, start, end, e);
            throw new UncheckedIOException("MinIO 断点下载失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build()).size();
        } catch (Exception e) {
            throw new UncheckedIOException("MinIO 获取文件大小失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public void copy(String sourcePath, String destinationPath) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .source(CopySource.builder()
                            .bucket(bucket)
                            .object(sourcePath)
                            .build())
                    .object(destinationPath)
                    .build());
            log.debug("MinIO 复制成功: {} -> {}", sourcePath, destinationPath);
        } catch (Exception e) {
            throw new UncheckedIOException("MinIO 复制失败: " + sourcePath + " -> " + destinationPath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            log.debug("MinIO 删除成功: {}", storagePath);
        } catch (Exception e) {
            throw new UncheckedIOException("MinIO 删除失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public String getPreviewUrl(String storagePath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(storagePath)
                    .expiry((int) presignedUrlExpiry, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("MinIO 生成预签名 URL 失败: {}", storagePath, e);
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
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new UncheckedIOException("MinIO 检查文件存在失败: " + storagePath, new IOException(e));
        } catch (Exception e) {
            throw new UncheckedIOException("MinIO 检查文件存在失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public String write(String storagePath, InputStream inputStream) {
        return upload(inputStream, storagePath, -1);
    }
}

