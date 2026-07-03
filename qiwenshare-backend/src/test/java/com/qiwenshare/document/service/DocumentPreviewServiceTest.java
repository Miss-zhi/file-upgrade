package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.vo.PreviewConfigVO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.service.FilePermissionService;
import com.qiwenshare.storage.factory.StorageFactory;
import com.qiwenshare.storage.interfaces.StorageBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DocumentPreviewService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class DocumentPreviewServiceTest {

    @Mock private UserFileRepository userFileRepository;
    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private FilePermissionService filePermissionService;
    @Mock private StorageFactory storageFactory;
    @Mock private DocumentTokenService documentTokenService;
    @Mock private OnlyOfficeProperties onlyOfficeProperties;
    @Mock private StorageBackend storageBackend;

    private DocumentPreviewService documentPreviewService;

    @BeforeEach
    void setUp() {
        documentPreviewService = new DocumentPreviewService(
                userFileRepository, fileBeanRepository, filePermissionService,
                storageFactory, documentTokenService, onlyOfficeProperties);
    }

    @Nested
    @DisplayName("构建预览配置")
    class BuildPreviewConfig {

        @Test
        @DisplayName("文件所有者预览可编辑格式返回 edit 模式")
        void owner_editableFormat_modeIsEdit() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 1024L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canView(1L, 10L)).thenReturn(true);
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getApiUrl()).thenReturn("http://localhost:8090/api.js");
            when(onlyOfficeProperties.getCallbackBaseUrl()).thenReturn("http://localhost:8080/callback");
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageBackend.getPreviewUrl(anyString())).thenReturn("http://download/test.docx");
            when(documentTokenService.generateCallbackToken(anyString(), anyLong(), anyString())).thenReturn("cb-token");
            when(documentTokenService.generateEditorConfigToken(anyMap())).thenReturn("oo-editor-token");

            PreviewConfigVO result = documentPreviewService.buildPreviewConfig(10L, 1L);

            assertThat(result.getEditorConfig().getMode()).isEqualTo("edit");
            assertThat(result.getDocument().getFileType()).isEqualTo("docx");
            assertThat(result.getDocument().getDocType()).isEqualTo("word");
            assertThat(result.getToken()).isEqualTo("oo-editor-token");
        }

        @Test
        @DisplayName("无编辑权限时返回 view 模式")
        void noEditPermission_modeIsView() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 1024L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canView(2L, 10L)).thenReturn(true);
            when(filePermissionService.canEdit(2L, 10L)).thenReturn(false);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getApiUrl()).thenReturn("http://localhost:8090/api.js");
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageBackend.getPreviewUrl(anyString())).thenReturn("http://download/test.docx");
            when(documentTokenService.generateEditorConfigToken(anyMap())).thenReturn("oo-editor-token");

            PreviewConfigVO result = documentPreviewService.buildPreviewConfig(10L, 2L);

            assertThat(result.getEditorConfig().getMode()).isEqualTo("view");
        }

        @Test
        @DisplayName("PDF 格式强制 view 模式")
        void pdfFormat_modeIsView() {
            UserFile userFile = createUserFile(1L, 10L, "report", "pdf");
            FileBean fileBean = createFileBean(100L, 2048L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canView(1L, 10L)).thenReturn(true);
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getApiUrl()).thenReturn("http://localhost:8090/api.js");
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageBackend.getPreviewUrl(anyString())).thenReturn("http://download/report.pdf");
            when(documentTokenService.generateEditorConfigToken(anyMap())).thenReturn("oo-editor-token");

            PreviewConfigVO result = documentPreviewService.buildPreviewConfig(10L, 1L);

            assertThat(result.getEditorConfig().getMode()).isEqualTo("view");
            assertThat(result.getDocument().getDocType()).isEqualTo("word");
        }

        @Test
        @DisplayName("无查看权限时抛出异常")
        void noViewPermission_throwsException() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canView(2L, 10L)).thenReturn(false);

            assertThatThrownBy(() -> documentPreviewService.buildPreviewConfig(10L, 2L))
                    .isInstanceOf(DocumentModuleException.class)
                    .satisfies(e -> assertThat(((DocumentModuleException) e).getErrorCode())
                            .isEqualTo(DocumentErrorCode.DOC_ACCESS_DENIED));
        }

        @Test
        @DisplayName("文件超大时抛出异??)")

        void fileTooLarge_throwsException() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 100L * 1024 * 1024);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canView(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);

            assertThatThrownBy(() -> documentPreviewService.buildPreviewConfig(10L, 1L))
                    .isInstanceOf(DocumentModuleException.class)
                    .satisfies(e -> assertThat(((DocumentModuleException) e).getErrorCode())
                            .isEqualTo(DocumentErrorCode.DOC_FILE_TOO_LARGE));
        }

        @Test
        @DisplayName("文件不存在时抛出异常")
        void fileNotFound_throwsException() {
            when(userFileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentPreviewService.buildPreviewConfig(99L, 1L))
                    .isInstanceOf(DocumentModuleException.class);
        }
    }

    private UserFile createUserFile(Long userId, Long userFileId, String fileName, String ext) {
        UserFile uf = new UserFile();
        uf.setUserFileId(userFileId);
        uf.setUserId(userId);
        uf.setFileName(fileName);
        uf.setExtendName(ext);
        uf.setFileId(100L);
        uf.setDeleteStatus(0);
        return uf;
    }

    private FileBean createFileBean(Long fileId, Long fileSize) {
        FileBean fb = new FileBean();
        fb.setFileId(fileId);
        fb.setFileSize(fileSize);
        fb.setFileHash("abc123");
        fb.setStorageType("local");
        fb.setStoragePath("path/" + fileId);
        return fb;
    }
}
