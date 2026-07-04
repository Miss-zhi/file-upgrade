package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.storage.factory.StorageFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件下载服务。
 *
 * <p>支持流式下载（&lt; 50MB）和断点续传下载（≥ 50MB，Range 请求 206）。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileDownloadService {

    private static final long RANGE_THRESHOLD = 50 * 1024 * 1024; // 50MB

    private final UserFileRepository userFileRepository;
    private final FileBeanRepository fileBeanRepository;
    private final StorageFactory storageFactory;
    private final AuditLogService auditLogService;

    /**
     * 文件下载（支持断点续传）。
     *
     * @param userFileId 用户文件 ID
     * @param userId     当前用户 ID
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     */
    public void download(Long userFileId, Long userId, HttpServletRequest request, HttpServletResponse response) {
        // 查询用户文件
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        // 权限校验
        if (!userFile.getUserId().equals(userId)) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }

        // 已删除文件不可下载
        if (userFile.getDeleteStatus() != 0) {
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }

        // 查询物理文件
        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        String storagePath = fileBean.getStoragePath();
        long fileSize = fileBean.getFileSize();

        // 构建文件名
        String fullFileName = userFile.getFileName()
                + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                        ? "." + userFile.getExtendName() : "");

        // 解析 Range 头
        String rangeHeader = request.getHeader("Range");
        boolean isRangeRequest = rangeHeader != null && rangeHeader.startsWith("bytes=");

        if (isRangeRequest) {
            handleRangeDownload(response, storagePath, fileSize, fullFileName, rangeHeader);
        } else {
            handleFullDownload(response, storagePath, fileSize, fullFileName);
        }

        // 异步记录审计日志
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        auditLogService.recordAudit(userId, userFileId, "download", ip, ua);
    }

    /**
     * 分享文件下载。
     *
     * @param userFileId 用户文件 ID
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     * @param downloaderUserId 下载者用户 ID（可能为 null）
     */
    public void downloadForShare(Long userFileId, HttpServletRequest request,
                                  HttpServletResponse response, Long downloaderUserId) {
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        String storagePath = fileBean.getStoragePath();
        long fileSize = fileBean.getFileSize();

        String fullFileName = userFile.getFileName()
                + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                        ? "." + userFile.getExtendName() : "");

        String rangeHeader = request.getHeader("Range");
        boolean isRangeRequest = rangeHeader != null && rangeHeader.startsWith("bytes=");

        if (isRangeRequest) {
            handleRangeDownload(response, storagePath, fileSize, fullFileName, rangeHeader);
        } else {
            handleFullDownload(response, storagePath, fileSize, fullFileName);
        }

        // 异步记录审计日志
        if (downloaderUserId != null) {
            String ip = request.getRemoteAddr();
            String ua = request.getHeader("User-Agent");
            auditLogService.recordAudit(downloaderUserId, userFileId, "share_download", ip, ua);
        }
    }

    /**
     * OnlyOffice 预览下载（通过 token 验证，无需 JWT 登录）。
     *
     * <p>OnlyOffice Document Server 通过此方法获取文件内容，
     * 使用文档 token 代替 JWT 认证。</p>
     *
     * @param userFileId 用户文件 ID
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     */
    public void downloadForPreview(Long userFileId, HttpServletRequest request, HttpServletResponse response) {
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        if (userFile.getDeleteStatus() != 0) {
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }

        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        String storagePath = fileBean.getStoragePath();
        long fileSize = fileBean.getFileSize();

        String fullFileName = userFile.getFileName()
                + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                        ? "." + userFile.getExtendName() : "");

        // OnlyOffice Document Server 会缓存已下载的文件，加 no-cache 头确保每次取最新内容
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        handleFullDownload(response, storagePath, fileSize, fullFileName);
    }

    /**
     * 文件预览（内联显示，支持图片缩略图）。
     *
     * <p>与 download 不同，Content-Disposition 为 inline（浏览器内显示而非下载）。
     * 当 isMin=true 且文件为图片时，生成缩略图返回。</p>
     *
     * @param userFileId 用户文件 ID
     * @param userId     当前用户 ID（可为 null，分享场景）
     * @param isMin      是否返回缩略图
     * @param response   HTTP 响应
     */
    public void preview(Long userFileId, Long userId, boolean isMin, HttpServletResponse response) {
        UserFile userFile = userFileRepository.findById(userFileId)
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        // 权限校验（userId 为 null 时跳过，用于分享场景）
        if (userId != null && !userId.equals(userFile.getUserId())) {
            throw new FileModuleException(FileErrorCode.FILE_ACCESS_DENIED);
        }

        if (userFile.getDeleteStatus() != 0) {
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }

        FileBean fileBean = fileBeanRepository.findById(userFile.getFileId())
                .orElseThrow(() -> new FileModuleException(FileErrorCode.FILE_NOT_FOUND));

        String storagePath = fileBean.getStoragePath();
        String extendName = userFile.getExtendName() != null ? userFile.getExtendName().toLowerCase() : "";

        String fullFileName = userFile.getFileName()
                + (extendName != null && !extendName.isEmpty() ? "." + extendName : "");

        String contentType = resolveContentType(fullFileName);
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "filename=\""
                + URLEncoder.encode(fullFileName, StandardCharsets.UTF_8) + "\"");

        // 图片缩略图生成
        if (isMin && isImageExtension(extendName)) {
            response.setHeader("Cache-Control", "public, max-age=86400");
            try (InputStream is = storageFactory.getBackend().download(storagePath)) {
                BufferedImage original = ImageIO.read(is);
                if (original == null) {
                    // 无法解析为图片，直接返回原始文件
                    try (InputStream origIs = storageFactory.getBackend().download(storagePath);
                         OutputStream os = response.getOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = origIs.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush();
                    }
                    return;
                }

                int targetWidth = 50;
                int targetHeight = 50;
                int origWidth = original.getWidth();
                int origHeight = original.getHeight();

                // 按较小维度等比缩放，最大 50px
                double scale = Math.min((double) targetWidth / origWidth, (double) targetHeight / origHeight);
                if (scale < 1) {
                    int scaledWidth = (int) (origWidth * scale);
                    int scaledHeight = (int) (origHeight * scale);

                    BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = scaled.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
                    g2d.dispose();

                    String formatName = "png";
                    if ("jpg".equals(extendName) || "jpeg".equals(extendName)) {
                        formatName = "jpg";
                    } else if ("gif".equals(extendName)) {
                        formatName = "gif";
                    }
                    ImageIO.write(scaled, formatName, response.getOutputStream());
                } else {
                    // 原图已经很小，直接返回
                    String formatName = "png";
                    if ("jpg".equals(extendName) || "jpeg".equals(extendName)) {
                        formatName = "jpg";
                    } else if ("gif".equals(extendName)) {
                        formatName = "gif";
                    }
                    ImageIO.write(original, formatName, response.getOutputStream());
                }
                response.getOutputStream().flush();
            } catch (Exception e) {
                log.error("缩略图生成失败: {}", storagePath, e);
                throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
            }
            return;
        }

        // 非缩略图场景：内联返回原始文件
        response.setContentLengthLong(fileBean.getFileSize());
        try (InputStream is = storageFactory.getBackend().download(storagePath);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            log.error("文件预览失败: {}", storagePath, e);
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * 批量下载（打包为 ZIP）。
     *
     * @param userFileIds 用户文件 ID 列表
     * @param userId      当前用户 ID
     * @param response    HTTP 响应
     */
    public void batchDownload(List<Long> userFileIds, Long userId, HttpServletResponse response) {
        if (userFileIds == null || userFileIds.isEmpty()) {
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }

        String zipFileName = "batch_download_" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + URLEncoder.encode(zipFileName, StandardCharsets.UTF_8) + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Long userFileId : userFileIds) {
                UserFile userFile = userFileRepository.findById(userFileId)
                        .orElse(null);
                if (userFile == null || !userFile.getUserId().equals(userId) || userFile.getDeleteStatus() != 0) {
                    continue; // 跳过无权限或已删除文件
                }

                FileBean fileBean = userFile.getFileId() != null
                        ? fileBeanRepository.findById(userFile.getFileId()).orElse(null) : null;
                if (fileBean == null) {
                    continue; // 跳过文件夹和无物理文件记录
                }

                String fullFileName = userFile.getFileName()
                        + (userFile.getExtendName() != null && !userFile.getExtendName().isEmpty()
                                ? "." + userFile.getExtendName() : "");

                zos.putNextEntry(new ZipEntry(fullFileName));
                try (InputStream is = storageFactory.getBackend().download(fileBean.getStoragePath())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                zos.closeEntry();

                // 审计日志
                auditLogService.recordAudit(userId, userFileId, "download", null, null);
            }
            zos.finish();
        } catch (FileModuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量下载失败", e);
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    private void handleFullDownload(HttpServletResponse response, String storagePath,
                                     long fileSize, String fullFileName) {
        String contentType = resolveContentType(fullFileName);
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + URLEncoder.encode(fullFileName, StandardCharsets.UTF_8) + "\"");
        response.setContentLengthLong(fileSize);
        response.setHeader("Accept-Ranges", "bytes");

        try (InputStream is = storageFactory.getBackend().download(storagePath);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            log.error("文件下载失败: {}", storagePath, e);
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    private void handleRangeDownload(HttpServletResponse response, String storagePath,
                                      long fileSize, String fullFileName, String rangeHeader) {
        // 解析 Range: bytes=start-end
        String rangeValue = rangeHeader.substring("bytes=".length());
        String[] parts = rangeValue.split("-", 2);
        long start = Long.parseLong(parts[0]);
        long end = parts[1].isEmpty() ? fileSize - 1 : Long.parseLong(parts[1]);

        if (start > end || start >= fileSize) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileSize);
            return;
        }
        if (end >= fileSize) {
            end = fileSize - 1;
        }

        long contentLength = end - start + 1;
        String contentType = resolveContentType(fullFileName);

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + URLEncoder.encode(fullFileName, StandardCharsets.UTF_8) + "\"");
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        response.setContentLengthLong(contentLength);
        response.setHeader("Accept-Ranges", "bytes");

        try (InputStream is = storageFactory.getBackend().downloadRange(storagePath, start, end);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            log.error("断点续传下载失败: {}", storagePath, e);
            throw new FileModuleException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    private String resolveContentType(String fileName) {
        String lower = fileName.toLowerCase();
        // 图片
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        // 文档
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".md")) return "text/markdown";
        if (lower.endsWith(".rtf")) return "application/rtf";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        // 视频
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".wmv")) return "video/x-ms-wmv";
        if (lower.endsWith(".flv")) return "video/x-flv";
        if (lower.endsWith(".webm")) return "video/webm";
        // 音频
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".flac")) return "audio/flac";
        if (lower.endsWith(".aac")) return "audio/aac";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".wma")) return "audio/x-ms-wma";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        // 压缩包
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/vnd.rar";
        if (lower.endsWith(".7z")) return "application/x-7z-compressed";
        if (lower.endsWith(".tar")) return "application/x-tar";
        if (lower.endsWith(".gz")) return "application/gzip";
        return "application/octet-stream";
    }

    private boolean isImageExtension(String extendName) {
        return "jpg".equals(extendName) || "jpeg".equals(extendName)
                || "png".equals(extendName) || "gif".equals(extendName)
                || "bmp".equals(extendName) || "webp".equals(extendName);
    }
}
