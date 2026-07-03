package com.qiwenshare.storage.impl.local;

import com.qiwenshare.storage.config.StorageProperties;
import com.qiwenshare.storage.interfaces.StorageBackend;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储后端实现。
 *
 * <p>文件存储在本地文件系统中，按日期分目录组织。</p>
 */
@Slf4j
public class LocalStorageBackend implements StorageBackend {

    private final String basePath;

    public LocalStorageBackend(StorageProperties.Local localProperties) {
        this.basePath = localProperties.getBasePath();
        // 确保根目录存在
        try {
            Files.createDirectories(Paths.get(basePath));
        } catch (IOException e) {
            log.error("无法创建本地存储根目录: {}", basePath, e);
            throw new UncheckedIOException("无法创建本地存储根目录: " + basePath, e);
        }
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    public boolean checkConnectivity() {
        Path testDir = Paths.get(basePath);
        return Files.exists(testDir) && Files.isWritable(testDir);
    }

    @Override
    public String upload(InputStream inputStream, String storagePath, long fileSize) {
        Path targetPath = resolvePath(storagePath);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("本地上传成功: {}", targetPath);
            return storagePath;
        } catch (IOException e) {
            log.error("本地上传失败: {}", storagePath, e);
            throw new UncheckedIOException("本地上传失败: " + storagePath, e);
        }
    }

    @Override
    public InputStream download(String storagePath) {
        Path targetPath = resolvePath(storagePath);
        try {
            return new BufferedInputStream(Files.newInputStream(targetPath));
        } catch (IOException e) {
            log.error("本地下载失败: {}", storagePath, e);
            throw new UncheckedIOException("本地下载失败: " + storagePath, e);
        }
    }

    @Override
    public InputStream downloadRange(String storagePath, long start, long end) {
        Path targetPath = resolvePath(storagePath);
        try (RandomAccessFile raf = new RandomAccessFile(targetPath.toFile(), "r")) {
            raf.seek(start);
            long length = end - start + 1;
            byte[] data = new byte[(int) length];
            raf.readFully(data);
            return new ByteArrayInputStream(data);
        } catch (IOException e) {
            log.error("本地断点下载失败: {} range=[{}-{}]", storagePath, start, end, e);
            throw new UncheckedIOException("本地断点下载失败: " + storagePath, e);
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        Path targetPath = resolvePath(storagePath);
        try {
            return Files.size(targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException("获取文件大小失败: " + storagePath, e);
        }
    }

    @Override
    public void copy(String sourcePath, String destinationPath) {
        Path source = resolvePath(sourcePath);
        Path dest = resolvePath(destinationPath);
        try {
            Files.createDirectories(dest.getParent());
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("本地复制失败: " + sourcePath + " -> " + destinationPath, e);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path targetPath = resolvePath(storagePath);
        try {
            Files.deleteIfExists(targetPath);
            log.debug("本地删除成功: {}", targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException("本地删除失败: " + storagePath, e);
        }
    }

    @Override
    public String getPreviewUrl(String storagePath) {
        // 本地存储不支持预览 URL，返回 null
        return null;
    }

    @Override
    public InputStream read(String storagePath) {
        return download(storagePath);
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(resolvePath(storagePath));
    }

    @Override
    public String write(String storagePath, InputStream inputStream) {
        return upload(inputStream, storagePath, -1);
    }

    private Path resolvePath(String storagePath) {
        Path basePathResolved = Paths.get(this.basePath).toAbsolutePath().normalize();
        Path resolved = basePathResolved.resolve(storagePath).normalize();
        if (!resolved.startsWith(basePathResolved)) {
            throw new SecurityException("路径遍历攻击被拦截，非法路径: " + storagePath);
        }
        return resolved;
    }
}
