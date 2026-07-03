package com.qiwenshare.document.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.document.dto.EditRequestDTO;
import com.qiwenshare.document.dto.PreviewRequestDTO;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.service.DocumentHistoryService;
import com.qiwenshare.document.service.DocumentPreviewService;
import com.qiwenshare.document.service.DocumentEditService;
import com.qiwenshare.document.vo.DocumentVersionVO;
import com.qiwenshare.document.vo.EditConfigVO;
import com.qiwenshare.document.vo.PreviewConfigVO;
import com.qiwenshare.file.service.FilePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档预览/编辑/历史 API。
 */
@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentPreviewService documentPreviewService;
    private final DocumentEditService documentEditService;
    private final DocumentHistoryService documentHistoryService;
    private final FilePermissionService filePermissionService;

    /**
     * 获取文档预览配置。
     *
     * @param dto            预览请求
     * @param authentication 当前认证信息
     * @return OnlyOffice 预览配置
     */
    @PostMapping("/preview")
    public RestResult<PreviewConfigVO> preview(
            @Valid @RequestBody PreviewRequestDTO dto,
            Authentication authentication) {
        Long userId = parseUserId(authentication);
        PreviewConfigVO config = documentPreviewService.buildPreviewConfig(dto.userFileId(), userId);
        return RestResult.success(config);
    }

    /**
     * 获取文档编辑配置。
     *
     * @param dto            编辑请求
     * @param authentication 当前认证信息
     * @return OnlyOffice 编辑配置
     */
    @PostMapping("/edit")
    public RestResult<EditConfigVO> edit(
            @Valid @RequestBody EditRequestDTO dto,
            Authentication authentication) {
        Long userId = parseUserId(authentication);
        EditConfigVO config = documentEditService.buildEditConfig(dto.userFileId(), userId);
        return RestResult.success(config);
    }

    /**
     * 获取文档版本历史。
     *
     * @param userFileId     用户文件 ID
     * @param authentication 当前认证信息
     * @return 版本列表
     */
    @GetMapping("/{userFileId}/history")
    public RestResult<List<DocumentVersionVO>> history(
            @PathVariable Long userFileId,
            Authentication authentication) {
        Long userId = parseUserId(authentication);
        // 权限检查：查看权限即可查询版本历史
        if (!filePermissionService.canView(userId, userFileId)) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED);
        }
        List<DocumentVersionVO> versions = documentHistoryService.listVersions(userFileId);
        return RestResult.success(versions);
    }

    /**
     * 回滚到指定版本。
     *
     * @param userFileId     用户文件 ID
     * @param version        目标版本号
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/{userFileId}/history/{version}/restore")
    public RestResult<Void> restoreVersion(
            @PathVariable Long userFileId,
            @PathVariable int version,
            Authentication authentication) {
        Long userId = parseUserId(authentication);
        // 回滚需要编辑权限
        if (!filePermissionService.canEdit(userId, userFileId)) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED);
        }
        documentHistoryService.restoreVersion(userFileId, version, userId);
        return RestResult.success(null);
    }

    /**
     * 从 Authentication 中安全提取用户 ID。
     *
     * @throws DocumentModuleException 当 userId 非数字时
     */
    private Long parseUserId(Authentication authentication) {
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new DocumentModuleException(DocumentErrorCode.DOC_ACCESS_DENIED);
        }
    }
}
