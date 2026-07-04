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
import java.util.UUID;

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

        // 文件夹或无关联文件的记录不能预览
        if (userFile.getFileId() == null) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_FILE_NOT_FOUND);
        }

        // 查询 FileBean
        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED));

        // 大小检查
        if (fileBean.getFileSize() > onlyOfficeProperties.getMaxFileSize()) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_FILE_TOO_LARGE);
        }

        // 预览端点始终以 view 模式打开（只读）。
        // 编辑模式由 /api/v1/document/edit 端点（DocumentEditService）单独处理。
        // 与旧项目行为一致：预览 = 只读，编辑 = 可编辑。

        // 构建配置
        return buildConfig(userFile, fileBean, "view", userId);
    }

    /**
     * 构建 OnlyOffice Config 对象。
     */
    protected PreviewConfigVO buildConfig(UserFile userFile, FileBean fileBean, String mode, Long userId) {
        String extension = userFile.getExtendName() != null ? userFile.getExtendName().toLowerCase() : "";

        // 文档 key
        // OnlyOffice 会按 key 缓存文档状态（包括下载失败），所以 view 模式加随机后缀防止缓存
        // edit 模式保持同一文件同一 key 以支持多人协作
        long epochMillis = fileBean.getModifyTime() != null
                ? fileBean.getModifyTime().toInstant(ZoneOffset.UTC).toEpochMilli()
                : 0L;
        String keyInput = userFile.getUserFileId() + ":" + epochMillis;
        String baseKey = computeStableKey(keyInput);
        String documentKey;
        if ("edit".equals(mode)) {
            documentKey = baseKey;
        } else {
            // view 模式：加 UUID 后缀（16 + 1 + 3 = 20 字符，OnlyOffice 上限 20）
            String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 3);
            documentKey = baseKey + "_" + uniqueSuffix;
        }

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
                    + userFile.getUserFileId() + "?token=" + token
                    + "&_cb=" + System.currentTimeMillis();
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
        // Document Server 用此 token 验证编辑器请求来源，token 声明必须与 config 结构一致
        // （旧项目 DefaultFileConfigurer 签名为 {type, documentType, document, editorConfig}）
        Map<String, Object> documentForToken = new HashMap<>();
        documentForToken.put("title", userFile.getFileName() + (extension.isEmpty() ? "" : "." + extension));
        documentForToken.put("url", fileUrl);
        documentForToken.put("fileType", extension);
        documentForToken.put("key", documentKey);
        documentForToken.put("permissions", docConfig.getPermissions());

        Map<String, Object> editorConfigForToken = new HashMap<>();
        editorConfigForToken.put("mode", mode);
        editorConfigForToken.put("lang", "zh-CN");
        if ("edit".equals(mode) && editorConfig.getCallbackUrl() != null) {
            editorConfigForToken.put("callbackUrl", editorConfig.getCallbackUrl());
        }
        Map<String, Object> userForToken = new HashMap<>();
        userForToken.put("id", String.valueOf(userId));
        userForToken.put("name", "User " + userId);
        editorConfigForToken.put("user", userForToken);

        Map<String, Object> tokenPayload = new HashMap<>();
        tokenPayload.put("document", documentForToken);
        tokenPayload.put("editorConfig", editorConfigForToken);
        tokenPayload.put("documentType", docType);

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
