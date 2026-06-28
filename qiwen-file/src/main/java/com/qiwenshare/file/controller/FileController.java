package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.aop.MyLog;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.dto.file.DeleteFileDTO;
import com.qiwenshare.file.dto.file.ListFileDTO;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.file.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件控制器
 */
@Tag(name = "文件管理", description = "文件上传/下载/删除/列表")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;

    @Operation(summary = "文件列表")
    @PostMapping("/list")
    public RestResult<List<FileVO>> list(@Valid @RequestBody ListFileDTO dto) {
        String userId = getCurrentUserId();
        List<FileBean> files = fileService.listByPath(dto.getPath(), userId);
        List<FileVO> voList = files.stream().map(FileVO::fromEntity).toList();
        return RestResult.success(voList, voList.size());
    }

    @Operation(summary = "上传文件")
    @MyLog(module = "文件管理", value = "文件上传")
    @PostMapping("/upload")
    public RestResult<FileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "/") String path) {
        String userId = getCurrentUserId();
        String filePath = path.endsWith("/") ? path + file.getOriginalFilename() : path + "/" + file.getOriginalFilename();
        FileBean fileBean = fileService.upload(
                file.getOriginalFilename(), filePath, file.getSize(),
                file.getContentType(), userId);
        return RestResult.success(FileVO.fromEntity(fileBean));
    }

    @Operation(summary = "删除文件")
    @MyLog(module = "文件管理", value = "文件删除")
    @PostMapping("/delete")
    public RestResult<Void> delete(@RequestBody DeleteFileDTO dto) {
        String userId = getCurrentUserId();
        fileService.delete(dto.getId(), userId);
        return RestResult.success();
    }

    @Operation(summary = "创建文件夹")
    @MyLog(module = "文件管理", value = "创建文件夹")
    @PostMapping("/create-folder")
    public RestResult<FileVO> createFolder(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam String folderName) {
        String userId = getCurrentUserId();
        FileBean folder = fileService.createFolder(path, folderName, userId);
        return RestResult.success(FileVO.fromEntity(folder));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download/{id}")
    public RestResult<FileVO> download(@PathVariable String id) {
        FileBean file = fileService.getById(id);
        if (file == null) {
            return RestResult.fail("文件不存在");
        }
        return RestResult.success(FileVO.fromEntity(file));
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (String) auth.getPrincipal();
    }
}
