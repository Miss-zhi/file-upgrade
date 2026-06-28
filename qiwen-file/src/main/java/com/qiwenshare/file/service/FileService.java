package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.mapper.FileBeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件服务实现
 */
@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<FileBeanMapper, FileBean> implements IFileService {

    private final FileBeanMapper fileBeanMapper;

    @Override
    public List<FileBean> listByPath(String path, String userId) {
        LambdaQueryWrapper<FileBean> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileBean::getParentPath, path)
               .eq(FileBean::getUserId, userId)
               .orderByAsc(FileBean::getIsFolder)
               .orderByAsc(FileBean::getFileName);
        return fileBeanMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public FileBean upload(String fileName, String filePath, Long fileSize, String fileType, String userId) {
        FileBean file = new FileBean();
        file.setId(IdUtil.getSnowflakeNextIdStr());
        file.setFileName(fileName);
        file.setFilePath(filePath);
        file.setFileSize(fileSize);
        file.setFileType(fileType);
        file.setIsFolder(false);
        file.setParentPath(getParentPath(filePath));
        file.setUserId(userId);
        file.setCreateTime(LocalDateTime.now());
        file.setUpdateTime(LocalDateTime.now());

        fileBeanMapper.insert(file);
        return file;
    }

    @Override
    @Transactional
    public void delete(String fileId, String userId) {
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new QiwenException(403, "无权删除此文件");
        }
        fileBeanMapper.deleteById(fileId);
    }

    @Override
    @Transactional
    public FileBean createFolder(String parentPath, String folderName, String userId) {
        String folderPath = ensureTrailingSlash(parentPath) + folderName;

        // 检查同名文件夹
        LambdaQueryWrapper<FileBean> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileBean::getFilePath, folderPath)
               .eq(FileBean::getUserId, userId);
        if (fileBeanMapper.selectCount(wrapper) > 0) {
            throw new QiwenException(400, "同名文件夹已存在");
        }

        FileBean folder = new FileBean();
        folder.setId(IdUtil.getSnowflakeNextIdStr());
        folder.setFileName(folderName);
        folder.setFilePath(folderPath);
        folder.setFileSize(0L);
        folder.setFileType("");
        folder.setIsFolder(true);
        folder.setParentPath(parentPath);
        folder.setUserId(userId);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());

        fileBeanMapper.insert(folder);
        return folder;
    }

    @Override
    public FileBean getById(String fileId) {
        return fileBeanMapper.selectById(fileId);
    }

    @Override
    public FileBean getByPath(String filePath) {
        LambdaQueryWrapper<FileBean> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileBean::getFilePath, filePath);
        return fileBeanMapper.selectOne(wrapper);
    }

    private String getParentPath(String filePath) {
        int idx = filePath.lastIndexOf('/');
        return idx <= 0 ? "/" : filePath.substring(0, idx + 1);
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
