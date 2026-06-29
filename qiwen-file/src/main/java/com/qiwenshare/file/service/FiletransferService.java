package com.qiwenshare.file.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IFiletransferService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.task.UploadTask;
import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.mapper.UploadTaskMapper;
import com.qiwenshare.ufop.config.UFOPConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiletransferService implements IFiletransferService {

    private final UploadTaskMapper taskMapper;
    private final IFileService fileService;
    private final UFOPConfigProperties ufopConfig;

    private Path getChunkDir(String identifier) {
        return Paths.get(ufopConfig.getRootPath(), "chunks", identifier);
    }

    @Override
    public void uploadChunk(String identifier, int chunkNum, int totalChunks,
            String fileName, String filePath, long totalSize,
            String userId, InputStream chunkStream) {
        try {
            Path chunkDir = getChunkDir(identifier);
            Files.createDirectories(chunkDir);
            Files.copy(chunkStream, chunkDir.resolve(String.valueOf(chunkNum)),
                       StandardCopyOption.REPLACE_EXISTING);

            UploadTask task = getTaskByIdentifier(identifier);
            if (task == null) {
                task = new UploadTask();
                task.setIdentifier(identifier);
                task.setFileName(fileName);
                task.setFilePath(filePath);
                task.setTotalSize(totalSize);
                task.setTotalChunks(totalChunks);
                task.setUserId(userId);
                task.setUploadStatus(0);
                task.setCreateTime(LocalDateTime.now());
                taskMapper.insert(task);
            }
            task.setChunkNum(chunkNum + 1);
            taskMapper.updateById(task);
        } catch (IOException e) {
            throw new QiwenException(500, "分片上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public FileBean mergeChunks(String identifier, String filePath, String userId) {
        UploadTask task = getTaskByIdentifier(identifier);
        if (task == null) throw new QiwenException(404, "上传任务不存在");

        try {
            Path target = Paths.get(ufopConfig.getRootPath(), filePath);
            Files.createDirectories(target.getParent());
            try (OutputStream out = new FileOutputStream(target.toFile())) {
                for (int i = 0; i < task.getTotalChunks(); i++) {
                    Path chunk = getChunkDir(identifier).resolve(String.valueOf(i));
                    Files.copy(chunk, out);
                }
            }

            FileBean file = fileService.upload(task.getFileName(), filePath,
                    Files.size(target), getContentType(task.getFileName()), userId);

            task.setUploadStatus(1);
            taskMapper.updateById(task);
            cleanupChunks(identifier);
            return file;
        } catch (IOException e) {
            throw new QiwenException(500, "合并分片失败: " + e.getMessage());
        }
    }

    @Override
    public UploadTask getProgress(String identifier) {
        return getTaskByIdentifier(identifier);
    }

    @Override
    public void cleanupChunks(String identifier) {
        try { FileUtil.del(getChunkDir(identifier).toFile()); }
        catch (Exception e) { log.warn("清理分片失败: {}", e.getMessage()); }
    }

    private UploadTask getTaskByIdentifier(String identifier) {
        LambdaQueryWrapper<UploadTask> w = new LambdaQueryWrapper<>();
        w.eq(UploadTask::getIdentifier, identifier);
        return taskMapper.selectOne(w);
    }

    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }
}
