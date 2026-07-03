package com.qiwenshare.file.service;

import com.qiwenshare.file.dto.ShareCreateDTO;
import com.qiwenshare.file.entity.FileBean;
import com.qiwenshare.file.entity.ShareFile;
import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.FileBeanRepository;
import com.qiwenshare.file.repository.ShareFileRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import com.qiwenshare.file.vo.ShareInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * FileShareService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FileShareServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ShareFileRepository shareFileRepository;
    @Mock private UserFileRepository userFileRepository;
    @Mock private FileBeanRepository fileBeanRepository;

    private FileShareService fileShareService;

    @BeforeEach
    void setUp() {
        fileShareService = new FileShareService(redisTemplate, shareFileRepository, userFileRepository, fileBeanRepository);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("创建分享")
    class CreateShare {

        @Test
        @DisplayName("创建分享生成 8 位分享码??4 位提取码")
        void createShare_generatesCodes() {
            UserFile uf = new UserFile();
            uf.setUserFileId(1L);
            uf.setUserId(1L);
            uf.setFileName("doc");
            uf.setExtendName("pdf");
            uf.setFileId(10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(createFileBean(10L, 2048L)));
            when(shareFileRepository.save(any(ShareFile.class))).thenAnswer(inv -> {
                ShareFile sf = inv.getArgument(0);
                sf.setShareId(100L);
                return sf;
            });

            ShareInfoVO result = fileShareService.createShare(new ShareCreateDTO(1L, 7, null, null, null), 1L);

            assertThat(result.shareCode()).hasSize(8);
            assertThat(result.extractCode()).hasSize(4);
            assertThat(result.expireTime()).isNotNull();
        }

        @Test
        @DisplayName("永久分享 expireTime ??null")
        void createShare_permanentShare_nullExpireTime() {
            UserFile uf = new UserFile();
            uf.setUserFileId(1L);
            uf.setUserId(1L);
            uf.setFileName("file");
            uf.setExtendName("txt");
            uf.setFileId(10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(createFileBean(10L, 100L)));
            when(shareFileRepository.save(any(ShareFile.class))).thenAnswer(inv -> {
                ShareFile sf = inv.getArgument(0);
                sf.setShareId(101L);
                return sf;
            });

            ShareInfoVO result = fileShareService.createShare(new ShareCreateDTO(1L, 0, null, null, null), 1L);

            assertThat(result.expireTime()).isNull();
        }

        @Test
        @DisplayName("非所有者创建分享抛??FILE_ACCESS_DENIED")
        void createShare_notOwner_throwsAccessDenied() {
            UserFile uf = new UserFile();
            uf.setUserFileId(1L);
            uf.setUserId(2L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));

            assertThatThrownBy(() -> fileShareService.createShare(new ShareCreateDTO(1L, 7, null, null, null), 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_ACCESS_DENIED));
        }
    }

    @Nested
    @DisplayName("验证提取??)")

    class VerifyShare {

        @Test
        @DisplayName("提取码正确返回分享信??)")

        void verifyShare_correctCode_returnsInfo() {
            ShareFile sf = createShareFile(1L, "abcd1234", "1234", null, 1L);
            when(shareFileRepository.findByShareCode("abcd1234")).thenReturn(Optional.of(sf));
            UserFile uf = new UserFile();
            uf.setUserFileId(1L);
            uf.setFileName("doc");
            uf.setExtendName("pdf");
            uf.setFileId(10L);
            when(userFileRepository.findById(1L)).thenReturn(Optional.of(uf));
            when(fileBeanRepository.findById(10L)).thenReturn(Optional.of(createFileBean(10L, 500L)));

            ShareInfoVO result = fileShareService.verifyShare("abcd1234", "1234");

            assertThat(result.shareCode()).isEqualTo("abcd1234");
            assertThat(sf.getViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("提取码错误抛??SHARE_EXTRACT_CODE_WRONG")
        void verifyShare_wrongCode_throwsException() {
            ShareFile sf = createShareFile(1L, "abcd1234", "1234", null, 1L);
            when(shareFileRepository.findByShareCode("abcd1234")).thenReturn(Optional.of(sf));

            assertThatThrownBy(() -> fileShareService.verifyShare("abcd1234", "9999"))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.SHARE_EXTRACT_CODE_WRONG));
        }

        @Test
        @DisplayName("过期分享抛出 SHARE_EXPIRED")
        void verifyShare_expired_throwsException() {
            ShareFile sf = createShareFile(1L, "expired1", "1234",
                    LocalDateTime.now().minusDays(1), 1L);
            when(shareFileRepository.findByShareCode("expired1")).thenReturn(Optional.of(sf));

            assertThatThrownBy(() -> fileShareService.verifyShare("expired1", "1234"))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.SHARE_EXPIRED));
        }

        @Test
        @DisplayName("分享不存在抛??SHARE_NOT_FOUND")
        void verifyShare_notFound_throwsException() {
            when(shareFileRepository.findByShareCode("nonexist")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileShareService.verifyShare("nonexist", "1234"))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.SHARE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("取消分享")
    class CancelShare {

        @Test
        @DisplayName("非所有者取消分享抛??FILE_ACCESS_DENIED")
        void cancelShare_notOwner_throwsException() {
            ShareFile sf = createShareFile(1L, "code1234", "1234", null, 2L);
            when(shareFileRepository.findById(1L)).thenReturn(Optional.of(sf));

            assertThatThrownBy(() -> fileShareService.cancelShare(1L, 1L))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.FILE_ACCESS_DENIED));
        }
    }

    private ShareFile createShareFile(Long shareId, String shareCode, String extractCode,
                                       LocalDateTime expireTime, Long userId) {
        ShareFile sf = new ShareFile();
        sf.setShareId(shareId);
        sf.setShareCode(shareCode);
        sf.setExtractCode(extractCode);
        sf.setExpireTime(expireTime);
        sf.setUserId(userId);
        sf.setUserFileId(1L);
        sf.setViewCount(0);
        sf.setCreateTime(LocalDateTime.now());
        return sf;
    }

    private FileBean createFileBean(Long fileId, Long fileSize) {
        FileBean fb = new FileBean();
        fb.setFileId(fileId);
        fb.setFileSize(fileSize);
        fb.setFileHash("hash");
        fb.setStorageType("local");
        fb.setStoragePath("path");
        return fb;
    }
}
