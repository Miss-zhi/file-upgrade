package com.qiwenshare.file.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.file.common.FileCategory;
import com.qiwenshare.file.dto.*;
import com.qiwenshare.file.service.FileOperationService;
import com.qiwenshare.file.vo.BatchOperationResultVO;
import com.qiwenshare.file.vo.FileDetailVO;
import com.qiwenshare.file.vo.FileListVO;
import com.qiwenshare.file.vo.TreeNodeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件 CRUD 操作控制器。
 *
 * <p>处理文件列表、重命名、移动、复制、创建文件夹、文件树等。</p>
 */
@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileOperationService fileOperationService;

    /**
     * 文件列表查询。
     *
     * @param dto            查询参数
     * @param authentication 当前认证信息
     * @return 分页文件列表
     */
    @GetMapping("/getfilelist")
    public RestResult<Page<FileListVO>> listFiles(
            @Valid FileListDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Page<FileListVO> result = fileOperationService.listFiles(userId, dto);
        return RestResult.success(result);
    }

    /**
     * 按文件分类浏览（image/document/video/audio/archive）。
     *
     * @param category       分类名（image/document/video/audio/archive/other）
     * @param page           页码
     * @param size           每页大小
     * @param authentication 当前认证信息
     * @return 分页文件列表
     */
    @GetMapping("/getfilelist/bycategory")
    public RestResult<Page<FileListVO>> listFilesByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Page<FileListVO> result = fileOperationService.listFilesByCategory(userId, category, page, size);
        return RestResult.success(result);
    }

    /**
     * 重命名文件。
     *
     * @param dto            重命名请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/renamefile")
    public RestResult<Void> renameFile(
            @Valid @RequestBody RenameFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileOperationService.renameFile(dto, userId);
        return RestResult.success("重命名成功");
    }

    /**
     * 移动文件。
     *
     * @param dto            移动请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/movefile")
    public RestResult<Void> moveFile(
            @Valid @RequestBody MoveFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileOperationService.moveFile(dto, userId);
        return RestResult.success("移动成功");
    }

    /**
     * 批量移动文件。
     *
     * @param dto            批量移动请求
     * @param authentication 当前认证信息
     * @return 批量操作结果
     */
    @PostMapping("/batchmovefile")
    public RestResult<BatchOperationResultVO> batchMoveFile(
            @Valid @RequestBody BatchMoveFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        BatchOperationResultVO result = fileOperationService.batchMoveFile(dto, userId);
        return RestResult.success(result);
    }

    /**
     * 复制文件。
     *
     * @param dto            复制请求
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/copyfile")
    public RestResult<Void> copyFile(
            @Valid @RequestBody CopyFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileOperationService.copyFile(dto, userId);
        return RestResult.success("复制成功");
    }

    /**
     * 批量复制文件。
     *
     * @param dto            批量复制请求
     * @param authentication 当前认证信息
     * @return 批量操作结果
     */
    @PostMapping("/batchcopyfile")
    public RestResult<BatchOperationResultVO> batchCopyFile(
            @Valid @RequestBody BatchCopyFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        BatchOperationResultVO result = fileOperationService.batchCopyFile(dto, userId);
        return RestResult.success(result);
    }

    /**
     * 创建文件夹。
     *
     * @param dto            创建文件夹请求
     * @param authentication 当前认证信息
     * @return 文件夹 ID
     */
    @PostMapping("/createfold")
    public RestResult<Long> createFolder(
            @Valid @RequestBody CreateFoldDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Long folderId = fileOperationService.createFolder(dto, userId);
        return RestResult.success(folderId);
    }

    /**
     * 创建空文件。
     *
     * @param dto            创建文件请求
     * @param authentication 当前认证信息
     * @return 文件 ID
     */
    @PostMapping("/createfile")
    public RestResult<Long> createFile(
            @Valid @RequestBody CreateFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        Long fileId = fileOperationService.createFile(dto, userId);
        return RestResult.success(fileId);
    }

    /**
     * 文件详情。
     *
     * @param userFileId     文件 ID
     * @param authentication 当前认证信息
     * @return 文件详情
     */
    @GetMapping("/getfiledetail/{userFileId}")
    public RestResult<FileDetailVO> getFileDetail(
            @PathVariable Long userFileId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        FileDetailVO detail = fileOperationService.getFileDetail(userFileId, userId);
        return RestResult.success(detail);
    }

    /**
     * 文件树（仅文件夹层级结构）。
     *
     * @param authentication 当前认证信息
     * @return 文件树
     */
    @GetMapping("/getfiletree")
    public RestResult<List<TreeNodeVO>> getFileTree(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<TreeNodeVO> tree = fileOperationService.getFileTree(userId);
        return RestResult.success(tree);
    }

    /**
     * 修改文件文本内容（代码/文本文件在线编辑保存）。
     *
     * @param dto            修改请求（userFileId + fileContent）
     * @param authentication 当前认证信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public RestResult<Void> updateFile(
            @Valid @RequestBody UpdateFileDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileOperationService.updateFileContent(dto, userId);
        return RestResult.success("修改文件成功");
    }
}
