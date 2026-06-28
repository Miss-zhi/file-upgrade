package com.qiwenshare.file.controller;

import com.qiwenshare.file.service.OnlyOfficeService;
import com.qiwenshare.file.util.RestResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "OnlyOffice")
@RestController
@RequiredArgsConstructor
public class OnlyOfficeController {

    private final OnlyOfficeService ooService;

    @Operation(summary = "获取编辑器配置")
    @GetMapping("/onlyoffice/edit/{fileId}")
    public RestResult<Map<String, Object>> getEditorConfig(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "edit") String mode) {
        String userId = getCurrentUserId();
        Map<String, Object> config = ooService.getEditorConfig(fileId, userId, mode);
        return config != null ? RestResult.success(config) : RestResult.fail("文件不存在");
    }

    @Operation(summary = "OnlyOffice 回调")
    @PostMapping("/onlyoffice/callback")
    public Map<String, Integer> callback(@RequestBody Map<String, Object> body) {
        int status = body.get("status") != null ? ((Number) body.get("status")).intValue() : 0;
        String fileId = body.get("key") instanceof String ? ((String) body.get("key")).split("_")[0] : "";
        String url = body.get("url") instanceof String ? (String) body.get("url") : "";
        ooService.handleCallback(status, fileId, url);
        return Map.of("error", 0);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (String) auth.getPrincipal() : "anonymous";
    }
}
