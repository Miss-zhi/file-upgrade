package com.qiwenshare.file.service;

import com.qiwenshare.file.dto.ChunkUploadDTO;
import com.qiwenshare.file.dto.ChunkUploadInitDTO;
import com.qiwenshare.file.dto.SpeedUploadDTO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.UploadTask;
import com.qiwenshare.file.entity.UploadTaskDetail;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.UploadTaskDetailRepository;
import com.qiwenshare.file.repository.UploadTaskRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.UploadFileVO;
import com.qiwenshare.storage.factory.StorageFactory;
import com.qiwenshare.storage.interfaces.StorageBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FileUploadService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock private FileBeanRepository fileBeanRepository;
    @Mock private UserFileRepository userFileRepository;
    @Mock private UploadTaskRepository uploadTaskRepository;
    @Mock private UploadTaskDetailRepository uploadTaskDetailRepository;
    @Mock private StorageFactory storageFactory;
    @Mock private StorageQuotaService storageQuotaService;
    @Mock private StorageBackend storageBackend;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(
                fileBeanRepository, userFileRepository,
                uploadTaskRepository, uploadTaskDetailRepository,
                storageFactory, storageQuotaService, eventPublisher);
    }

    @Nested
    @DisplayName("秒传")
    class SpeedUpload {

        @Test
        @DisplayName("hash 匹配时复??FileBean 并创??UserFile")
        void speedUpload_hashExists_reusesFileBean() {
            SpeedUploadDTO dto = new SpeedUploadDTO("test.txt", "/test", 1024L, "abc123hash");
            FileBean existingBean = new FileBean();
            existingBean.setFileId(1L);
            existingBean.setFileHash("abc123hash");
            existingBean.setFileSize(1024L);

            when(fileBeanRepository.findByFileHashAndFileSize("abc123hash", 1024L))
                    .thenReturn(Optional.of(existingBean));
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    anyLong(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                    .thenReturn(false);
            when(userFileRepository.save(any(UserFile.class))).thenAnswer(inv -> {
                UserFile uf = inv.getArgument(0);
                uf.setUserFileId(100L);
                return uf;
            });

            UploadFileVO result = fileUploadService.speedUpload(dto, 1L);

            assertThat(result.isSpeed()).isTrue();
            assertThat(result.fileSize()).isEqualTo(1024L);
            verify(userFileRepository).save(any(UserFile.class));
        }

        @Test
        @DisplayName("hash 不匹配时抛出 FILE_NOT_FOUND")
        void speedUpload_hashNotExists_throwsFileNotFound() {
            SpeedUploadDTO dto = new SpeedUploadDTO("missing.txt", "/", 2048L, "unknown");
            when(fileBeanRepository.findByFileHashAndFileSize("unknown", 2048L))
                    .thenReturn(Optional.empty());
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    anyLong(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                    .thenReturn(false);

            assertThatThrownBy(() -> fileUploadService.speedUpload(dto, 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("同名文件已存在时抛出 UPLOAD_DUPLICATE")
        void speedUpload_duplicateName_throwsUploadDuplicate() {
            SpeedUploadDTO dto = new SpeedUploadDTO("dup.txt", "/", 100L, "hash1");
            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    1L, "/", "dup", "txt", 0, 1))
                    .thenReturn(true);

            assertThatThrownBy(() -> fileUploadService.speedUpload(dto, 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.UPLOAD_DUPLICATE));
        }
    }

    @Nested
    @DisplayName("普通上??)")

    class NormalUpload {

        @Test
        @DisplayName("超过 10MB 抛出 UPLOAD_SIZE_EXCEEDED")
        void uploadFile_exceedsSizeLimit_throwsSizeExceeded() {
            MockMultipartFile file = new MockMultipartFile("file", "big.dat",
                    "application/octet-stream", new byte[11 * 1024 * 1024]);

            assertThatThrownBy(() -> fileUploadService.uploadFile(file, "/", 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.UPLOAD_SIZE_EXCEEDED));
        }

        @Test
        @DisplayName("正常上传创建 FileBean ??UserFile")
        void uploadFile_success() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "small.txt",
                    "text/plain", "hello world".getBytes());

            when(userFileRepository.existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
                    anyLong(), anyString(), anyString(), anyString(), anyInt(), anyInt()))
                    .thenReturn(false);
            when(fileBeanRepository.findByFileHashAndFileSize(anyString(), anyLong()))
                    .thenReturn(Optional.empty());
            when(storageFactory.getBackend()).thenReturn(storageBackend);
            when(storageFactory.getActiveType()).thenReturn("local");
            when(storageBackend.upload(any(), anyString(), anyLong())).thenReturn("path");
            when(fileBeanRepository.save(any(FileBean.class))).thenAnswer(inv -> {
                FileBean fb = inv.getArgument(0);
                fb.setFileId(10L);
                return fb;
            });
            when(userFileRepository.save(any(UserFile.class))).thenAnswer(inv -> {
                UserFile uf = inv.getArgument(0);
                uf.setUserFileId(200L);
                return uf;
            });

            UploadFileVO result = fileUploadService.uploadFile(file, "/", 1L);

            assertThat(result).isNotNull();
            assertThat(result.fileSize()).isEqualTo(11L);
            verify(storageQuotaService).checkQuota(1L, 11L);
            verify(storageQuotaService).preDeduct(1L, 11L);
            verify(storageQuotaService).confirmQuota(eq(1L), eq(11L), eq(11L));
        }
    }

    @Nested
    @DisplayName("分片上传")
    class ChunkUpload {

        @Test
        @DisplayName("初始化分片上传创建任务和分片详情")
        void initChunkUpload_createsTaskAndDetails() {
            ChunkUploadInitDTO dto = new ChunkUploadInitDTO("big.zip", "/", 15 * 1024 * 1024L, "hash123", 3);

            String taskId = fileUploadService.initChunkUpload(dto, 1L);

            assertThat(taskId).isNotBlank();
            verify(uploadTaskRepository).save(any(UploadTask.class));
            verify(uploadTaskDetailRepository, times(3)).save(any(UploadTaskDetail.class));
        }

        @Test
        @DisplayName("上传分片更新分片�??)")

        void uploadChunk_updatesDetailStatus() throws Exception {
            UploadTask task = new UploadTask();
            task.setTaskId("task1");
            task.setUserId(1L);
            task.setTotalChunks(3);
            task.setUploadedChunks(0);

            when(uploadTaskRepository.findById("task1")).thenReturn(Optional.of(task));
            when(storageFactory.getBackend()).thenReturn(storageBackend);

            UploadTaskDetail detail = new UploadTaskDetail();
            detail.setTaskId("task1");
            detail.setChunkIndex(0);
            detail.setStatus(0);
            when(uploadTaskDetailRepository.findByTaskIdAndChunkIndex("task1", 0))
                    .thenReturn(Optional.of(detail));

            MockMultipartFile chunkData = new MockMultipartFile("chunkData", "chunk",
                    "application/octet-stream", new byte[1024]);
            ChunkUploadDTO dto = new ChunkUploadDTO("task1", 0);

            fileUploadService.uploadChunk(dto, chunkData);

            assertThat(detail.getStatus()).isEqualTo(1);
            assertThat(task.getUploadedChunks()).isEqualTo(1);
            verify(uploadTaskRepository).save(task);
        }

        @Test
        @DisplayName("合并分片 - 任务不存在时抛出异常")
        void mergeChunks_taskNotFound_throwsException() {
            when(uploadTaskRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileUploadService.mergeChunks("nonexistent", "/", 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.UPLOAD_TASK_NOT_FOUND));
        }
    }
}
