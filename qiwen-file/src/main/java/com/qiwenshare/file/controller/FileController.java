package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.aop.MyLog;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.dto.file.DeleteFileDTO;
import com.qiwenshare.file.dto.file.ListFileDTO;
import com.qiwenshare.ufop.UFOPFactory;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.file.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件控制器
 */
@Tag(name = "文件管理", description = "文件上传/下载/删除/列表")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;
    private final UFOPFactory ufopFactory;

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
        // 写入物理文件
        try { ufopFactory.getUploader().upload(filePath, file.getInputStream()); } catch (Exception ignored) {}
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
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable String id) {
        FileBean file = fileService.getById(id);
        if (file == null) return ResponseEntity.notFound().build();
        java.io.InputStream is = ufopFactory.getDownloader().download(file.getFilePath());
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(is);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(summary = "回收站列表")
    @PostMapping("/recycle")
    public RestResult<List<FileVO>> recycleList() {
        String userId = getCurrentUserId();
        List<FileBean> files = fileService.listDeleted(userId);
        return RestResult.success(files.stream().map(FileVO::fromEntity).toList(), files.size());
    }

    @Operation(summary = "恢复文件")
    @PostMapping("/restore")
    public RestResult<Void> restore(@RequestBody DeleteFileDTO dto) {
        String userId = getCurrentUserId();
        fileService.restore(dto.getId(), userId);
        return RestResult.success();
    }

    @Operation(summary = "彻底删除")
    @DeleteMapping("/permanent/{id}")
    public RestResult<Void> permanentDelete(@PathVariable String id) {
        String userId = getCurrentUserId();
        fileService.permanentDelete(id, userId);
        return RestResult.success();
    }

    @Operation(summary = "批量删除")
    @PostMapping("/batch-delete")
    public RestResult<Void> batchDelete(@RequestBody Map<String, List<String>> body) {
        fileService.batchDelete(body.get("ids"), getCurrentUserId());
        return RestResult.success();
    }

    @Operation(summary = "批量移动")
    @PostMapping("/batch-move")
    public RestResult<Void> batchMove(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String targetPath = (String) body.get("targetPath");
        fileService.batchMove(ids, targetPath, getCurrentUserId());
        return RestResult.success();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (String) auth.getPrincipal();
    }
}
