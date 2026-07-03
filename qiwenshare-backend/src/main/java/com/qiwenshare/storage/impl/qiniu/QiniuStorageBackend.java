package com.qiwenshare.storage.impl.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.util.Auth;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiwenshare.storage.interfaces.StorageBackend;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 七牛云 Kodo 存储后端实现。
 *
 * <p>使用七牛 SDK 执行文件操作，Auth/UploadManager/BucketManager 作为单例注入。</p>
 */
@Slf4j
public class QiniuStorageBackend implements StorageBackend {

    private final Auth auth;
    private final UploadManager uploadManager;
    private final BucketManager bucketManager;
    private final String bucket;
    private final String domain;
    private final long expireSeconds;

    public QiniuStorageBackend(Auth auth, UploadManager uploadManager, BucketManager bucketManager,
                               String bucket, String domain, long expireSeconds) {
        this.auth = auth;
        this.uploadManager = uploadManager;
        this.bucketManager = bucketManager;
        this.bucket = bucket;
        this.domain = domain;
        this.expireSeconds = expireSeconds;
    }

    @Override
    public String getStorageType() {
        return "qiniu";
    }

    @Override
    public boolean checkConnectivity() {
        try {
            bucketManager.buckets();
            return true;
        } catch (QiniuException e) {
            log.warn("Qiniu 连通性检查失败", e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String storagePath, long fileSize) {
        try {
            String upToken = auth.uploadToken(bucket);
            Response response = uploadManager.put(inputStream, storagePath, upToken, null, null);
            if (!response.isOK()) {
                throw new IOException("七牛上传响应异常: " + response.error);
            }
            log.debug("Qiniu 上传成功: {}", storagePath);
            return storagePath;
        } catch (QiniuException e) {
            log.error("Qiniu 上传失败: {}", storagePath, e);
            throw new UncheckedIOException("Qiniu 上传失败: " + storagePath, new IOException(e));
        } catch (IOException e) {
            log.error("Qiniu 上传失败: {}", storagePath, e);
            throw new UncheckedIOException("Qiniu 上传失败: " + storagePath, e);
        }
    }

    @Override
    public InputStream download(String storagePath) {
        try {
            String signedUrl = getSignedUrl(storagePath);
            URL url = new URL(signedUrl);
            return url.openStream();
        } catch (IOException e) {
            log.error("Qiniu 下载失败: {}", storagePath, e);
            throw new UncheckedIOException("Qiniu 下载失败: " + storagePath, e);
        }
    }

    @Override
    public InputStream downloadRange(String storagePath, long start, long end) {
        HttpURLConnection conn = null;
        try {
            String signedUrl = getSignedUrl(storagePath);
            URL url = new URL(signedUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            conn.setRequestMethod("GET");
            byte[] data = readAllBytes(conn.getInputStream());
            return new ByteArrayInputStream(data);
        } catch (IOException e) {
            log.error("Qiniu 断点下载失败: {} range=[{}-{}]", storagePath, start, end, e);
            throw new UncheckedIOException("Qiniu 断点下载失败: " + storagePath, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        try {
            FileInfo fileInfo = bucketManager.stat(bucket, storagePath);
            return fileInfo.fsize;
        } catch (QiniuException e) {
            throw new UncheckedIOException("Qiniu 获取文件大小失败: " + storagePath, new IOException(e));
        }
    }

    @Override
    public void copy(String sourcePath, String destinationPath) {
        try {
            bucketManager.copy(bucket, sourcePath, bucket, destinationPath, true);
            log.debug("Qiniu 复制成功: {} -> {}", sourcePath, destinationPath);
        } catch (QiniuException e) {
            throw new UncheckedIOException("Qiniu 复制失败: " + sourcePath + " -> " + destinationPath, new IOException(e));
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            bucketManager.delete(bucket, storagePath);
            log.debug("Qiniu 删除成功: {}", storagePath);
        } catch (QiniuException e) {
            throw new UncheckedIOException("Qiniu 删除失败: " + storagePath, new IOException(e));
        }
    }

    @Override
    public String getPreviewUrl(String storagePath) {
        try {
            return auth.privateDownloadUrl(domain + "/" + storagePath, expireSeconds);
        } catch (Exception e) {
            log.error("Qiniu 生成预览 URL 失败: {}", storagePath, e);
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
            bucketManager.stat(bucket, storagePath);
            return true;
        } catch (QiniuException e) {
            if (e.code() == 612) {
                return false;
            }
            throw new UncheckedIOException("Qiniu 检查文件存在失败: " + storagePath, new IOException(e));
        }
    }

    @Override
    public String write(String storagePath, InputStream inputStream) {
        return upload(inputStream, storagePath, -1);
    }

    /**
     * 构建带签名的下载 URL。
     */
    private String getSignedUrl(String storagePath) {
        return auth.privateDownloadUrl(domain + "/" + storagePath, expireSeconds);
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
}
