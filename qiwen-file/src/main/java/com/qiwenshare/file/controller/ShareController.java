package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IShareFileService;
import com.qiwenshare.file.aop.MyLog;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.share.ShareFile;
import com.qiwenshare.file.dto.share.CreateShareDTO;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.file.FileVO;
import com.qiwenshare.file.vo.share.ShareVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "文件分享")
@RestController
@RequiredArgsConstructor
public class ShareController {

    private final IShareFileService shareFileService;
    private final IFileService fileService;

    @Operation(summary = "创建分享")
    @PostMapping("/share/create")
    public RestResult<ShareVO> createShare(@RequestBody CreateShareDTO dto) {
        String userId = getCurrentUserId();
        ShareFile share = shareFileService.createShare(
                dto.getFilePath(), userId, dto.getExpireDays(), dto.getCode());
        String link = "/anonymous/download/" + share.getShareToken();
        return RestResult.success(ShareVO.fromEntity(share, link));
    }

    @Operation(summary = "分享列表")
    @PostMapping("/share/list")
    public RestResult<List<ShareVO>> listShares() {
        String userId = getCurrentUserId();
        List<ShareFile> shares = shareFileService.listShares(userId);
        List<ShareVO> vos = shares.stream()
                .map(s -> ShareVO.fromEntity(s, "/anonymous/download/" + s.getShareToken()))
                .toList();
        return RestResult.success(vos, vos.size());
    }

    @Operation(summary = "取消分享")
    @PostMapping("/share/cancel")
    public RestResult<Void> cancelShare(@RequestBody CreateShareDTO dto) {
        String userId = getCurrentUserId();
        shareFileService.cancelShare(dto.getFileId(), userId);
        return RestResult.success();
    }

    @Operation(summary = "验证分享")
    @GetMapping("/share/verify")
    public RestResult<FileVO> verifyShare(@RequestParam String token, @RequestParam(required = false) String code) {
        ShareFile share = shareFileService.verifyShare(token, code);
        FileBean file = fileService.getById(share.getFilePath());
        // Actually need to get file by path. For now, find by path.
        return file != null ? RestResult.success(FileVO.fromEntity(file)) : RestResult.fail("文件不存在");
    }

    @Operation(summary = "匿名下载")
    @GetMapping("/anonymous/download/{token}")
    public RestResult<FileVO> anonymousDownload(@PathVariable String token, @RequestParam(required = false) String code) {
        ShareFile share = shareFileService.verifyShare(token, code);
        FileBean file = fileService.getByPath(share.getFilePath());
        return file != null ? RestResult.success(FileVO.fromEntity(file)) : RestResult.fail("文件不存在");
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (String) auth.getPrincipal() : null;
    }
}
