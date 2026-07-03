package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.storage.factory.StorageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FileRecoveryService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FileRecoveryServiceTest {

    @Mock private UserFileRepository userFileRepository;
    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private StorageFactory storageFactory;
    @Mock private StorageQuotaService storageQuotaService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private FileRecoveryService fileRecoveryService;

    @BeforeEach
    void setUp() {
        fileRecoveryService = new FileRecoveryService(
                userFileRepository, fileBeanRepository, storageFactory, storageQuotaService, eventPublisher);
    }

    @Nested
    @DisplayName("软删??)")

    class SoftDelete {

        @Test
        @DisplayName("软删除设??deleteStatus=1 ??deleteTime")
        void softDelete_setsDeleteStatusAndTime() {
            UserFile uf = createFile(1L, 1L, "test", "txt", "/", 1, 10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));

            fileRecoveryService.softDelete(1L, 1L);

            verify(userFileRepository).save(argThat(f ->
                    f.getDeleteStatus() == 1 && f.getDeleteTime() != null && f.getDeleteBatchNum() != null));
        }

        @Test
        @DisplayName("非所有者操作抛??FILE_ACCESS_DENIED")
        void softDelete_notOwner_throwsAccessDenied() {
            UserFile uf = createFile(1L, 2L, "test", "txt", "/", 1, 10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));

            assertThatThrownBy(() -> fileRecoveryService.softDelete(1L, 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("恢复文件")
    class Restore {

        @Test
        @DisplayName("恢复已删除文件重??deleteStatus")
        void restoreFiles_success() {
            UserFile uf = createFile(1L, 1L, "test", "txt", "/", 1, 10L);
            uf.setDeleteStatus(1);
            uf.setDeleteTime(LocalDateTime.now());
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    1L, "/", "test", "txt", 0, 1)).thenReturn(false);

            fileRecoveryService.restoreFiles(List.of(1L), 1L);

            verify(userFileRepository).save(argThat(f ->
                    f.getDeleteStatus() == 0 && f.getDeleteTime() == null && f.getDeleteBatchNum() == null));
        }

        @Test
        @DisplayName("原路径存在同名文件时抛出 RECOVERY_CONFLICT")
        void restoreFiles_conflict_throwsException() {
            UserFile uf = createFile(1L, 1L, "test", "txt", "/", 1, 10L);
            uf.setDeleteStatus(1);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    1L, "/", "test", "txt", 0, 1)).thenReturn(true);

            assertThatThrownBy(() -> fileRecoveryService.restoreFiles(List.of(1L), 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.RECOVERY_CONFLICT));
        }
    }

    @Nested
    @DisplayName("永久删除")
    class PermanentDelete {

        @Test
        @DisplayName("永久删除未软删除的文件抛出异??)")

        void permanentDelete_notSoftDeleted_throwsException() {
            UserFile uf = createFile(1L, 1L, "test", "txt", "/", 1, 10L);
            uf.setDeleteStatus(0);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));

            assertThatThrownBy(() -> fileRecoveryService.permanentDelete(List.of(1L), 1L))
                    .isInstanceOf(FileModuleException.class);
        }
    }

    private UserFile createFile(Long userFileId, Long userId, String fileName, String extendName,
                                 String filePath, Integer fileType, Long fileId) {
        UserFile uf = new UserFile();
        uf.setUserFileId(userFileId);
        uf.setUserId(userId);
        uf.setFileName(fileName);
        uf.setExtendName(extendName);
        uf.setFilePath(filePath);
        uf.setFileType(fileType);
        uf.setFileId(fileId);
        uf.setDeleteStatus(0);
        return uf;
    }
}
