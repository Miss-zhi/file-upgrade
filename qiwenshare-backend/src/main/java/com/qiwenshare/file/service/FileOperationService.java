package com.qiwenshare.file.service;

import com.qiwenshare.file.common.FileCategory;
import com.qiwenshare.file.dto.*;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.event.FileChangedEvent;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.BatchOperationResultVO;
import com.qiwenshare.file.vo.FileDetailVO;
import com.qiwenshare.file.vo.FileListVO;
import com.qiwenshare.file.vo.TreeNodeVO;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件 CRUD 操作服务。
 *
 * <p>处理文件列表查询、重命名、移动、复制、创建文件夹、文件树等。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileOperationService {

    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;
    private final StorageQuotaService storageQuotaService;
    private final StorageFactory storageFactory;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询文件列表。
     *
     * @param userId 用户 ID
     * @param dto    查询参数
     * @return 分页文件列表
     */
    public Page<FileListVO> listFiles(Long userId, FileListDTO dto) {
        Sort sort = "desc".equalsIgnoreCase(dto.sort())
                ? Sort.by(dto.order()).descending()
                : Sort.by(dto.order()).ascending();
        PageRequest pageable = PageRequest.of(dto.page(), dto.size(), sort);

        Page<UserFile> page;
        if (dto.fileType() != null && dto.filePath() == null) {
            // 按类型分类浏览（跨目录）
            page = userFileRepository.findByUserIdAndDeleteStatusAndFileType(
                    userId, 0, dto.fileType(), pageable);
        } else if (dto.fileType() != null) {
            page = userFileRepository.findByUserIdAndFilePathAndDeleteStatusAndFileType(
                    userId, dto.filePath(), 0, dto.fileType(), pageable);
        } else {
            page = userFileRepository.findByUserIdAndFilePathAndDeleteStatus(
                    userId, dto.filePath(), 0, pageable);
        }

        return toFileListVOBatch(page);
    }

    /**
     * 按文件分类浏览。
     *
     * @param userId   用户 ID
     * @param category 分类名
     * @param page     页码
     * @param size     每页大小
     * @return 分页文件列表
     */
    public Page<FileListVO> listFilesByCategory(Long userId, String category, int page, int size) {
        Set<String> extensions = FileCategory.getExtensionsByCategoryName(category);
        if (extensions.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("uploadTime").descending());
        Page<UserFile> result = userFileRepository.findByUserIdAndDeleteStatusAndExtendNameIn(
                userId, 0, extensions, pageable);
        return toFileListVOBatch(result);
    }

    /**
     * 重命名文件。
     *
     * @param dto    重命名请求
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void renameFile(RenameFileDTO dto, Long userId) {
        UserFile userFile = getUserFileOwnedByUser(dto.userFileId(), userId);

        String newName = dto.newName();
        String nameWithoutExt = newName;
        String extendName = "";
        int dotIndex = newName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = newName.substring(0, dotIndex);
            extendName = newName.substring(dotIndex + 1);
        }

        // 检查同名文件
        checkDuplicate(userId, userFile.getFilePath(), nameWithoutExt, extendName, userFile.getFileType());

        userFile.setFileName(nameWithoutExt);
        userFile.setExtendName(extendName);
        userFileRepository.save(userFile);

        // 发布文件更新事件
        publishUpdatedEvent(userFile.getUserFileId());
    }

    /**
     * 移动文件到目标文件夹。
     *
     * @param dto    移动请求
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveFile(MoveFileDTO dto, Long userId) {
        UserFile userFile = getUserFileOwnedByUser(dto.userFileId(), userId);

        String targetPath;
        if (dto.targetFolderId() == null) {
            targetPath = "/";
        } else {
            UserFile targetFolder = getUserFileOwnedByUser(dto.targetFolderId(), userId);
            if (targetFolder.getFileType() != 2) {
                throw new FileModuleException(FileErrorCode.FOLDER_NOT_FOUND);
            }
            targetPath = buildFullPath(targetFolder);
        }

        // 文件夹不能移动到自身或子目录
        if (userFile.getFileType() == 2) {
            String oldPath = buildFullPath(userFile);
            if (targetPath.startsWith(oldPath + "/") || targetPath.equals(oldPath)) {
                throw new FileModuleException(FileErrorCode.MOVE_TO_SELF);
            }
        }

        String oldPath = buildFullPath(userFile);

        // 检查目标目录同名文件
        checkDuplicate(userId, targetPath, userFile.getFileName(), userFile.getExtendName(), userFile.getFileType());

        userFile.setFilePath(targetPath);
        userFileRepository.save(userFile);

        // 如果是文件夹，递归更新子文件路径
        if (userFile.getFileType() == 2) {
            updateChildrenPath(userId, oldPath, targetPath + "/" + userFile.getFileName(), 0);
        }

        // 发布文件更新事件
        publishUpdatedEvent(userFile.getUserFileId());
    }

    /**
     * 批量移动文件。
     *
     * @param dto    批量移动请求
     * @param userId 用户 ID
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchMoveFile(BatchMoveFileDTO dto, Long userId) {
        int successCount = 0;
        List<BatchOperationResultVO.FailedItem> failedItems = new ArrayList<>();

        for (Long fileId : dto.userFileIds()) {
            try {
                moveFile(new MoveFileDTO(fileId, dto.targetFolderId()), userId);
                successCount++;
            } catch (Exception e) {
                failedItems.add(new BatchOperationResultVO.FailedItem(fileId, e.getMessage()));
            }
        }

        return new BatchOperationResultVO(successCount, failedItems);
    }

    /**
     * 复制文件。
     *
     * @param dto    复制请求
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void copyFile(CopyFileDTO dto, Long userId) {
        UserFile sourceFile = getUserFileOwnedByUser(dto.userFileId(), userId);

        String targetPath;
        if (dto.targetFolderId() == null) {
            targetPath = "/";
        } else {
            UserFile targetFolder = getUserFileOwnedByUser(dto.targetFolderId(), userId);
            if (targetFolder.getFileType() != 2) {
                throw new FileModuleException(FileErrorCode.FOLDER_NOT_FOUND);
            }
            targetPath = buildFullPath(targetFolder);
        }

        // 检查目标目录同名文件（自动重命名）
        String copyName = generateCopyName(userId, targetPath, sourceFile.getFileName(), sourceFile.getExtendName(), sourceFile.getFileType());
        String copyExtendName = sourceFile.getExtendName();

        UserFile copy = new UserFile();
        copy.setUserId(userId);
        copy.setFileId(sourceFile.getFileId()); // 复用 FileBean
        copy.setFileName(copyName);
        copy.setExtendName(copyExtendName);
        copy.setFilePath(targetPath);
        copy.setFileType(sourceFile.getFileType());
        userFileRepository.save(copy);

        // 如果是文件夹，递归复制子文件
        if (sourceFile.getFileType() == 2) {
            String sourcePath = buildFullPath(sourceFile);
            copyChildrenRecursive(userId, sourcePath, targetPath + "/" + copyName, userId);
        }
    }

    /**
     * 批量复制文件。
     *
     * @param dto    批量复制请求
     * @param userId 用户 ID
     * @return 批量操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchCopyFile(BatchCopyFileDTO dto, Long userId) {
        int successCount = 0;
        List<BatchOperationResultVO.FailedItem> failedItems = new ArrayList<>();

        for (Long fileId : dto.userFileIds()) {
            try {
                copyFile(new CopyFileDTO(fileId, dto.targetFolderId()), userId);
                successCount++;
            } catch (Exception e) {
                failedItems.add(new BatchOperationResultVO.FailedItem(fileId, e.getMessage()));
            }
        }

        return new BatchOperationResultVO(successCount, failedItems);
    }

    /**
     * 创建文件夹。
     *
     * @param dto    创建文件夹请求
     * @param userId 用户 ID
     * @return 创建的文件夹 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFolder(CreateFoldDTO dto, Long userId) {
        String folderPath = dto.filePath();
        String folderName = dto.folderName();

        // 检查同名文件夹
        checkDuplicate(userId, folderPath, folderName, "", 2);

        UserFile folder = new UserFile();
        folder.setUserId(userId);
        folder.setFileName(folderName);
        folder.setExtendName("");
        folder.setFilePath(folderPath);
        folder.setFileType(2);
        folder = userFileRepository.save(folder);
        return folder.getUserFileId();
    }

    /** 新建 Office 文档时使用的模板映射（classpath:static/template/）。 */
    private static final Map<String, String> TEMPLATE_MAP = Map.of(
            "docx", "static/template/Word.docx",
            "xlsx", "static/template/Excel.xlsx",
            "pptx", "static/template/PowerPoint.pptx"
    );

    /**
     * 创建文件。
     *
     * <p>对 Office 文档（docx/xlsx/pptx）使用 classpath 下的模板文件上传到存储后端，
     * 生成真实的 FileBean，确保新建的文件可被 OnlyOffice 正常打开和编辑。
     * 其他扩展名仍走空文件逻辑（fileId=null），前端可引导用户后续上传内容。</p>
     *
     * @param dto    创建文件请求
     * @param userId 用户 ID
     * @return 创建的文件 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFile(CreateFileDTO dto, Long userId) {
        String fileName = dto.fileName();
        String nameWithoutExt = fileName;
        String extendName = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExt = fileName.substring(0, dotIndex);
            extendName = fileName.substring(dotIndex + 1);
        }

        // 检查同名文件
        checkDuplicate(userId, dto.filePath(), nameWithoutExt, extendName, 1);

        String templatePath = TEMPLATE_MAP.get(extendName.toLowerCase());
        Long fileBeanId = null;

        if (templatePath != null) {
            // 从 classpath 读取模板字节
            byte[] templateBytes;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
                if (is == null) {
                    log.error("模板文件缺失: {}", templatePath);
                    throw new FileModuleException(FileErrorCode.TEMPLATE_LOAD_FAILED);
                }
                templateBytes = is.readAllBytes();
            } catch (FileModuleException e) {
                throw e;
            } catch (Exception e) {
                log.error("读取模板文件失败: {}", templatePath, e);
                throw new FileModuleException(FileErrorCode.TEMPLATE_LOAD_FAILED);
            }

            if (templateBytes.length == 0) {
                log.error("模板文件为空: {}", templatePath);
                throw new FileModuleException(FileErrorCode.TEMPLATE_LOAD_FAILED);
            }

            long fileSize = templateBytes.length;
            String fileHash = calculateHash(new ByteArrayInputStream(templateBytes));

            // 配额校验 + 预扣
            storageQuotaService.checkQuota(userId, fileSize);
            storageQuotaService.preDeduct(userId, fileSize);

            try {
                // 文件去重：如 hash+size 已存在则复用 FileBean
                FileBean fileBean = fileBeanRepository.findByFileHashAndFileSize(fileHash, fileSize)
                        .orElseGet(() -> {
                            String storagePath = generateStoragePath(fileName);
                            try (InputStream is = new ByteArrayInputStream(templateBytes)) {
                                storageFactory.getBackend().upload(is, storagePath, fileSize);
                            } catch (Exception e) {
                                log.error("上传模板到存储后端失败: {}", templatePath, e);
                                throw new FileModuleException(FileErrorCode.UPLOAD_STORAGE_ERROR);
                            }
                            FileBean newBean = new FileBean();
                            newBean.setFileSize(fileSize);
                            newBean.setFileHash(fileHash);
                            newBean.setStorageType(storageFactory.getActiveType());
                            newBean.setStoragePath(storagePath);
                            return fileBeanRepository.save(newBean);
                        });
                fileBeanId = fileBean.getFileId();

                // 确认配额（预扣量 = 实际量）
                storageQuotaService.confirmQuota(userId, fileSize, fileSize);
            } catch (FileModuleException e) {
                storageQuotaService.releaseQuota(userId, fileSize);
                throw e;
            } catch (Exception e) {
                storageQuotaService.releaseQuota(userId, fileSize);
                log.error("创建模板文件失败: fileName={}", fileName, e);
                throw new FileModuleException(FileErrorCode.UPLOAD_STORAGE_ERROR);
            }
        } else {
            log.debug("扩展名 {} 无对应模板，创建空文件记录: fileName={}", extendName, fileName);
        }

        UserFile userFile = new UserFile();
        userFile.setUserId(userId);
        userFile.setFileId(fileBeanId);
        userFile.setFileName(nameWithoutExt);
        userFile.setExtendName(extendName);
        userFile.setFilePath(dto.filePath());
        userFile.setFileType(1);
        userFile = userFileRepository.save(userFile);

        // 事务提交后发布文件变更事件（仅当实际写入了存储）
        if (fileBeanId != null) {
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
        }

        return userFile.getUserFileId();
    }

    private String generateStoragePath(String fileName) {
        String datePath = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + java.util.UUID.randomUUID().toString().replace("-", "") + "_" + fileName;
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

    /**
     * 获取文件详情。
     *
     * @param userFileId 用户文件 ID
     * @param userId     用户 ID
     * @return 文件详情
     */
    public FileDetailVO getFileDetail(Long userFileId, Long userId) {
        UserFile userFile = getUserFileOwnedByUser(userFileId, userId);

        Long fileSize = 0L;
        String fileHash = "";
        String storageType = "";

        if (userFile.getFileId() != null && userFile.getFileType() == 1) {
            FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                    .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));
            fileSize = fileBean.getFileSize();
            fileHash = fileBean.getFileHash();
            storageType = fileBean.getStorageType();
        }

        String fullName = userFile.getFileName()
                + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                        ? "." + userFile.getExtendName() : "");

        return new FileDetailVO(
                userFile.getUserFileId(), fullName, userFile.getFilePath(),
                userFile.getFileType(), fileSize, userFile.getExtendName(),
                fileHash, storageType, userFile.getUploadTime(), userFile.getModifyTime()
        );
    }

    /**
     * 修改文件文本内容。
     *
     * <p>支持 Copy-On-Write：当 FileBean 被多个 UserFile 引用时，
     * 先复制物理文件再写入新内容，避免影响其他引用者。</p>
     *
     * @param dto    修改请求（userFileId + fileContent）
     * @param userId 当前用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFileContent(UpdateFileDTO dto, Long userId) {
        UserFile userFile = getUserFileOwnedByUser(dto.userFileId(), userId);

        if (userFile.getFileType() != 1) {
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }

        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        // COW：多引用时先复制物理文件
        long refCount = userFileRepository.countByFileIdAndDeleteStatus(fileBean.getFileId(), 0);
        if (refCount > 1) {
            log.info("COW: FileBean 被 {} 个 UserFile 引用，创建副本: userFileId={}", refCount, dto.userFileId());
            String newPath = "file/cow/" + System.currentTimeMillis() + "/" + fileBean.getStoragePath();
            try (InputStream is = storageFactory.getBackend().download(fileBean.getStoragePath())) {
                storageFactory.getBackend().write(newPath, is);
            } catch (Exception e) {
                log.error("COW 物理文件复制失败: {}", fileBean.getStoragePath(), e);
                throw new FileModuleException(FileErrorCode.FILE_UPDATE_FAILED);
            }
            fileBean.setStoragePath(newPath);
        }

        // 写入新内容
        byte[] contentBytes = dto.fileContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (InputStream is = new ByteArrayInputStream(contentBytes)) {
            storageFactory.getBackend().write(fileBean.getStoragePath(), is);
        } catch (Exception e) {
            log.error("文件内容写入失败: userFileId={}", dto.userFileId(), e);
            throw new FileModuleException(FileErrorCode.FILE_UPDATE_FAILED);
        }

        // 更新 FileBean 的 hash 和 size
        String newHash = computeSha256(contentBytes);
        fileBean.setFileHash(newHash);
        fileBean.setFileSize((long) contentBytes.length);
        fileBeanRepository.save(fileBean);

        // 发布文件更新事件
        publishUpdatedEvent(userFile.getUserFileId());
    }

    /**
     * 计算 SHA-256 哈希。
     */
    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    /**
     * 获取文件树（仅文件夹层级结构）。
     *
     * @param userId 用户 ID
     * @return 文件树根节点列表
     */
    public List<TreeNodeVO> getFileTree(Long userId) {
        List<UserFile> allFolders = userFileRepository.findByUserIdAndDeleteStatusAndFileType(userId, 0, 2);

        // 按路径分组构建树
        Map<String, List<UserFile>> pathMap = allFolders.stream()
                .collect(Collectors.groupingBy(UserFile::getFilePath));

        return buildTree(pathMap, "/");
    }

    private List<TreeNodeVO> buildTree(Map<String, List<UserFile>> pathMap, String currentPath) {
        List<UserFile> folders = pathMap.getOrDefault(currentPath, List.of());
        return folders.stream()
                .map(folder -> {
                    String fullPath = currentPath.equals("/")
                            ? "/" + folder.getFileName()
                            : currentPath + "/" + folder.getFileName();
                    List<TreeNodeVO> children = buildTree(pathMap, fullPath);
                    return new TreeNodeVO(folder.getUserFileId(), folder.getFileName(), fullPath, children);
                })
                .collect(Collectors.toList());
    }

    private UserFile getUserFileOwnedByUser(Long userFileId, Long userId) {
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));
        if (!userFile.getUserId().equals(userId)) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        return userFile;
    }

    private void checkDuplicate(Long userId, String filePath, String fileName, String extendName, Integer fileType) {
        boolean exists = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                userId, filePath, fileName, extendName, 0, fileType);
        if (exists) {
            throw new FileModuleException(FileErrorCode.FILE_NAME_DUPLICATE);
        }
    }

    private String generateCopyName(Long userId, String filePath, String fileName, String extendName, Integer fileType) {
        boolean exists = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                userId, filePath, fileName, extendName, 0, fileType);
        if (!exists) {
            return fileName;
        }
        // 自动添加后缀 (1), (2)...
        for (int i = 1; i <= 100; i++) {
            String newName = fileName + "(" + i + ")";
            boolean nameExists = userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    userId, filePath, newName, extendName, 0, fileType);
            if (!nameExists) {
                return newName;
            }
        }
        throw new FileModuleException(FileErrorCode.FILE_NAME_DUPLICATE);
    }

    private String buildFullPath(UserFile userFile) {
        if (userFile.getFilePath() == null || userFile.getFilePath().equals("/")) {
            return "/" + userFile.getFileName();
        }
        return userFile.getFilePath() + "/" + userFile.getFileName();
    }

    private void updateChildrenPath(Long userId, String oldPath, String newPath, Integer deleteStatus) {
        List<UserFile> children = userFileRepository.findByUserIdAndFilePathStartingWithAndDeleteStatus(
                userId, oldPath, deleteStatus);
        for (UserFile child : children) {
            child.setFilePath(child.getFilePath().replaceFirst(
                    java.util.regex.Pattern.quote(oldPath), newPath));
            userFileRepository.save(child);
        }
    }

    private void copyChildrenRecursive(Long userId, String sourcePath, String targetPath, Long copyUserId) {
        List<UserFile> children = userFileRepository.findByUserIdAndFilePathStartingWithAndDeleteStatus(
                userId, sourcePath, 0);
        for (UserFile child : children) {
            String newChildPath = child.getFilePath().replaceFirst(
                    java.util.regex.Pattern.quote(sourcePath), targetPath);

            UserFile copy = new UserFile();
            copy.setUserId(copyUserId);
            copy.setFileId(child.getFileId());
            copy.setFileName(child.getFileName());
            copy.setExtendName(child.getExtendName());
            copy.setFilePath(newChildPath);
            copy.setFileType(child.getFileType());
            userFileRepository.save(copy);
        }
    }

    private void publishUpdatedEvent(Long userFileId) {
        FileChangedEvent event = new FileChangedEvent(this, userFileId, FileChangedEvent.ChangeType.UPDATED);
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

    /**
     * 批量转换 UserFile 为 FileListVO，一次查询获取所有 FileBean 避免 N+1。
     */
    private Page<FileListVO> toFileListVOBatch(Page<UserFile> page) {
        List<UserFile> userFiles = page.getContent();

        // 批量获取所有文件的 FileBean（1 条 SQL 代替 N 条）
        Set<Long> fileIds = userFiles.stream()
                .filter(f -> f.getFileId() != null && f.getFileType() == 1)
                .map(UserFile::getFileId)
                .collect(Collectors.toSet());

        Map<Long, FileBean> fileBeanMap = new HashMap<>();
        if (!fileIds.isEmpty()) {
            fileBeanRepository.findAllById(fileIds)
                    .forEach(fb -> fileBeanMap.put(fb.getFileId(), fb));
        }

        List<FileListVO> voList = userFiles.stream().map(userFile -> {
            Long fileSize = 0L;
            if (userFile.getFileId() != null && userFile.getFileType() == 1) {
                FileBean fileBean = fileBeanMap.get(userFile.getFileId());
                if (fileBean != null) {
                    fileSize = fileBean.getFileSize();
                }
            }
            String fullName = userFile.getFileName()
                    + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                            ? "." + userFile.getExtendName() : "");
            return new FileListVO(
                    userFile.getUserFileId(), fullName, userFile.getFilePath(),
                    userFile.getFileType(), fileSize, userFile.getExtendName(),
                    userFile.getUploadTime(), userFile.getModifyTime(), userFile.getDeleteStatus()
            );
        }).collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(voList, page.getPageable(), page.getTotalElements());
    }
}
