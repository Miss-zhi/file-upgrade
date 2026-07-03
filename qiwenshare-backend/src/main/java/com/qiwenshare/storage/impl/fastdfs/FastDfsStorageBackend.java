package com.qiwenshare.storage.impl.fastdfs;

import com.github.tobato.fastdfs.domain.FileInfo;
import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.exception.FdfsServerException;
import com.github.tobato.fastdfs.proto.storage.DownloadCallback;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.qiwenshare.storage.interfaces.StorageBackend;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * FastDFS 存储后端实现。
 *
 * <p>使用 tobato FastDFS 客户端执行文件操作。
 * storagePath 格式为 {@code group/path}（如 {@code group1/M00/00/01/xxx.jpg}）。</p>
 */
@Slf4j
public class FastDfsStorageBackend implements StorageBackend {

    private static final long RANGE_WARN_THRESHOLD = 50L * 1024 * 1024; // 50MB

    private final FastFileStorageClient storageClient;
    private final String defaultGroup;

    public FastDfsStorageBackend(FastFileStorageClient storageClient, String defaultGroup) {
        this.storageClient = storageClient;
        this.defaultGroup = defaultGroup;
    }

    @Override
    public String getStorageType() {
        return "fastdfs";
    }

    @Override
    public boolean checkConnectivity() {
        try {
            // 通过查询一个不存在的文件来验证 FastDFS tracker/storage 可访问
            storageClient.queryFileInfo(defaultGroup, "__connectivity_check__");
            return true;
        } catch (FdfsServerException e) {
            // 文件不存在但服务器可达，说明连通性正常
            return true;
        } catch (Exception e) {
            log.warn("FastDFS 连通性检查失败", e);
            return false;
        }
    }

    @Override
    public String upload(InputStream inputStream, String storagePath, long fileSize) {
        try {
            byte[] bytes = readAllBytes(inputStream);
            String ext = getExtension(storagePath);
            StorePath storePath = storageClient.uploadFile(
                    new ByteArrayInputStream(bytes), bytes.length, ext, null);
            String resultPath = storePath.getFullPath(); // group/path
            log.debug("FastDFS 上传成功: {}", resultPath);
            return resultPath;
        } catch (IOException e) {
            log.error("FastDFS 上传失败: {}", storagePath, e);
            throw new UncheckedIOException("FastDFS 上传失败: " + storagePath, e);
        }
    }

    @Override
    public InputStream download(String storagePath) {
        try {
            String[] parsed = parseStoragePath(storagePath);
            String group = parsed[0];
            String path = parsed[1];
            byte[] data = storageClient.downloadFile(group, path, new DownloadByteArray());
            return new ByteArrayInputStream(data);
        } catch (Exception e) {
            log.error("FastDFS 下载失败: {}", storagePath, e);
            throw new UncheckedIOException("FastDFS 下载失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public InputStream downloadRange(String storagePath, long start, long end) {
        try {
            String[] parsed = parseStoragePath(storagePath);
            long fileSize = storageClient.queryFileInfo(parsed[0], parsed[1]).getFileSize();
            if (fileSize > RANGE_WARN_THRESHOLD) {
                log.warn("FastDFS 不支持原生 range 请求，当前文件大小 {} bytes（>{}MB），需全量下载后截取，建议调用方避免 range 请求: {}",
                        fileSize, RANGE_WARN_THRESHOLD / (1024 * 1024), storagePath);
            }
            // FastDFS 无原生 range API，下载全量后截取
            byte[] fullData = readAllBytes(download(storagePath));
            int startIndex = (int) start;
            int length = (int) (end - start + 1);
            byte[] rangeData = new byte[length];
            System.arraycopy(fullData, startIndex, rangeData, 0, length);
            return new ByteArrayInputStream(rangeData);
        } catch (IOException e) {
            log.error("FastDFS 断点下载失败: {} range=[{}-{}]", storagePath, start, end, e);
            throw new UncheckedIOException("FastDFS 断点下载失败: " + storagePath, e);
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        try {
            String[] parsed = parseStoragePath(storagePath);
            return storageClient.queryFileInfo(parsed[0], parsed[1]).getFileSize();
        } catch (Exception e) {
            throw new UncheckedIOException("FastDFS 获取文件大小失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public void copy(String sourcePath, String destinationPath) {
        try {
            // FastDFS 无服务端 copy，下载源文件后重新上传
            byte[] data = readAllBytes(download(sourcePath));
            String ext = getExtension(destinationPath);
            storageClient.uploadFile(
                    new ByteArrayInputStream(data), data.length, ext, null);
            log.debug("FastDFS 复制成功: {} -> {}", sourcePath, destinationPath);
        } catch (IOException e) {
            throw new UncheckedIOException("FastDFS 复制失败: " + sourcePath + " -> " + destinationPath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            String[] parsed = parseStoragePath(storagePath);
            storageClient.deleteFile(parsed[0], parsed[1]);
            log.debug("FastDFS 删除成功: {}", storagePath);
        } catch (Exception e) {
            throw new UncheckedIOException("FastDFS 删除失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public String getPreviewUrl(String storagePath) {
        // FastDFS 无内置 HTTP 预览，需配合 Nginx
        return null;
    }

    @Override
    public InputStream read(String storagePath) {
        return download(storagePath);
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            String[] parsed = parseStoragePath(storagePath);
            storageClient.queryFileInfo(parsed[0], parsed[1]);
            return true;
        } catch (FdfsServerException e) {
            return false;
        } catch (Exception e) {
            throw new UncheckedIOException("FastDFS 检查文件存在失败: " + storagePath,
                    e instanceof IOException ? (IOException) e : new IOException(e));
        }
    }

    @Override
    public String write(String storagePath, InputStream inputStream) {
        return upload(inputStream, storagePath, -1);
    }

    /**
     * 解析 storagePath 为 [group, path]。
     * 格式示例：group1/M00/00/01/xxx.jpg
     */
    String[] parseStoragePath(String storagePath) {
        int slashIndex = storagePath.indexOf('/');
        if (slashIndex <= 0) {
            return new String[]{defaultGroup, storagePath};
        }
        String group = storagePath.substring(0, slashIndex);
        String path = storagePath.substring(slashIndex + 1);
        return new String[]{group, path};
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

    private String getExtension(String path) {
        int dotIndex = path.lastIndexOf('.');
        return dotIndex >= 0 ? path.substring(dotIndex + 1) : "";
    }

    /**
     * FastDFS 下载回调，返回 byte[]。
     */
    private static class DownloadByteArray implements DownloadCallback<byte[]> {
        @Override
        public byte[] recv(InputStream inputStream) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
}

