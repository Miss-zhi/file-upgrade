package com.qiwenshare.search.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索管理端 API 控制器。
 *
 * <p>提供全量重建索引等管理功能，需要 ADMIN 权限。</p>
 */
@RestController
@RequestMapping("/api/v1/search/admin")
@RequiredArgsConstructor
public class SearchAdminController {

    private final SearchIndexService searchIndexService;

    /**
     * 全量重建索引。
     *
     * @return 操作结果
     */
    @PostMapping("/rebuild")
    @PreAuthorize("hasAuthority('admin:search-rebuild')")
    public RestResult<String> rebuild() {
        searchIndexService.rebuildAll();
        return RestResult.success("全量重建索引完成");
    }
}
