package com.qiwenshare.file.service;

import com.qiwenshare.file.dto.CreateFoldDTO;
import com.qiwenshare.file.dto.RenameFileDTO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.FileDetailVO;
import com.qiwenshare.file.vo.FileListVO;
import com.qiwenshare.file.vo.TreeNodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.qiwenshare.storage.factory.StorageFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FileOperationService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FileOperationServiceTest {

    @Mock private UserFileRepository userFileRepository;
    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private StorageQuotaService storageQuotaService;
    @Mock private StorageFactory storageFactory;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private FileOperationService fileOperationService;

    @BeforeEach
    void setUp() {
        fileOperationService = new FileOperationService(
                userFileRepository, fileBeanRepository, storageQuotaService, storageFactory, eventPublisher);
    }

    @Nested
    @DisplayName("文件列表查询")
    class ListFiles {

        @Test
        @DisplayName("按目录查询返回文件列??)")

        void listFiles_byPath_returnsFileList() {
            UserFile uf = createFile(1L, 1L, "doc", "pdf", "/docs", 1, 10L);
            FileBean fb = new FileBean();
            fb.setFileId(10L);
            fb.setFileSize(1024L);
            when(userFileRepository.findByUserIdAndFilePathAndDeleteStatus(eq(1L), eq("/"), eq(0), any()))
                    .thenReturn(new PageImpl<>(List.of(uf)));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(fb));

            var dto = new com.qiwenshare.file.dto.FileListDTO("/", null, 0, 20, "fileName", "asc");
            Page<FileListVO> result = fileOperationService.listFiles(1L, dto);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).fileName()).isEqualTo("doc.pdf");
        }
    }

    @Nested
    @DisplayName("重命??)")

    class Rename {

        @Test
        @DisplayName("重命名成功更新文件名")
        void renameFile_success() {
            UserFile uf = createFile(1L, 1L, "old", "txt", "/", 1, 10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    1L, "/", "new", "txt", 0, 1)).thenReturn(false);

            fileOperationService.renameFile(new RenameFileDTO(1L, "new.txt"), 1L);

            verify(userFileRepository).save(argThat(f ->
                    f.getFileName().equals("new") && f.getExtendName().equals("txt")));
        }

        @Test
        @DisplayName("同名文件存在时抛??FILE_NAME_DUPLICATE")
        void renameFile_duplicateName_throwsException() {
            UserFile uf = createFile(1L, 1L, "old", "txt", "/", 1, 10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    1L, "/", "dup", "txt", 0, 1)).thenReturn(true);

            assertThatThrownBy(() -> fileOperationService.renameFile(new RenameFileDTO(1L, "dup.txt"), 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_NAME_DUPLICATE));
        }
    }

    @Nested
    @DisplayName("创建文件??)")

    class CreateFolder {

        @Test
        @DisplayName("创建文件夹返回文件夹 ID")
        void createFolder_success() {
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    1L, "/", "NewFolder", "", 0, 2)).thenReturn(false);
            when(userFileRepository.save(any(UserFile.class))).thenAnswer(inv -> {
                UserFile uf = inv.getArgument(0);
                uf.setUserFileId(50L);
                return uf;
            });

            Long folderId = fileOperationService.createFolder(new CreateFoldDTO("NewFolder", "/"), 1L);

            assertThat(folderId).isEqualTo(50L);
            verify(userFileRepository).save(argThat(f ->
                    f.getFileName().equals("NewFolder") && f.getFileType() == 2));
        }
    }

    @Nested
    @DisplayName("文件详情")
    class FileDetail {

        @Test
        @DisplayName("获取文件详情包含 FileBean 信息")
        void getFileDetail_success() {
            UserFile uf = createFile(1L, 1L, "report", "pdf", "/docs", 1, 10L);
            FileBean fb = new FileBean();
            fb.setFileId(10L);
            fb.setFileSize(2048L);
            fb.setFileHash("abc123");
            fb.setStorageType("local");

            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(fb));

            FileDetailVO detail = fileOperationService.getFileDetail(1L, 1L);

            assertThat(detail.fileName()).isEqualTo("report.pdf");
            assertThat(detail.fileSize()).isEqualTo(2048L);
            assertThat(detail.fileHash()).isEqualTo("abc123");
            assertThat(detail.storageType()).isEqualTo("local");
        }
    }

    @Nested
    @DisplayName("文件??)")

    class FileTree {

        @Test
        @DisplayName("返回文件夹层级结??)")

        void getFileTree_returnsTreeStructure() {
            UserFile folder1 = createFile(1L, 1L, "docs", "", "/", 2, null);
            folder1.setFilePath("/");
            UserFile folder2 = createFile(2L, 1L, "images", "", "/docs", 2, null);

            when(userFileRepository.findByUserIdAndDeleteStatusAndFileType(1L, 0, 2))
                    .thenReturn(List.of(folder1, folder2));

            List<TreeNodeVO> tree = fileOperationService.getFileTree(1L);

            assertThat(tree).hasSize(1);
            TreeNodeVO docsNode = tree.stream()
                    .filter(n -> n.fileName().equals("docs"))
                    .findFirst().orElseThrow();
            assertThat(docsNode.children()).hasSize(1);
            assertThat(docsNode.children().get(0).fileName()).isEqualTo("images");
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
        uf.setUploadTime(LocalDateTime.now());
        uf.setModifyTime(LocalDateTime.now());
        return uf;
    }
}
