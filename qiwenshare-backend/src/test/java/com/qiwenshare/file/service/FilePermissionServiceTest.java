package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.ShareFileRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FilePermissionService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FilePermissionServiceTest {

    @Mock private UserFileRepository userFileRepository;
    @Mock private ShareFileRepository shareFileRepository;

    private FilePermissionServiceImpl filePermissionService;

    @BeforeEach
    void setUp() {
        filePermissionService = new FilePermissionServiceImpl(userFileRepository, shareFileRepository);
    }

    @Nested
    @DisplayName("canView")
    class CanView {

        @Test
        @DisplayName("文件所有者有查看权限")
        void owner_canView() {
            UserFile userFile = createUserFile(1L, 10L, "test.docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));

            assertThat(filePermissionService.canView(1L, 10L)).isTrue();
        }

        @Test
        @DisplayName("非所有者有有效分享时可查看")
        void nonOwner_withValidShare_canView() {
            UserFile userFile = createUserFile(1L, 10L, "test.docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));

            when(shareFileRepository.countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(1L);

            assertThat(filePermissionService.canView(2L, 10L)).isTrue();
        }

        @Test
        @DisplayName("非所有者无分享时不可查??)")

        void nonOwner_noShare_cannotView() {
            UserFile userFile = createUserFile(1L, 10L, "test.docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));
            when(shareFileRepository.countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(0L);

            assertThat(filePermissionService.canView(2L, 10L)).isFalse();
        }

        @Test
        @DisplayName("文件不存在时不可查看")
        void fileNotFound_cannotView() {
            when(userFileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(filePermissionService.canView(1L, 99L)).isFalse();
        }

        @Test
        @DisplayName("分享已过期时不可查看")
        void expiredShare_cannotView() {
            UserFile userFile = createUserFile(1L, 10L, "test.docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));

            // 已过期的分享不会??countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull 计入
            when(shareFileRepository.countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(0L);

            assertThat(filePermissionService.canView(2L, 10L)).isFalse();
        }
    }

    @Nested
    @DisplayName("canEdit")
    class CanEdit {

        @Test
        @DisplayName("文件所有者有编辑权限")
        void owner_canEdit() {
            UserFile userFile = createUserFile(1L, 10L, "test.docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));

            assertThat(filePermissionService.canEdit(1L, 10L)).isTrue();
        }

        @Test
        @DisplayName("非所有者不可编??)")

        void nonOwner_cannotEdit() {
            UserFile userFile = createUserFile(1L, 10L, "test.docx");
            when(userFileRepository.findById(10L)).thenReturn(Optional.of(userFile));

            assertThat(filePermissionService.canEdit(2L, 10L)).isFalse();
        }

        @Test
        @DisplayName("文件不存在时不可编辑")
        void fileNotFound_cannotEdit() {
            when(userFileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(filePermissionService.canEdit(1L, 99L)).isFalse();
        }
    }

    private UserFile createUserFile(Long userId, Long userFileId, String fileName) {
        UserFile uf = new UserFile();
        uf.setUserFileId(userFileId);
        uf.setUserId(userId);
        uf.setFileName(fileName);
        uf.setFileId(100L);
        uf.setDeleteStatus(0);
        return uf;
    }
}
