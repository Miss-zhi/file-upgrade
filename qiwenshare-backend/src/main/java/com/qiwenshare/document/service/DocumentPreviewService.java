package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.vo.PreviewConfigVO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.service.FilePermissionService;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * 文档预览服务。
 *
 * <p>构建 OnlyOffice 预览配置，包含权限检查、大小检查、格式分类。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPreviewService {

    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;
    private final FilePermissionService filePermissionService;
    private final StorageFactory storageFactory;
    private final DocumentTokenService documentTokenService;
    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 构建预览配置。
     *
     * @param userFileId 用户文件 ID
     * @param userId     当前用户 ID
     * @return 预览配置 VO
     */
    public PreviewConfigVO buildPreviewConfig(Long userFileId, Long userId) {
        // 查询文件
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED));

        // 权限检查
        if (!filePermissionService.canView(userId, userFileId)) {
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

        // 判断是否有编辑权限
        boolean canEdit = filePermissionService.canEdit(userId, userFileId);
        boolean isEditable = onlyOfficeProperties.getEditedExtensions().contains(extension);
        String mode = (canEdit && isEditable) ? "edit" : "view";

        // 构建配置
        return buildConfig(userFile, fileBean, mode, userId);
    }

    /**
     * 构建 OnlyOffice Config 对象。
     */
    protected PreviewConfigVO buildConfig(UserFile userFile, FileBean fileBean, String mode, Long userId) {
        String extension = userFile.getExtendName() != null ? userFile.getExtendName().toLowerCase() : "";

        // 文档 key = SHA-256(userFileId + ":" + modifyTime.toEpochMilli()) 取前 16 位十六进制
        // 使用 toEpochMilli() 保证毫秒级稳定精度，SHA-256 保证唯一性
        long epochMillis = fileBean.getModifyTime() != null
                ? fileBean.getModifyTime().toInstant(ZoneOffset.UTC).toEpochMilli()
                : 0L;
        String keyInput = userFile.getUserFileId() + ":" + epochMillis;
        String documentKey = computeStableKey(keyInput);

        // 文件下载 URL
        String fileUrl = storageFactory.getBackend().getPreviewUrl(fileBean.getStoragePath());
        if (fileUrl == null || fileUrl.isBlank()) {
            // 本地存储等不支持预签名 URL，通过后端代理下载
            String previewBaseUrl = onlyOfficeProperties.getPreviewBaseUrl();
            if (previewBaseUrl == null || previewBaseUrl.isBlank()) {
                throw new DocumentModuleException(DocumentErrorCode.DOC_PREVIEW_NOT_CONFIGURED);
            }
            // 先生成 token，用于构建下载 URL
            String action = "edit".equals(mode) ? "edit" : "view";
            String token = documentTokenService.generateDocumentToken(
                    String.valueOf(userId), userFile.getUserFileId(), action);
            fileUrl = previewBaseUrl + "/api/v1/document/download/"
                    + userFile.getUserFileId() + "?token=" + token;
        }

        // DocumentType 判断
        String docType = determineDocType(extension);

        PreviewConfigVO vo = new PreviewConfigVO();
        vo.setDocserviceApiUrl(onlyOfficeProperties.getApiUrl());

        // Document 配置
        PreviewConfigVO.DocumentConfig docConfig = new PreviewConfigVO.DocumentConfig();
        docConfig.setKey(documentKey);
        docConfig.setTitle(userFile.getFileName() + (extension.isEmpty() ? "" : "." + extension));
        docConfig.setUrl(fileUrl);
        docConfig.setFileType(extension);
        docConfig.setDocType(docType);
        docConfig.setFileSize(fileBean.getFileSize());

        Map<String, Object> permissions = new HashMap<>();
        permissions.put("comment", false);
        permissions.put("download", true);
        permissions.put("edit", "edit".equals(mode));
        permissions.put("print", true);
        permissions.put("review", false);
        docConfig.setPermissions(permissions);

        vo.setDocument(docConfig);

        // EditorConfig
        PreviewConfigVO.EditorConfig editorConfig = new PreviewConfigVO.EditorConfig();
        editorConfig.setMode(mode);
        editorConfig.setLang("zh-CN");

        if ("edit".equals(mode)) {
            // 编辑模式需要 callback URL
            String callbackToken = documentTokenService.generateCallbackToken(
                    String.valueOf(userId), userFile.getUserFileId(), "edit");
            String callbackUrl = onlyOfficeProperties.getCallbackBaseUrl()
                    + "?token=" + callbackToken;
            editorConfig.setCallbackUrl(callbackUrl);
        }

        // 用户信息
        PreviewConfigVO.UserConfig userConfig = new PreviewConfigVO.UserConfig();
        userConfig.setId(String.valueOf(userId));
        userConfig.setName("User " + userId);
        editorConfig.setUser(userConfig);

        vo.setEditorConfig(editorConfig);

        // 生成 Editor Config token —— 必须用 OnlyOffice JWT secret 签名
        // Document Server 用此 token 验证编辑器请求来源，密钥不匹配会导致文档无法打开
        Map<String, Object> tokenPayload = new HashMap<>();
        tokenPayload.put("key", documentKey);
        tokenPayload.put("title", userFile.getFileName() + (extension.isEmpty() ? "" : "." + extension));
        tokenPayload.put("url", fileUrl);
        tokenPayload.put("fileType", extension);
        tokenPayload.put("permissions", docConfig.getPermissions());
        String configToken = documentTokenService.generateEditorConfigToken(tokenPayload);
        vo.setToken(configToken != null ? configToken : "");

        return vo;
    }

    /**
     * 根据扩展名判断 DocumentType。
     */
    private String determineDocType(String extension) {
        return switch (extension) {
            case "docx", "doc", "odt", "rtf", "txt", "html", "htm", "mht", "mhtml",
                 "fb2", "epub", "docxf" -> "word";
            case "xlsx", "xls", "ods", "csv" -> "cell";
            case "pptx", "ppt", "odp" -> "slide";
            default -> "word";
        };
    }

    /**
     * 计算稳定的文档 key（SHA-256 取前 16 位十六进制）。
     *
     * <p>OnlyOffice 要求 document key 稳定唯一，相同文件内容产生相同 key。</p>
     */
    private String computeStableKey(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }
}
