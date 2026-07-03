package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.storage.factory.StorageFactory;
import com.qiwenshare.storage.interfaces.StorageBackend;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FileDownloadService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FileDownloadServiceTest {

    @Mock private UserFileRepository userFileRepository;
    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private StorageFactory storageFactory;
    @Mock private AuditLogService auditLogService;
    @Mock private StorageBackend storageBackend;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private FileDownloadService fileDownloadService;

    @BeforeEach
    void setUp() {
        fileDownloadService = new FileDownloadService(
                userFileRepository, fileBeanRepository, storageFactory, auditLogService);
    }

    @Nested
    @DisplayName("权限校验")
    class AccessControl {

        @Test
        @DisplayName("文件不存在时抛出 FILE_NOT_FOUND")
        void download_fileNotFound_throwsException() {
            when(userFileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileDownloadService.download(999L, 1L, request, response))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("非文件所有者访问时抛出 FILE_ACCESS_DENIED")
        void download_notOwner_throwsAccessDenied() {
            UserFile userFile = new UserFile();
            userFile.setUserFileId(1L);
            userFile.setUserId(2L); // 不同用户
            userFile.setDeleteStatus(0);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(userFile));

            assertThatThrownBy(() -> fileDownloadService.download(1L, 1L, request, response))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_ACCESS_DENIED));
        }

        @Test
        @DisplayName("已删除文件不可下??)")

        void download_deletedFile_throwsNotFound() {
            UserFile userFile = new UserFile();
            userFile.setUserFileId(1L);
            userFile.setUserId(1L);
            userFile.setDeleteStatus(1);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(userFile));

            assertThatThrownBy(() -> fileDownloadService.download(1L, 1L, request, response))
                    .isInstanceOf(FileModuleException.class);
        }
    }

    @Nested
    @DisplayName("流式下载")
    class StreamDownload {

        @Test
        @DisplayName("正常下载设置正确的响应头")
        void download_success_setsCorrectHeaders() throws Exception {
            UserFile userFile = createUserFile(1L, 1L, "test.txt", "txt", "/");
            FileBean fileBean = createFileBean(10L, 1024L, "hash1", "local", "path/test.txt");

            when(userFileRepository.findById(1L)).thenReturn(Optional.of(userFile));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(fileBean));
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageBackend.download("path/test.txt"))
                    .thenReturn(new ByteArrayInputStream("hello".getBytes()));
            when(request.getHeader("Range")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(response.getOutputStream()).thenReturn(new MockServletOutputStream());

            fileDownloadService.download(1L, 1L, request, response);

            verify(response).setContentType("text/plain");
            verify(response).setContentLengthLong(1024L);
            verify(auditLogService).recordAudit(eq(1L), eq(1L), eq("download"), anyString(), any());
        }
    }

    @Nested
    @DisplayName("分享下载")
    class ShareDownload {

        @Test
        @DisplayName("分享文件下载记录 action ??share_download")
        void downloadForShare_recordsAuditWithShareAction() throws Exception {
            UserFile userFile = createUserFile(1L, 2L, "shared.pdf", "pdf", "/");
            FileBean fileBean = createFileBean(10L, 2048L, "hash2", "local", "path/shared.pdf");

            when(userFileRepository.findById(1L)).thenReturn(Optional.of(userFile));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(fileBean));
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageBackend.download("path/shared.pdf"))
                    .thenReturn(new ByteArrayInputStream("pdf content".getBytes()));
            when(request.getHeader("Range")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");
            when(response.getOutputStream()).thenReturn(new MockServletOutputStream());

            fileDownloadService.downloadForShare(1L, request, response, 5L);

            verify(auditLogService).recordAudit(eq(5L), eq(1L), eq("share_download"), anyString(), any());
        }
    }

    private UserFile createUserFile(Long userFileId, Long userId, String fileName, String extendName, String filePath) {
        UserFile uf = new UserFile();
        uf.setUserFileId(userFileId);
        uf.setUserId(userId);
        uf.setFileId(10L);
        uf.setFileName(fileName.replace("." + extendName, ""));
        uf.setExtendName(extendName);
        uf.setFilePath(filePath);
        uf.setFileType(1);
        uf.setDeleteStatus(0);
        return uf;
    }

    private FileBean createFileBean(Long fileId, Long fileSize, String fileHash, String storageType, String storagePath) {
        FileBean fb = new FileBean();
        fb.setFileId(fileId);
        fb.setFileSize(fileSize);
        fb.setFileHash(fileHash);
        fb.setStorageType(storageType);
        fb.setStoragePath(storagePath);
        return fb;
    }

    /**
     * 简化的 Mock ServletOutputStream??
     */
    private static class MockServletOutputStream extends jakarta.servlet.ServletOutputStream {
        private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        @Override
        public boolean isReady() { return true; }

        @Override
        public void setWriteListener(jakarta.servlet.WriteListener listener) {}

        @Override
        public void write(int b) { baos.write(b); }

        @Override
        public void write(byte[] b, int off, int len) { baos.write(b, off, len); }
    }
}
