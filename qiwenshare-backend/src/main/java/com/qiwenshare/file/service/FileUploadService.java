package com.qiwenshare.file.service;

import com.qiwenshare.file.dto.ChunkUploadDTO;
import com.qiwenshare.file.dto.ChunkUploadInitDTO;
import com.qiwenshare.file.dto.SpeedUploadDTO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UploadTask;
import com.qiwenshare.file.entity.UploadTaskDetail;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.event.FileChangedEvent;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UploadTaskDetailRepository;
import com.qiwenshare.file.repository.UploadTaskRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.UploadFileVO;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务。
 *
 * <p>处理普通上传、秒传、分片上传。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private static final long CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long SPEED_THRESHOLD = 10 * 1024 * 1024; // 10MB

    private final FileBeanRepository fileBeanRepository;
    private final UserFileRepository userFileRepository;
    private final UploadTaskRepository uploadTaskRepository;
    private final UploadTaskDetailRepository uploadTaskDetailRepository;
    private final StorageFactory storageFactory;
    private final StorageQuotaService storageQuotaService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 秒传：检查文件 hash 是否已存在，复用 FileBean。
     *
     * @param dto    秒传请求
     * @param userId 用户 ID
     * @return 上传结果
     */
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO speedUpload(SpeedUploadDTO dto, Long userId) {
        // 检查同名文件
        checkDuplicate(userId, dto.filePath(), dto.fileName());

        // 检查文件 hash 是否已存在
        return fileBeanRepository.findByFileHashAndFileSize(dto.fileHash(), dto.fileSize())
                .map(existingBean -> {
                    // 复用 FileBean，创建新 UserFile
                    UserFile userFile = createUserFile(userId, dto.filePath(), dto.fileName(), existingBean.getFileId());
                    userFileRepository.save(userFile);
                    return new UploadFileVO(userFile.getUserFileId(), dto.fileName(), dto.fileSize(), dto.fileHash(), true);
                })
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));
    }

    /**
     * 普通上传（≤10MB）。
     *
     * @param file     上传文件
     * @param filePath 目标目录
     * @param userId   用户 ID
     * @return 上传结果
     */
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO uploadFile(MultipartFile file, String filePath, Long userId) {
        long fileSize = file.getSize();

        // 大小校验
        if (fileSize > SPEED_THRESHOLD) {
            throw new FileModuleException(FileErrorCode.UPLOAD_SIZE_EXCEEDED);
        }

        // 检查同名文件
        String fileName = file.getOriginalFilename();
        checkDuplicate(userId, filePath, fileName);

        // 配额校验 + 预扣
        storageQuotaService.checkQuota(userId, fileSize);
        storageQuotaService.preDeduct(userId, fileSize);

        try {
            // 计算 hash
            String fileHash = calculateHash(file.getInputStream());

            // 检查去重
            FileBean fileBean = fileBeanRepository.findByFileHashAndFileSize(fileHash, fileSize)
                    .orElseGet(() -> {
                        // 上传到存储后端
                        String storagePath = generateStoragePath(fileName);
                        try (InputStream is = file.getInputStream()) {
                            storageFactory.getBackend().upload(is, storagePath, fileSize);
                        } catch (Exception e) {
                            throw new FileModuleException(FileErrorCode.UPLOAD_STORAGE_ERROR);
                        }
                        // 创建 FileBean
                        FileBean newBean = new FileBean();
                        newBean.setFileSize(fileSize);
                        newBean.setFileHash(fileHash);
                        newBean.setStorageType(storageFactory.getActiveType());
                        newBean.setStoragePath(storagePath);
                        return fileBeanRepository.save(newBean);
                    });

            // 创建 UserFile
            UserFile userFile = createUserFile(userId, filePath, fileName, fileBean.getFileId());
            userFileRepository.save(userFile);

            // 确认配额
            storageQuotaService.confirmQuota(userId, fileSize, fileSize);

            // 发布文件创建事件（事务提交后执行）
            Long userFileId = userFile.getUserFileId();
            FileChangedEvent event = new FileChangedEvent(this, userFileId, FileChangedEvent.ChangeType.CREATED);
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

            return new UploadFileVO(userFile.getUserFileId(), fileName, fileSize, fileHash, false);
        } catch (FileModuleException e) {
            storageQuotaService.releaseQuota(userId, fileSize);
            throw e;
        } catch (Exception e) {
            storageQuotaService.releaseQuota(userId, fileSize);
            log.error("文件上传失败", e);
            throw new FileModuleException(FileErrorCode.UPLOAD_STORAGE_ERROR);
        }
    }

    /**
     * 初始化分片上传任务。
     *
     * @param dto    分片上传初始化请求
     * @param userId 用户 ID
     * @return 任务 ID 和已上传分片列表
     */
    @Transactional(rollbackFor = Exception.class)
    public String initChunkUpload(ChunkUploadInitDTO dto, Long userId) {
        // 配额校验
        storageQuotaService.checkQuota(userId, dto.fileSize());

        String taskId = UUID.randomUUID().toString().replace("-", "");

        UploadTask task = new UploadTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setFileName(dto.fileName());
        task.setFileHash(dto.fileHash());
        task.setFileSize(dto.fileSize());
        task.setTotalChunks(dto.totalChunks());
        uploadTaskRepository.save(task);

        // 创建分片详情记录
        for (int i = 0; i < dto.totalChunks(); i++) {
            UploadTaskDetail detail = new UploadTaskDetail();
            detail.setTaskId(taskId);
            detail.setChunkIndex(i);
            detail.setChunkSize(i == dto.totalChunks() - 1 ? dto.fileSize() - (long) i * CHUNK_SIZE : CHUNK_SIZE);
            detail.setStatus(0);
            uploadTaskDetailRepository.save(detail);
        }

        return taskId;
    }

    /**
     * 上传单个分片。
     *
     * @param dto       分片信息
     * @param chunkData 分片数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(ChunkUploadDTO dto, MultipartFile chunkData) {
        UploadTask task = uploadTaskRepository.findById(dto.taskId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.UPLOAD_TASK_NOT_FOUND));

        // 校验分片序号
        if (dto.chunkIndex() < 0 || dto.chunkIndex() >= task.getTotalChunks()) {
            throw new FileModuleException(FileErrorCode.UPLOAD_CHUNK_MISMATCH);
        }

        // 上传分片到临时存储
        String chunkPath = "chunks/" + dto.taskId() + "/" + dto.chunkIndex();
        try (InputStream is = chunkData.getInputStream()) {
            storageFactory.getBackend().upload(is, chunkPath, chunkData.getSize());
        } catch (FileModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new FileModuleException(FileErrorCode.UPLOAD_STORAGE_ERROR);
        }

        // 更新分片状态
        uploadTaskDetailRepository.findByTaskIdAndChunkIndex(dto.taskId(), dto.chunkIndex())
                .ifPresent(detail -> {
                    detail.setStatus(1);
                    detail.setStoragePath(chunkPath);
                    detail.setChunkSize(chunkData.getSize());
                    uploadTaskDetailRepository.save(detail);
                });

        // 更新已上传分片数
        task.setUploadedChunks(task.getUploadedChunks() + 1);
        uploadTaskRepository.save(task);
    }

    /**
     * 合并分片。
     *
     * @param taskId   任务 ID
     * @param filePath 目标目录
     * @param userId   用户 ID
     * @return 上传结果
     */
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO mergeChunks(String taskId, String filePath, Long userId) {
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.UPLOAD_TASK_NOT_FOUND));

        if (!task.getUserId().equals(userId)) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }

        // 检查同名文件
        checkDuplicate(userId, filePath, task.getFileName());

        // 检查所有分片是否已上传
        if (task.getUploadedChunks() < task.getTotalChunks()) {
            throw new FileModuleException(FileErrorCode.UPLOAD_CHUNK_MISMATCH);
        }

        task.setStatus(1); // 合并中
        uploadTaskRepository.save(task);

        try {
            // 检查去重
            FileBean fileBean = fileBeanRepository.findByFileHashAndFileSize(task.getFileHash(), task.getFileSize())
                    .orElseGet(() -> {
                        // TODO: 合并分片为完整文件（需要存储后端支持）
                        String storagePath = generateStoragePath(task.getFileName());
                        // 简化处理：假设存储后端支持合并操作
                        FileBean newBean = new FileBean();
                        newBean.setFileSize(task.getFileSize());
                        newBean.setFileHash(task.getFileHash());
                        newBean.setStorageType(storageFactory.getActiveType());
                        newBean.setStoragePath(storagePath);
                        return fileBeanRepository.save(newBean);
                    });

            // 创建 UserFile
            UserFile userFile = createUserFile(userId, filePath, task.getFileName(), fileBean.getFileId());
            userFileRepository.save(userFile);

            // 确认配额
            storageQuotaService.confirmQuota(userId, task.getFileSize(), task.getFileSize());

            // 更新任务状态
            task.setStatus(2); // 完成
            uploadTaskRepository.save(task);

            // 清理分片临时数据
            cleanupChunks(taskId);

            return new UploadFileVO(userFile.getUserFileId(), task.getFileName(), task.getFileSize(), task.getFileHash(), false);
        } catch (Exception e) {
            task.setStatus(3); // 失败
            uploadTaskRepository.save(task);
            storageQuotaService.releaseQuota(userId, task.getFileSize());
            throw new FileModuleException(FileErrorCode.UPLOAD_TASK_MERGE_FAILED);
        }
    }

    private void checkDuplicate(Long userId, String filePath, String fileName) {
        String nameWithoutExt = fileName;
        String extendName = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extendName = fileName.substring(dotIndex + 1);
        }
        boolean exists = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                userId, filePath, nameWithoutExt, extendName, 0, 1);
        if (exists) {
            throw new FileModuleException(FileErrorCode.UPLOAD_DUPLICATE);
        }
    }

    private UserFile createUserFile(Long userId, String filePath, String fileName, Long fileBeanId) {
        String nameWithoutExt = fileName;
        String extendName = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extendName = fileName.substring(dotIndex + 1);
        }

        UserFile userFile = new UserFile();
        userFile.setUserId(userId);
        userFile.setFileId(fileBeanId);
        userFile.setFileName(nameWithoutExt);
        userFile.setExtendName(extendName);
        userFile.setFilePath(filePath);
        userFile.setFileType(1);
        return userFile;
    }

    private String generateStoragePath(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "_" + fileName;
    }

    private String calculateHash(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            inputStream.close();
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new FileModuleException(FileErrorCode.UPLOAD_STORAGE_ERROR);
        }
    }

    private void cleanupChunks(String taskId) {
        List<UploadTaskDetail> details = uploadTaskDetailRepository.findByTaskId(taskId);
        for (UploadTaskDetail detail : details) {
            if (detail.getStoragePath() != null) {
                try {
                    storageFactory.getBackend().delete(detail.getStoragePath());
                } catch (Exception e) {
                    log.warn("清理分片失败: {}", detail.getStoragePath(), e);
                }
            }
        }
        uploadTaskDetailRepository.deleteByTaskId(taskId);
    }
}
