package com.qiwenshare.file.controller;

import com.qiwenshare.file.service.FileSearchService;
import com.qiwenshare.file.util.RestResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "全文搜索")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final FileSearchService searchService;

    @Operation(summary = "搜索文件")
    @PostMapping
    public RestResult<List<Map<String, Object>>> search(@RequestParam String keyword) {
        String userId = getCurrentUserId();
        List<Map<String, Object>> results = searchService.search(keyword, userId);
        return RestResult.success(results, results.size());
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (String) auth.getPrincipal();
    }
}
