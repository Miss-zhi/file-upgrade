package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFiletransferService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.task.UploadTask;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.file.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件传输")
@RestController
@RequestMapping("/filetransfer")
@RequiredArgsConstructor
public class FiletransferController {

    private final IFiletransferService transferService;

    @Operation(summary = "上传分片")
    @PostMapping("/upload-chunk")
    public RestResult<Void> uploadChunk(
            @RequestParam MultipartFile chunk,
            @RequestParam int chunkNum,
            @RequestParam int totalChunks,
            @RequestParam String identifier,
            @RequestParam String fileName,
            @RequestParam String filePath,
            @RequestParam long totalSize) throws Exception {
        String userId = getCurrentUserId();
        transferService.uploadChunk(identifier, chunkNum, totalChunks,
                fileName, filePath, totalSize, userId, chunk.getInputStream());
        return RestResult.success();
    }

    @Operation(summary = "合并分片")
    @PostMapping("/merge-chunks")
    public RestResult<FileVO> mergeChunks(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        FileBean file = transferService.mergeChunks(
                body.get("identifier"), body.get("filePath"), userId);
        return RestResult.success(FileVO.fromEntity(file));
    }

    @Operation(summary = "查询上传进度")
    @GetMapping("/progress/{identifier}")
    public RestResult<UploadTask> getProgress(@PathVariable String identifier) {
        return RestResult.success(transferService.getProgress(identifier));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
