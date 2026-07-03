package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.event.FileChangedEvent;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.FileListVO;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件回收站服务。
 *
 * <p>处理软删除、恢复、永久删除。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileRecoveryService {

    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;
    private final StorageFactory storageFactory;
    private final StorageQuotaService storageQuotaService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 回收站列表查询。
     *
     * @param userId 用户 ID
     * @param page   页码
     * @param size   每页大小
     * @return 回收站文件列表
     */
    public Page<FileListVO> listRecycleBin(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("deleteTime").descending());
        Page<UserFile> userFiles = userFileRepository.findByUserIdAndDeleteStatusOrderByDeleteTimeDesc(
                userId, 1, pageable);
        return userFiles.map(this::toFileListVO);
    }

    /**
     * 软删除单个文件。
     *
     * @param userFileId 文件 ID
     * @param userId     用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(Long userFileId, Long userId) {
        UserFile userFile = getUserFileOwnedByUser(userFileId, userId);
        String batchNum = UUID.randomUUID().toString().replace("-", "");
        doSoftDelete(userFile, batchNum);

        // 如果是文件夹，递归软删除子文件
        if (userFile.getFileType() == 2) {
            String folderPath = buildFullPath(userFile);
            List<UserFile> children = userFileRepository.findByUserIdAndFilePathStartingWithAndDeleteStatus(
                    userId, folderPath, 0);
            for (UserFile child : children) {
                doSoftDelete(child, batchNum);
            }
        }
    }

    /**
     * 批量软删除。
     *
     * @param userFileIds 文件 ID 列表
     * @param userId      用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSoftDelete(List<Long> userFileIds, Long userId) {
        String batchNum = UUID.randomUUID().toString().replace("-", "");
        for (Long fileId : userFileIds) {
            UserFile userFile = getUserFileOwnedByUser(fileId, userId);
            doSoftDelete(userFile, batchNum);

            if (userFile.getFileType() == 2) {
                String folderPath = buildFullPath(userFile);
                List<UserFile> children = userFileRepository.findByUserIdAndFilePathStartingWithAndDeleteStatus(
                        userId, folderPath, 0);
                for (UserFile child : children) {
                    doSoftDelete(child, batchNum);
                }
            }
        }
    }

    /**
     * 恢复文件。
     *
     * @param userFileIds 文件 ID 列表
     * @param userId      用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreFiles(List<Long> userFileIds, Long userId) {
        for (Long fileId : userFileIds) {
            UserFile userFile = getUserFileOwnedByUser(fileId, userId);
            if (userFile.getDeleteStatus() != 1) {
                continue;
            }

            // 检查原路径是否存在同名文件
            boolean conflict = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    userId, userFile.getFilePath(), userFile.getFileName(),
                    userFile.getExtendName(), 0, userFile.getFileType());
            if (conflict) {
                throw new FileModuleException(FileErrorCode.RECOVERY_CONFLICT);
            }

            userFile.setDeleteStatus(0);
            userFile.setDeleteTime(null);
            userFile.setDeleteBatchNum(null);
            userFileRepository.save(userFile);
        }
    }

    /**
     * 永久删除文件（异步清理存储）。
     *
     * @param userFileIds 文件 ID 列表
     * @param userId      用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void permanentDelete(List<Long> userFileIds, Long userId) {
        for (Long fileId : userFileIds) {
            UserFile userFile = getUserFileOwnedByUser(fileId, userId);
            if (userFile.getDeleteStatus() != 1) {
                throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
            }

            Long fileBeanId = userFile.getFileId();
            userFileRepository.delete(userFile);

            // 异步检查 FileBean 引用并清理存储
            if (fileBeanId != null) {
                asyncCleanupFileBean(fileBeanId, userId);
            }
        }
    }

    /**
     * 异步清理物理文件（检查引用计数后决定是否删除存储）。
     *
     * @param fileBeanId 物理文件 ID
     * @param userId     操作用户 ID（用于配额释放）
     */
    @Async("fileTaskExecutor")
    public void asyncCleanupFileBean(Long fileBeanId, Long userId) {
        try {
            long refCount = userFileRepository.countByFileIdAndDeleteStatus(fileBeanId, 0);
            if (refCount == 0) {
                // 无引用，删除物理文件
                fileBeanRepository.findById(fileBeanId).ifPresent(fileBean -> {
                    try {
                        storageFactory.getBackend(fileBean.getStorageType()).delete(fileBean.getStoragePath());
                        // 释放配额
                        storageQuotaService.releaseQuota(userId, fileBean.getFileSize());
                        fileBeanRepository.delete(fileBean);
                        log.info("永久删除物理文件: fileId={}, path={}", fileBeanId, fileBean.getStoragePath());
                    } catch (Exception e) {
                        log.error("删除物理文件失败: fileId={}", fileBeanId, e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("异步清理物理文件失败: fileId={}", fileBeanId, e);
        }
    }

    /**
     * 自动清理过期回收站文件（定时任务调用）。
     *
     * @param retentionDays 保留天数
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredRecycleBin(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        List<UserFile> expiredFiles = userFileRepository.findByDeleteStatusAndDeleteTimeBefore(1, threshold);
        log.info("清理过期回收站文件: {} 个", expiredFiles.size());

        for (UserFile userFile : expiredFiles) {
            Long fileBeanId = userFile.getFileId();
            userFileRepository.delete(userFile);

            if (fileBeanId != null) {
                asyncCleanupFileBean(fileBeanId, userFile.getUserId());
            }
        }
    }

    /**
     * 清空用户回收站（永久删除该用户所有已删除文件）。
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllRecycleBin(Long userId) {
        List<UserFile> deletedFiles = userFileRepository.findByUserIdAndDeleteStatus(userId, 1);
        log.info("清空回收站: userId={}, 文件数={}", userId, deletedFiles.size());

        for (UserFile userFile : deletedFiles) {
            Long fileBeanId = userFile.getFileId();
            userFileRepository.delete(userFile);

            if (fileBeanId != null) {
                asyncCleanupFileBean(fileBeanId, userId);
            }
        }
    }

    private void doSoftDelete(UserFile userFile, String batchNum) {
        userFile.setDeleteStatus(1);
        userFile.setDeleteTime(LocalDateTime.now());
        userFile.setDeleteBatchNum(batchNum);
        userFileRepository.save(userFile);

        // 发布文件删除事件（事务提交后执行）
        Long userFileId = userFile.getUserFileId();
        FileChangedEvent event = new FileChangedEvent(this, userFileId, FileChangedEvent.ChangeType.DELETED);
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(event);
                        }
                    });
        } else {
            eventPublisher.publishEvent(event);
        }
    }

    private UserFile getUserFileOwnedByUser(Long userFileId, Long userId) {
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));
        if (!userFile.getUserId().equals(userId)) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        return userFile;
    }

    private String buildFullPath(UserFile userFile) {
        if (userFile.getFilePath() == null || userFile.getFilePath().equals("/")) {
            return "/" + userFile.getFileName();
        }
        return userFile.getFilePath() + "/" + userFile.getFileName();
    }

    private FileListVO toFileListVO(UserFile userFile) {
        Long fileSize = 0L;
        if (userFile.getFileId() != null && userFile.getFileType() == 1) {
            fileSize = fileBeanRepository.findById(userFile.getFileId())
                    .map(FileBean::getFileSize)
                    .orElse(0L);
        }
        String fullName = userFile.getFileName()
                + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                        ? "." + userFile.getExtendName() : "");
        return new FileListVO(
                userFile.getUserFileId(), fullName, userFile.getFilePath(),
                userFile.getFileType(), fileSize, userFile.getExtendName(),
                userFile.getUploadTime(), userFile.getModifyTime(), userFile.getDeleteStatus()
        );
    }
}
