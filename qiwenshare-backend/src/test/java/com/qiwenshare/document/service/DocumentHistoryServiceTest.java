package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.entity.DocumentVersion;
import com.qiwenshare.document.exception.DocumentErrorCode;
import com.qiwenshare.document.exception.DocumentModuleException;
import com.qiwenshare.document.repository.DocumentVersionRepository;
import com.qiwenshare.document.vo.DocumentVersionVO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DocumentHistoryService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class DocumentHistoryServiceTest {

    @Mock private DocumentVersionRepository documentVersionRepository;
    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private UserFileRepository userFileRepository;
    @Mock private OnlyOfficeProperties onlyOfficeProperties;

    private DocumentHistoryService documentHistoryService;

    @BeforeEach
    void setUp() {
        documentHistoryService = new DocumentHistoryService(
                documentVersionRepository, fileBeanRepository, userFileRepository, onlyOfficeProperties);
    }

    @Nested
    @DisplayName("创建版本")
    class CreateVersion {

        @Test
        @DisplayName("首次创建版本号为 1")
        void createVersion_firstVersion_numberIs1() {
            when(documentVersionRepository.findMaxVersionNumber(10L)).thenReturn(null);
            when(documentVersionRepository.countByUserFileId(10L)).thenReturn(1L);
            when(onlyOfficeProperties.getMaxVersionCount()).thenReturn(10);

            FileBean fileBean = createFileBean(100L, 1024L);
            documentHistoryService.createVersion(10L, fileBean, 1L);

            ArgumentCaptor<DocumentVersion> captor = ArgumentCaptor.forClass(DocumentVersion.class);
            verify(documentVersionRepository).save(captor.capture());
            assertThat(captor.getValue().getVersionNumber()).isEqualTo(1);
            assertThat(captor.getValue().getUserFileId()).isEqualTo(10L);
            assertThat(captor.getValue().getEditorId()).isEqualTo(1L);
            assertThat(captor.getValue().getFileSize()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("已有版本时版本号递增")
        void createVersion_existingVersions_numberIncrements() {
            when(documentVersionRepository.findMaxVersionNumber(10L)).thenReturn(3);
            when(documentVersionRepository.countByUserFileId(10L)).thenReturn(3L);
            when(onlyOfficeProperties.getMaxVersionCount()).thenReturn(10);

            FileBean fileBean = createFileBean(100L, 2048L);
            documentHistoryService.createVersion(10L, fileBean, 2L);

            ArgumentCaptor<DocumentVersion> captor = ArgumentCaptor.forClass(DocumentVersion.class);
            verify(documentVersionRepository).save(captor.capture());
            assertThat(captor.getValue().getVersionNumber()).isEqualTo(4);
        }

        @Test
        @DisplayName("超出最大保留数时删除最旧版??)")

        void createVersion_exceedsMax_deletesOldest() {
            when(documentVersionRepository.findMaxVersionNumber(10L)).thenReturn(10);
            when(documentVersionRepository.countByUserFileId(10L)).thenReturn(11L);
            when(onlyOfficeProperties.getMaxVersionCount()).thenReturn(10);

            DocumentVersion oldest = new DocumentVersion();
            oldest.setVersionId(1L);
            oldest.setVersionNumber(1);
            when(documentVersionRepository.findFirstByUserFileIdOrderByVersionNumberAsc(10L)).thenReturn(Optional.of(oldest));

            FileBean fileBean = createFileBean(100L, 512L);
            documentHistoryService.createVersion(10L, fileBean, 1L);

            verify(documentVersionRepository).delete(oldest);
        }

        @Test
        @DisplayName("未超出最大保留数时不删除")
        void createVersion_withinLimit_noDelete() {
            when(documentVersionRepository.findMaxVersionNumber(10L)).thenReturn(5);
            when(documentVersionRepository.countByUserFileId(10L)).thenReturn(5L);
            when(onlyOfficeProperties.getMaxVersionCount()).thenReturn(10);

            FileBean fileBean = createFileBean(100L, 512L);
            documentHistoryService.createVersion(10L, fileBean, 1L);

            verify(documentVersionRepository, never()).findFirstByUserFileIdOrderByVersionNumberAsc(any());
        }
    }

    @Nested
    @DisplayName("查询版本")
    class ListVersions {

        @Test
        @DisplayName("返回降序版本列表")
        void listVersions_returnsDescendingList() {
            DocumentVersion v1 = createVersion(10L, 1, 1024L, 1L);
            DocumentVersion v2 = createVersion(10L, 2, 2048L, 2L);
            when(documentVersionRepository.findByUserFileIdOrderByVersionNumberDesc(10L))
                    .thenReturn(List.of(v2, v1));

            List<DocumentVersionVO> result = documentHistoryService.listVersions(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).versionNumber()).isEqualTo(2);
            assertThat(result.get(1).versionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("无版本时返回空列??)")

        void listVersions_noVersions_returnsEmpty() {
            when(documentVersionRepository.findByUserFileIdOrderByVersionNumberDesc(10L))
                    .thenReturn(List.of());

            List<DocumentVersionVO> result = documentHistoryService.listVersions(10L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("查询指定版本")
    class GetVersion {

        @Test
        @DisplayName("版本存在时返??)")

        void getVersion_exists_returnsVersion() {
            DocumentVersion v = createVersion(10L, 1, 1024L, 1L);
            when(documentVersionRepository.findByUserFileIdAndVersionNumber(10L, 1))
                    .thenReturn(Optional.of(v));

            DocumentVersion result = documentHistoryService.getVersion(10L, 1);

            assertThat(result.getVersionNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("版本不存在时抛出异常")
        void getVersion_notFound_throwsException() {
            when(documentVersionRepository.findByUserFileIdAndVersionNumber(10L, 99))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentHistoryService.getVersion(10L, 99))
                    .isInstanceOf(DocumentModuleException.class)
                    .satisfies(e -> assertThat(((DocumentModuleException) e).getErrorCode())
                            .isEqualTo(DocumentErrorCode.DOC_VERSION_NOT_FOUND));
        }
    }

    private FileBean createFileBean(Long fileId, Long fileSize) {
        FileBean fb = new FileBean();
        fb.setFileId(fileId);
        fb.setFileSize(fileSize);
        fb.setFileHash("abc123");
        fb.setStorageType("local");
        fb.setStoragePath("path/file.docx");
        return fb;
    }

    private DocumentVersion createVersion(Long userFileId, int versionNumber, long fileSize, Long editorId) {
        DocumentVersion v = new DocumentVersion();
        v.setVersionId((long) versionNumber);
        v.setUserFileId(userFileId);
        v.setFileId(100L);
        v.setVersionNumber(versionNumber);
        v.setFileSize(fileSize);
        v.setEditorId(editorId);
        v.setCreateTime(LocalDateTime.now());
        return v;
    }
}
