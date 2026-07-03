package com.qiwenshare.file.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.file.dto.BatchDeleteFileDTO;
import com.qiwenshare.file.dto.DeleteFileDTO;
import com.qiwenshare.file.dto.RestoreFileDTO;
import com.qiwenshare.file.service.FileRecoveryService;
import com.qiwenshare.file.vo.FileListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站控制器。
 *
 * <p>处理软删除、恢复、永久删除。</p>
 */
@RestController
@RequestMapping("/api/v1/recycle")
@RequiredArgsConstructor
public class RecoveryFileController {

    private final FileRecoveryService fileRecoveryService;

    /**
     * 回收站列表。
     *
     * @param page           页码
     * @param size           每页大小
     * @param authentication 当前认证信息
     * @return 回收站文件列表
     */
    @GetMapping("/list")
    public RestResult<Page<FileListVO>> listRecycleBin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Page<FileListVO> result = fileRecoveryService.listRecycleBin(userId, page, size);
        return RestResult.success(result);
    }

    /**
     * 软删除文件。
     *
     * @param dto            删除请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/deletefile")
    public RestResult<Void> deleteFile(
            @Valid @RequestBody DeleteFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileRecoveryService.softDelete(dto.userFileId(), userId);
        return RestResult.success("删除成功，文件已移至回收站");
    }

    /**
     * 批量软删除文件。
     *
     * @param dto            批量删除请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/batchdeletefile")
    public RestResult<Void> batchDeleteFile(
            @Valid @RequestBody BatchDeleteFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileRecoveryService.batchSoftDelete(dto.userFileIds(), userId);
        return RestResult.success("批量删除成功，文件已移至回收站");
    }

    /**
     * 恢复文件。
     *
     * @param dto            恢复请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/restorefile")
    public RestResult<Void> restoreFile(
            @Valid @RequestBody RestoreFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileRecoveryService.restoreFiles(dto.userFileIds(), userId);
        return RestResult.success("恢复成功");
    }

    /**
     * 永久删除文件。
     *
     * @param dto            删除请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/deletepermanent")
    public RestResult<Void> deletePermanent(
            @Valid @RequestBody RestoreFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileRecoveryService.permanentDelete(dto.userFileIds(), userId);
        return RestResult.success("永久删除成功");
    }

    /**
     * 清空回收站（永久删除所有已删除文件）。
     *
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/deleteall")
    public RestResult<Void> deleteAll(
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileRecoveryService.deleteAllRecycleBin(userId);
        return RestResult.success("回收站已清空");
    }
}
