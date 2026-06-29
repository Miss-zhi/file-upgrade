package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.api.IFileVersionService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.file.FileVersion;
import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.mapper.FileBeanMapper;
import com.qiwenshare.file.mapper.FileVersionMapper;
import com.qiwenshare.ufop.UFOPFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileVersionService implements IFileVersionService {

    private final FileVersionMapper versionMapper;
    private final FileBeanMapper fileBeanMapper;
    private final UFOPFactory ufopFactory;

    @Override
    public FileVersion saveVersion(String fileId, String fileName, String filePath,
                                    Long fileSize, String storagePath, String userId) {
        int nextVersion = getNextVersion(fileId);
        FileVersion v = new FileVersion();
        v.setId(IdUtil.getSnowflakeNextIdStr());
        v.setFileId(fileId);
        v.setVersion(nextVersion);
        v.setFileName(fileName);
        v.setFilePath(filePath);
        v.setFileSize(fileSize);
        v.setStoragePath(storagePath);
        v.setUserId(userId);
        v.setCreateTime(LocalDateTime.now());
        versionMapper.insert(v);
        cleanupOldVersions(fileId, 10);
        return v;
    }

    @Override
    public List<FileVersion> listVersions(String fileId) {
        LambdaQueryWrapper<FileVersion> w = new LambdaQueryWrapper<>();
        w.eq(FileVersion::getFileId, fileId).orderByDesc(FileVersion::getVersion);
        return versionMapper.selectList(w);
    }

    @Override
    @Transactional
    public FileBean restoreVersion(String fileId, String versionId, String userId) {
        FileVersion target = versionMapper.selectById(versionId);
        if (target == null) throw new QiwenException(404, "版本不存在");
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null) throw new QiwenException(404, "文件不存在");
        // 保存当前状态为版本快照
        saveVersion(fileId, file.getFileName(), file.getFilePath(),
                    file.getFileSize(), file.getFilePath(), userId);
        // 恢复到目标版本
        file.setFileName(target.getFileName());
        file.setFilePath(target.getFilePath());
        file.setFileSize(target.getFileSize());
        file.setUpdateTime(LocalDateTime.now());
        fileBeanMapper.updateById(file);
        return file;
    }

    @Override
    public void cleanupOldVersions(String fileId, int maxVersions) {
        List<FileVersion> versions = listVersions(fileId);
        for (int i = maxVersions; i < versions.size(); i++) {
            versionMapper.deleteById(versions.get(i).getId());
        }
    }

    private int getNextVersion(String fileId) {
        LambdaQueryWrapper<FileVersion> w = new LambdaQueryWrapper<>();
        w.eq(FileVersion::getFileId, fileId)
         .orderByDesc(FileVersion::getVersion)
         .last("LIMIT 1");
        List<FileVersion> list = versionMapper.selectList(w);
        return list.isEmpty() ? 1 : list.get(0).getVersion() + 1;
    }
}
