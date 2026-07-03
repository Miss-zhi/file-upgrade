package com.qiwenshare.document.controller;

import com.qiwenshare.document.service.DocumentTokenService;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.service.FileDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OnlyOffice 文档预览下载控制器。
 *
 * <p>提供公开端点供 OnlyOffice Document Server 下载文件内容，
 * 通过 URL 中的 token 参数验证，不使用 JWT 认证。</p>
 */
@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final DocumentTokenService documentTokenService;
    private final FileDownloadService fileDownloadService;

    /**
     * OnlyOffice 预览文件下载。
     *
     * @param userFileId 用户文件 ID
     * @param token      文档预览 token
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     */
    @GetMapping("/download/{userFileId}")
    public void previewDownload(
            @PathVariable Long userFileId,
            @RequestParam String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        // 验证 token
        var claims = documentTokenService.parseDocumentToken(token);
        if (claims == null) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }
        fileDownloadService.downloadForPreview(userFileId, request, response);
    }
}
