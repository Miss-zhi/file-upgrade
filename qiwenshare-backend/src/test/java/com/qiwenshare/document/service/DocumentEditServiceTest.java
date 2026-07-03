package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.vo.EditConfigVO;
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

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DocumentEditService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class DocumentEditServiceTest {

    @Mock private UserFileRepository userFileRepository;
    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private FilePermissionService filePermissionService;
    @Mock private StorageFactory storageFactory;
    @Mock private DocumentPreviewService documentPreviewService;
    @Mock private DocumentTokenService documentTokenService;
    @Mock private OnlyOfficeProperties onlyOfficeProperties;
    @Mock private StorageBackend storageBackend;
    @Mock private DocumentEditService self;

    private DocumentEditService documentEditService;

    @BeforeEach
    void setUp() {
        documentEditService = new DocumentEditService(
                userFileRepository, fileBeanRepository, filePermissionService,
                storageFactory, documentPreviewService, documentTokenService, onlyOfficeProperties, self);
    }

    @Nested
    @DisplayName("构建编辑配置")
    class BuildEditConfig {

        @Test
        @DisplayName("有编辑权限且格式可编辑时返回 edit 模式")
        void canEdit_andEditableFormat_returnsEditConfig() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 1024L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getConvertExtensions()).thenReturn(List.of());
            when(userFileRepository.countByFileIdAndDeleteStatus(100L, 0)).thenReturn(1L);
            
            PreviewConfigVO previewConfig = createMockPreviewConfig("edit");
            when(documentPreviewService.buildConfig(eq(userFile), eq(fileBean), eq("edit"), eq(1L)))
                    .thenReturn(previewConfig);

            EditConfigVO result = documentEditService.buildEditConfig(10L, 1L);

            assertThat(result.isCowApplied()).isFalse();
            assertThat(result.getDocument()).isNotNull();
            assertThat(result.getEditorConfig()).isNotNull();
        }

        @Test
        @DisplayName("无编辑权限时抛出异常")
        void cannotEdit_throwsException() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canEdit(2L, 10L)).thenReturn(false);

            assertThatThrownBy(() -> documentEditService.buildEditConfig(10L, 2L))
                    .isInstanceOf(DocumentModuleException.class)
                    .satisfies(e -> assertThat(((DocumentModuleException) e).getErrorCode())
                            .isEqualTo(DocumentErrorCode.DOC_ACCESS_DENIED));
        }

        @Test
        @DisplayName("格式不可编辑也不可转换时降级为预??)")

        void notEditable_andNotConvertible_fallsBackToPreview() {
            UserFile userFile = createUserFile(1L, 10L, "test", "txt");
            FileBean fileBean = createFileBean(100L, 1024L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getConvertExtensions()).thenReturn(List.of());
            
            PreviewConfigVO previewConfig = createMockPreviewConfig("view");
            when(documentPreviewService.buildPreviewConfig(10L, 1L)).thenReturn(previewConfig);

            EditConfigVO result = documentEditService.buildEditConfig(10L, 1L);

            assertThat(result.getDocument()).isNotNull();
            verify(documentPreviewService).buildPreviewConfig(10L, 1L);
        }

        @Test
        @DisplayName("FileBean 被多??UserFile 引用时触??COW")
        void multipleReferences_triggersCOW() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 1024L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getConvertExtensions()).thenReturn(List.of());
            when(userFileRepository.countByFileIdAndDeleteStatus(100L, 0)).thenReturn(3L);
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageBackend.download(anyString())).thenReturn(new java.io.ByteArrayInputStream("test".getBytes()));
            FileBean copyBean = createFileBean(200L, 1024L);
            // saveCowCopy 通过 self 代理调用
            when(self.saveCowCopy(any(UserFile.class), any(FileBean.class))).thenReturn(copyBean);
            
            PreviewConfigVO previewConfig = createMockPreviewConfig("edit");
            when(documentPreviewService.buildConfig(eq(userFile), eq(copyBean), eq("edit"), eq(1L)))
                    .thenReturn(previewConfig);

            EditConfigVO result = documentEditService.buildEditConfig(10L, 1L);

            assertThat(result.isCowApplied()).isTrue();
            verify(self).saveCowCopy(eq(userFile), any(FileBean.class));
            // 验证 COW 物理文件复制被调用（S9??
            verify(storageBackend).write(anyString(), any(InputStream.class));
        }

        @Test
        @DisplayName("FileBean 仅被一??UserFile 引用时不触发 COW")
        void singleReference_noCOW() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 1024L);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
            when(onlyOfficeProperties.getEditedExtensions()).thenReturn(List.of("docx"));
            when(onlyOfficeProperties.getConvertExtensions()).thenReturn(List.of());
            when(userFileRepository.countByFileIdAndDeleteStatus(100L, 0)).thenReturn(1L);
            
            PreviewConfigVO previewConfig = createMockPreviewConfig("edit");
            when(documentPreviewService.buildConfig(eq(userFile), eq(fileBean), eq("edit"), eq(1L)))
                    .thenReturn(previewConfig);

            EditConfigVO result = documentEditService.buildEditConfig(10L, 1L);

            assertThat(result.isCowApplied()).isFalse();
            verify(userFileRepository, never()).save(any());
        }

        @Test
        @DisplayName("文件超大时抛出异??)")

        void fileTooLarge_throwsException() {
            UserFile userFile = createUserFile(1L, 10L, "test", "docx");
            FileBean fileBean = createFileBean(100L, 100L * 1024 * 1024);

            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(filePermissionService.canEdit(1L, 10L)).thenReturn(true);
            when(fileBeanRepository.findById(100L)).thenReturn(Optional.of(fileBean));
            when(onlyOfficeProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);

            assertThatThrownBy(() -> documentEditService.buildEditConfig(10L, 1L))
                    .isInstanceOf(DocumentModuleException.class)
                    .satisfies(e -> assertThat(((DocumentModuleException) e).getErrorCode())
                            .isEqualTo(DocumentErrorCode.DOC_FILE_TOO_LARGE));
        }

        @Test
        @DisplayName("文件不存在时抛出异常")
        void fileNotFound_throwsException() {
            when(userFileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentEditService.buildEditConfig(99L, 1L))
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

    private PreviewConfigVO createMockPreviewConfig(String mode) {
        PreviewConfigVO config = new PreviewConfigVO();
        config.setDocserviceApiUrl("http://localhost:8090/api.js");
        config.setToken("mock-token");
        
        PreviewConfigVO.DocumentConfig doc = new PreviewConfigVO.DocumentConfig();
        doc.setFileType("docx");
        doc.setDocType("word");
        doc.setKey("doc-key");
        doc.setTitle("test.docx");
        doc.setUrl("http://download/test.docx");
        config.setDocument(doc);
        
        PreviewConfigVO.EditorConfig editorConfig = new PreviewConfigVO.EditorConfig();
        editorConfig.setMode(mode);
        config.setEditorConfig(editorConfig);
        
        return config;
    }
}
