package com.qiwenshare.file.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.exception.QiwenException;
import com.qiwenshare.file.mapper.FileBeanMapper;
import com.qiwenshare.ufop.UFOPFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<FileBeanMapper, FileBean> implements IFileService {

    private final FileBeanMapper fileBeanMapper;
    private final FileSearchService searchService;
    private final UFOPFactory ufopFactory;

    @Override
    public List<FileBean> listByPath(String path, String userId) {
        LambdaQueryWrapper<FileBean> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileBean::getParentPath, path)
               .eq(FileBean::getUserId, userId)
               .eq(FileBean::getDeleted, 0)
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
        file.setDeleted(0);
        file.setCreateTime(LocalDateTime.now());
        file.setUpdateTime(LocalDateTime.now());

        fileBeanMapper.insert(file);
        // 异步建 ES 索引（忽略错误）
        try { searchService.createIndex(file.getId(), file.getFileName(), file.getFilePath(), file.getFileType(), file.getFileSize(), userId); } catch (Exception e) { log.warn("ES 索引失败: {}", e.getMessage()); }
        return file;
    }

    @Override
    @Transactional
    public void delete(String fileId, String userId) {
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new QiwenException(403, "无权删除此文件");
        }
        file.setDeleted(1);
        file.setUpdateTime(LocalDateTime.now());
        // 直接 SQL 更新，绕过 JPA dirty checking
        fileBeanMapper.updateById(file);
        // 删物理文件/ES（暂时保留，彻底删除时处理）
        try { ufopFactory.getDeleter().delete(file.getFilePath()); } catch (Exception e) { log.warn("物理文件删除失败: {}", e.getMessage()); }
        // 删 ES 索引
        try { searchService.deleteIndex(fileId); } catch (Exception e) { log.warn("ES 删除索引失败: {}", e.getMessage()); }
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

    @Override
    public List<FileBean> listDeleted(String userId) {
        LambdaQueryWrapper<FileBean> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileBean::getUserId, userId)
               .eq(FileBean::getDeleted, 1)
               .orderByDesc(FileBean::getUpdateTime);
        return fileBeanMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void restore(String fileId, String userId) {
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null || file.getDeleted() == null || file.getDeleted() != 1)
            throw new QiwenException(404, "文件不存在或未删除");
        file.setDeleted(0);
        file.setUpdateTime(LocalDateTime.now());
        fileBeanMapper.updateById(file);
    }

    @Override
    @Transactional
    public void permanentDelete(String fileId, String userId) {
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null) return;
        // 删物理文件
        try { ufopFactory.getDeleter().delete(file.getFilePath()); } catch (Exception e) { log.warn("物理删除失败: {}", e.getMessage()); }
        // ES 索引
        try { searchService.deleteIndex(fileId); } catch (Exception e) { log.warn("ES 删除索引失败: {}", e.getMessage()); }
        // 物理删 DB
        fileBeanMapper.deleteById(fileId);
    }

    @Override
    @Transactional
    public void batchDelete(List<String> fileIds, String userId) {
        for (String id : fileIds) {
            try { delete(id, userId); } catch (Exception e) { log.warn("批量删除跳过: id={}", id); }
        }
    }

    @Override
    @Transactional
    public void batchMove(List<String> fileIds, String targetPath, String userId) {
        String parent = targetPath.endsWith("/") ? targetPath : targetPath + "/";
        for (String id : fileIds) {
            FileBean file = fileBeanMapper.selectById(id);
            if (file == null || !file.getUserId().equals(userId)) continue;
            file.setFilePath(parent + file.getFileName());
            file.setParentPath(parent);
            file.setUpdateTime(LocalDateTime.now());
            fileBeanMapper.updateById(file);
        }
    }

    private String getParentPath(String filePath) {
        int idx = filePath.lastIndexOf('/');
        return idx <= 0 ? "/" : filePath.substring(0, idx + 1);
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
