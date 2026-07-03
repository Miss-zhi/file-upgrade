package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.StorageBean;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.StorageBeanRepository;
import com.qiwenshare.file.vo.QuotaInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * StorageQuotaService 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {

    @Mock private StorageBeanRepository storageBeanRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private StorageQuotaService storageQuotaService;

    @BeforeEach
    void setUp() {
        storageQuotaService = new StorageQuotaService(storageBeanRepository, redisTemplate);
    }

    @Nested
    @DisplayName("配额校验")
    class CheckQuota {

        @Test
        @DisplayName("配额充足时不抛出异常")
        void checkQuota_sufficientQuota_noException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("file:quota:used:1")).thenReturn("1000");
            StorageBean bean = new StorageBean();
            bean.setUserId(1L);
            bean.setTotalQuota(10737418240L);
            when(storageBeanRepository.findByUserId(1L)).thenReturn(Optional.of(bean));

            storageQuotaService.checkQuota(1L, 5000);
            // 不抛出异常即通过
        }

        @Test
        @DisplayName("配额不足时抛??UPLOAD_QUOTA_EXCEEDED")
        void checkQuota_insufficientQuota_throwsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("file:quota:used:1")).thenReturn("10737418230");
            StorageBean bean = new StorageBean();
            bean.setUserId(1L);
            bean.setTotalQuota(10737418240L);
            when(storageBeanRepository.findByUserId(1L)).thenReturn(Optional.of(bean));

            assertThatThrownBy(() -> storageQuotaService.checkQuota(1L, 100))
                    .isInstanceOf(FileModuleException.class)
                    .satisfies(e -> assertThat(((FileModuleException) e).getErrorCode())
                            .isEqualTo(FileErrorCode.UPLOAD_QUOTA_EXCEEDED));
        }
    }

    @Nested
    @DisplayName("配额操作")
    class QuotaOperations {

        @Test
        @DisplayName("preDeduct 增加 Redis 中的已用空间")
        void preDeduct_incrementsRedisValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            storageQuotaService.preDeduct(1L, 5000);

            verify(valueOperations).increment("file:quota:used:1", 5000);
        }

        @Test
        @DisplayName("releaseQuota 减少 Redis 中的已用空间")
        void releaseQuota_decrementsRedisValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            storageQuotaService.releaseQuota(1L, 3000);

            verify(valueOperations).increment("file:quota:used:1", -3000);
        }

        @Test
        @DisplayName("getQuotaInfo 返回正确的配额信??)")

        void getQuotaInfo_returnsCorrectInfo() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("file:quota:used:1")).thenReturn("5000");
            StorageBean bean = new StorageBean();
            bean.setUserId(1L);
            bean.setTotalQuota(10000L);
            when(storageBeanRepository.findByUserId(1L)).thenReturn(Optional.of(bean));

            QuotaInfoVO info = storageQuotaService.getQuotaInfo(1L);

            assertThat(info.totalQuota()).isEqualTo(10000L);
            assertThat(info.usedSize()).isEqualTo(5000L);
            assertThat(info.availableQuota()).isEqualTo(5000L);
        }
    }

    @Nested
    @DisplayName("管理员设置配??)")

    class SetQuota {

        @Test
        @DisplayName("已有配额记录时更??)")

        void setQuota_existingRecord_updates() {
            StorageBean bean = new StorageBean();
            bean.setUserId(1L);
            bean.setTotalQuota(10000L);
            when(storageBeanRepository.findByUserId(1L)).thenReturn(Optional.of(bean));

            storageQuotaService.setQuota(1L, 20000L);

            assertThat(bean.getTotalQuota()).isEqualTo(20000L);
            verify(storageBeanRepository).save(bean);
        }

        @Test
        @DisplayName("无配额记录时创建新的")
        void setQuota_noRecord_createsNew() {
            when(storageBeanRepository.findByUserId(2L)).thenReturn(Optional.empty());

            storageQuotaService.setQuota(2L, 50000L);

            verify(storageBeanRepository).save(argThat(bean ->
                    bean.getUserId().equals(2L) && bean.getTotalQuota().equals(50000L)));
        }
    }
}
