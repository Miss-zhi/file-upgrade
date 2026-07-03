package com.qiwenshare.document.callback;

import com.qiwenshare.document.service.DocumentHistoryService;
import com.qiwenshare.document.service.OnlyOfficeConverterClient;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.storage.factory.StorageFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * 保存回调异步写入器。
 *
 * <p>独立组件，确保 {@code @Async} 代理生效。
 * 避免 SaveCallbackHandler 自调用导致 @Async 失效（红线 #16）。</p>
 *
 * <p>保存流程：检查格式 → 需要时调 Converter API 转换 → 下载 → 写入存储 → 更新 DB。</p>
 */
@Component
@Slf4j
public class SaveCallbackAsyncWriter {

    private final StorageFactory storageFactory;
    private final FileBeanRepository fileBeanRepository;
    private final UserFileRepository userFileRepository;
    private final DocumentHistoryService documentHistoryService;
    private final OnlyOfficeConverterClient converterClient;
    /** @Lazy 自注入，确保 @Async 方法内调用 @Transactional 方法通过 AOP 代理（红线 #16） */
    private final SaveCallbackAsyncWriter self;
    /** 复用 HttpClient 连接池，避免每次下载新建（S4） */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SaveCallbackAsyncWriter(StorageFactory storageFactory,
                                   FileBeanRepository fileBeanRepository,
                                   UserFileRepository userFileRepository,
                                   DocumentHistoryService documentHistoryService,
                                   OnlyOfficeConverterClient converterClient,
                                   @Lazy SaveCallbackAsyncWriter self) {
        this.storageFactory = storageFactory;
        this.fileBeanRepository = fileBeanRepository;
        this.userFileRepository = userFileRepository;
        this.documentHistoryService = documentHistoryService;
        this.converterClient = converterClient;
        this.self = self;
    }

    /**
     * 异步执行文件下载和保存。
     *
     * <p>阶段一（无事务）：格式转换检查 → HTTP 下载 → UFOP 写入 → 计算 hash。
     * 阶段二（事务内）：更新 FileBean → 创建版本记录。</p>
     *
     * <p>格式转换逻辑与旧项目 {@code DefaultCallbackManager.processSave()} 一致：
     * 当 OnlyOffice 返回的文件格式与原始格式不同时，先调 Converter API 转换。</p>
     *
     * @param context     回调上下文
     * @param downloadUrl OnlyOffice 提供的编辑后文件下载 URL
     * @param filetype    OnlyOffice 回调中的文件格式（不含点号，如 "docx"）
     */
    @Async("fileTaskExecutor")
    public void asyncSave(CallbackContext context, String downloadUrl, String filetype) {
        Long userFileId = context.getUserFileId();
        log.info("异步保存开始: userFileId={}, downloadFiletype={}", userFileId, filetype);

        try {
            // 查询原始文件格式
            UserFile userFile = userFileRepository.findById(userFileId).orElse(null);
            if (userFile == null) {
                log.warn("文件不存在，放弃保存: userFileId={}", userFileId);
                return;
            }
            String currentExt = userFile.getExtendName() != null
                    ? userFile.getExtendName().toLowerCase() : "";
            String downloadExt = filetype != null ? filetype.toLowerCase() : currentExt;

            // 格式转换：OnlyOffice 返回的格式与原始格式不同时，先转换
            if (!currentExt.equals(downloadExt) && !currentExt.isBlank()) {
                log.info("格式不匹配，尝试转换: {} → {}, userFileId={}",
                        downloadExt, currentExt, userFileId);
                String revisionId = converterClient.generateRevisionId(downloadUrl);
                String convertedUrl = converterClient.convert(
                        downloadUrl, downloadExt, currentExt, revisionId, null);
                if (convertedUrl != null && !convertedUrl.isBlank()) {
                    downloadUrl = convertedUrl;
                    log.info("格式转换成功: userFileId={}", userFileId);
                } else {
                    log.warn("格式转换失败，使用原始下载格式 {}: userFileId={}",
                            downloadExt, userFileId);
                }
            }

            // 阶段一：下载编辑后文件
            byte[] fileData = downloadFromOnlyOffice(downloadUrl);
            if (fileData == null || fileData.length == 0) {
                log.error("下载编辑后文件失败或为空: userFileId={}", userFileId);
                return;
            }

            // 计算新文件 hash
            String newHash = computeSha256(fileData);
            long newSize = fileData.length;

            // 写入 UFOP 存储
            String storagePath = "document/" + userFileId + "/" + System.currentTimeMillis();
            storageFactory.getBackend().write(storagePath, new ByteArrayInputStream(fileData));

            // 阶段二：通过代理调用事务方法（红线 #16：禁止同类内部 @Transactional 自调用）
            self.updateFileBeanAndCreateVersion(userFileId, context.getUserId(), storagePath, newHash, newSize);

            log.info("异步保存完成: userFileId={}, newSize={}", userFileId, newSize);
        } catch (Exception e) {
            log.error("异步保存失败: userFileId={}", userFileId, e);
        }
    }

    /**
     * 从 OnlyOffice 下载编辑后的文件。
     */
    private byte[] downloadFromOnlyOffice(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.error("OnlyOffice 文件下载失败: status={}, url={}", response.statusCode(), url);
                return null;
            }
            return response.body();
        } catch (Exception e) {
            log.error("OnlyOffice 文件下载异常: url={}", url, e);
            return null;
        }
    }

    /**
     * 计算 SHA-256 hash。
     */
    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    /**
     * 事务内更新 FileBean 并创建版本记录。
     */
    @Transactional
    public void updateFileBeanAndCreateVersion(Long userFileId, Long userId,
                                                String storagePath, String newHash, long newSize) {
        UserFile userFile = userFileRepository.findById(userFileId).orElse(null);
        if (userFile == null || userFile.getFileId() == null) {
            log.warn("文件不存在: userFileId={}", userFileId);
            return;
        }

        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId()).orElse(null);
        if (fileBean == null) {
            log.warn("FileBean 不存在: fileId={}", userFile.getFileId());
            return;
        }

        // 创建版本记录（保存旧版本信息）
        documentHistoryService.createVersion(userFileId, fileBean, userId);

        // 更新 FileBean
        fileBean.setFileHash(newHash);
        fileBean.setFileSize(newSize);
        fileBean.setStoragePath(storagePath);
        fileBean.setModifyTime(LocalDateTime.now());
        fileBeanRepository.save(fileBean);

        // 更新 UserFile modifyTime
        userFile.setModifyTime(LocalDateTime.now());
        userFileRepository.save(userFile);
    }
}
