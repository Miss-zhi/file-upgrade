package com.qiwenshare.document.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.document.service.DocumentAdminService;
import com.qiwenshare.document.vo.DocumentHealthVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档管理端点（管理员权限）。
 */
@RestController
@RequestMapping("/api/v1/admin/document")
@RequiredArgsConstructor
public class DocumentAdminController {

    private final DocumentAdminService documentAdminService;

    /**
     * OnlyOffice 健康检查。
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('admin:document-health')")
    public RestResult<DocumentHealthVO> health() {
        return RestResult.success(documentAdminService.checkHealth());
    }
}