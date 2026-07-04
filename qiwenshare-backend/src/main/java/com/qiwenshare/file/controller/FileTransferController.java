package com.qiwenshare.file.controller;

import com.qiwenshare.auth.common.RestResult;
import com.qiwenshare.file.dto.ChunkUploadDTO;
import com.qiwenshare.file.dto.ChunkUploadInitDTO;
import com.qiwenshare.file.dto.SpeedUploadDTO;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.service.FileDownloadService;
import com.qiwenshare.file.service.FileUploadService;
import com.qiwenshare.file.service.StorageQuotaService;
import com.qiwenshare.file.vo.QuotaInfoVO;
import com.qiwenshare.file.vo.UploadFileVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传/下载控制器。
 *
 * <p>处理普通上传、秒传、分片上传、文件下载。</p>
 */
@RestController
@RequestMapping("/api/v1/filetransfer")
@RequiredArgsConstructor
@Slf4j
public class FileTransferController {

    private final FileUploadService fileUploadService;
    private final FileDownloadService fileDownloadService;
    private final StorageQuotaService storageQuotaService;

    /**
     * 普通上传（≤10MB）。
     *
     * @param file           上传文件
     * @param filePath       目标目录路径
     * @param authentication 当前认证信息
     * @return 上传结果
     */
    @PostMapping("/upload")
    public RestResult<UploadFileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filePath", defaultValue = "/") String filePath,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UploadFileVO result = fileUploadService.uploadFile(file, filePath, userId);
        return RestResult.success(result);
    }

    /**
     * 秒传。
     *
     * <p>任何失败（未命中 hash、同名冲突、配额不足）都降级为普通上传回退信号，
     * 不再向上传抛出异常，避免阻断前端上传流程。</p>
     *
     * @param dto            秒传请求体
     * @param authentication 当前认证信息
     * @return 上传结果（data 为 null 表示秒传未成功，前端应走普通上传）
     */
    @PostMapping("/upload/speed")
    public RestResult<UploadFileVO> speedUpload(
            @Valid @RequestBody SpeedUploadDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        try {
            UploadFileVO result = fileUploadService.speedUpload(dto, userId);
            if (result == null) {
                return RestResult.success("秒传未命中，请走普通上传", null);
            }
            return RestResult.success(result);
        } catch (FileModuleException e) {
            log.info("秒传降级为普通上传: code={}, msg={}", e.getErrorCode(), e.getMessage());
            return RestResult.success("秒传失败，请走普通上传", null);
        }
    }

    /**
     * 初始化分片上传。
     *
     * @param dto            分片上传初始化请求
     * @param authentication 当前认证信息
     * @return 任务 ID
     */
    @PostMapping("/upload/chunk/init")
    public RestResult<String> initChunkUpload(
            @Valid @RequestBody ChunkUploadInitDTO dto,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        String taskId = fileUploadService.initChunkUpload(dto, userId);
        return RestResult.success(taskId);
    }

    /**
     * 上传分片。
     *
     * @param taskId     任务 ID
     * @param chunkIndex 分片序号
     * @param chunkData  分片数据
     * @return 上传结果
     */
    @PostMapping("/upload/chunk")
    public RestResult<Void> uploadChunk(
            @RequestParam("taskId") String taskId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunkData") MultipartFile chunkData) {
        ChunkUploadDTO dto = new ChunkUploadDTO(taskId, chunkIndex);
        fileUploadService.uploadChunk(dto, chunkData);
        return RestResult.success("分片上传成功");
    }

    /**
     * 合并分片。
     *
     * @param taskId         任务 ID
     * @param filePath       目标目录路径
     * @param authentication 当前认证信息
     * @return 上传结果
     */
    @PostMapping("/upload/chunk/merge")
    public RestResult<UploadFileVO> mergeChunks(
            @RequestParam("taskId") String taskId,
            @RequestParam(value = "filePath", defaultValue = "/") String filePath,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UploadFileVO result = fileUploadService.mergeChunks(taskId, filePath, userId);
        return RestResult.success(result);
    }

    /**
     * 文件下载（支持断点续传）。
     *
     * @param userFileId 用户文件 ID
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     * @param authentication 当前认证信息
     */
    @GetMapping("/download/{userFileId}")
    public void download(
            @PathVariable Long userFileId,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileDownloadService.download(userFileId, userId, request, response);
    }

    /**
     * 文件预览（内联显示，支持图片缩略图）。
     *
     * <p>与 download 不同，响应头为 Content-Disposition: inline，
     * 浏览器直接显示文件内容而非下载。当 isMin=true 时返回图片缩略图。</p>
     *
     * @param userFileId     用户文件 ID
     * @param isMin          是否返回缩略图（默认 false）
     * @param response       HTTP 响应
     * @param authentication 当前认证信息
     */
    @GetMapping("/preview/{userFileId}")
    public void preview(
            @PathVariable Long userFileId,
            @RequestParam(value = "isMin", defaultValue = "false") boolean isMin,
            HttpServletResponse response) {
        Long userId = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            userId = Long.parseLong(auth.getName());
        }
        fileDownloadService.preview(userFileId, userId, isMin, response);
    }

    /**
     * 批量下载（打包为 ZIP）。
     *
     * @param userFileIds    用户文件 ID 列表
     * @param response       HTTP 响应
     * @param authentication 当前认证信息
     */
    @PostMapping("/batch-download")
    public void batchDownload(
            @RequestBody List<Long> userFileIds,
            HttpServletResponse response,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        fileDownloadService.batchDownload(userFileIds, userId, response);
    }

    /**
     * 获取用户存储容量信息。
     *
     * @param authentication 当前认证信息
     * @return 存储容量信息
     */
    @GetMapping("/getstorage")
    public RestResult<StorageCapacityVO> getStorage(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        QuotaInfoVO quotaInfo = storageQuotaService.getQuotaInfo(userId);
        // 适配前端期望的字段名
        StorageCapacityVO vo = new StorageCapacityVO(quotaInfo.usedSize(), quotaInfo.totalQuota());
        return RestResult.success(vo);
    }

    /**
     * 存储容量响应 VO（适配前端字段名）。
     */
    public record StorageCapacityVO(
            Long storageSize,
            Long totalStorageSize
    ) {}
}
