package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.vo.EditConfigVO;
import com.qiwenshare.document.vo.PreviewConfigVO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.service.FilePermissionService;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * 文档编辑服务。
 *
 * <p>构建编辑模式配置，包含权限检查、COW 逻辑、格式转换降级。</p>
 */
@Service
@Slf4j
public class DocumentEditService {

    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;
    private final FilePermissionService filePermissionService;
    private final StorageFactory storageFactory;
    private final DocumentPreviewService documentPreviewService;
    private final DocumentTokenService documentTokenService;
    private final OnlyOfficeProperties onlyOfficeProperties;
    /** @Lazy 自注入，确保 COW 事务方法通过 AOP 代理（红线 #16） */
    private final DocumentEditService self;

    public DocumentEditService(UserFileRepository userFileRepository,
                               FileBeanRepository fileBeanRepository,
                               FilePermissionService filePermissionService,
                               StorageFactory storageFactory,
                               DocumentPreviewService documentPreviewService,
                               DocumentTokenService documentTokenService,
                               OnlyOfficeProperties onlyOfficeProperties,
                               @Lazy DocumentEditService self) {
        this.userFileRepository = userFileRepository;
        this.fileBeanRepository = fileBeanRepository;
        this.filePermissionService = filePermissionService;
        this.storageFactory = storageFactory;
        this.documentPreviewService = documentPreviewService;
        this.documentTokenService = documentTokenService;
        this.onlyOfficeProperties = onlyOfficeProperties;
        this.self = self;
    }

    /**
     * 构建编辑配置。
     *
     * <p>非事务方法：COW 物理文件 IO 在事务外执行（红线 #15）。</p>
     *
     * @param userFileId 用户文件 ID
     * @param userId     当前用户 ID
     * @return 编辑配置 VO
     */
    public EditConfigVO buildEditConfig(Long userFileId, Long userId) {
        // 查询文件
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED));

        // 编辑权限检查
        if (!filePermissionService.canEdit(userId, userFileId)) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED);
        }

        // 查询 FileBean
        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED));

        // 大小检查
        if (fileBean.getFileSize() > onlyOfficeProperties.getMaxFileSize()) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_FILE_TOO_LARGE);
        }

        String extension = userFile.getExtendName() != null ? userFile.getExtendName().toLowerCase() : "";

        // 检查格式是否可编辑
        boolean isEditable = onlyOfficeProperties.getEditedExtensions().contains(extension);
        boolean isConvertible = onlyOfficeProperties.getConvertExtensions().contains(extension);

        if (!isEditable && !isConvertible) {
            // 不可编辑也不可转换，降级为预览
            log.info("文件格式不支持编辑，降级为预览: userFileId={}, extension={}", userFileId, extension);
            PreviewConfigVO previewConfig = documentPreviewService.buildPreviewConfig(userFileId, userId);
            EditConfigVO editConfig = new EditConfigVO();
            editConfig.setDocserviceApiUrl(previewConfig.getDocserviceApiUrl());
            editConfig.setDocument(previewConfig.getDocument());
            editConfig.setEditorConfig(previewConfig.getEditorConfig());
            editConfig.setToken(previewConfig.getToken());
            return editConfig;
        }

        // COW：检查 FileBean 引用数（直接使用已加载的 fileBean，无需重复 findById）
        boolean cowApplied = false;
        long refCount = userFileRepository.countByFileIdAndDeleteStatus(fileBean.getFileId(), 0);

        if (refCount > 1) {
            // 多引用，先在事务外复制物理文件（红线 #15：事务内禁止外部 IO）
            log.info("COW: FileBean 被 {} 个 UserFile 引用，创建副本: userFileId={}", refCount, userFileId);
            FileBean copy = createFileBeanCopy(fileBean);
            // 事务内更新 DB 引用
            fileBean = self.saveCowCopy(userFile, copy);
            cowApplied = true;
        }

        // 构建编辑配置
        PreviewConfigVO previewConfig = documentPreviewService.buildConfig(userFile, fileBean, "edit", userId);

        EditConfigVO editConfig = new EditConfigVO();
        editConfig.setDocserviceApiUrl(previewConfig.getDocserviceApiUrl());
        editConfig.setDocument(previewConfig.getDocument());
        editConfig.setEditorConfig(previewConfig.getEditorConfig());
        editConfig.setToken(previewConfig.getToken());
        editConfig.setCowApplied(cowApplied);

        return editConfig;
    }

    /**
     * 事务内保存 COW 副本的 DB 引用。
     *
     * <p>物理文件复制已在事务外完成，此方法仅执行 DB 写操作。</p>
     */
    @Transactional
    public FileBean saveCowCopy(UserFile userFile, FileBean copy) {
        FileBean saved = fileBeanRepository.save(copy);
        userFile.setFileId(saved.getFileId());
        userFileRepository.save(userFile);
        return saved;
    }

    /**
     * 创建 FileBean 副本（COW）—— 事务外执行物理文件复制。
     */
    private FileBean createFileBeanCopy(FileBean original) {
        // 复制物理文件（外部 IO，红线 #15）
        String newPath = "document/cow/" + System.currentTimeMillis() + "/" + original.getStoragePath();
        try (InputStream is = storageFactory.getBackend().download(original.getStoragePath())) {
            storageFactory.getBackend().write(newPath, is);
        } catch (Exception e) {
            log.error("COW 物理文件复制失败: {}", original.getStoragePath(), e);
            throw new DocumentModuleException(DocumentErrorCode.DOC_CONVERT_FAILED, e);
        }

        // 创建新 FileBean（不在此处 save，由 saveCowCopy 事务内 save）
        FileBean copy = new FileBean();
        copy.setFileSize(original.getFileSize());
        copy.setFileHash(original.getFileHash());
        copy.setStorageType(original.getStorageType());
        copy.setStoragePath(newPath);
        return copy;
    }
}
