package com.qiwenshare.admin.service;

import com.qiwenshare.admin.common.AdminErrorCode;
import com.qiwenshare.admin.common.AdminModuleException;
import com.qiwenshare.admin.dto.BatchSetQuotaDTO;
import com.qiwenshare.admin.vo.AdminQuotaVO;
import com.qiwenshare.auth.entity.User;
import com.qiwenshare.auth.repository.UserRepository;
import com.qiwenshare.file.service.StorageQuotaService;
import com.qiwenshare.file.vo.QuotaInfoVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminQuotaService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminQuotaServiceTest {

    @InjectMocks
    private AdminQuotaService adminQuotaService;

    @Mock
    private StorageQuotaService storageQuotaService;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("getQuotaInfo")
    class GetQuotaInfo {

        @Test
        @DisplayName("returns quota info for existing user")
        void returnsQuotaInfo() {
            User user = new User();
            user.setId(1L);
            user.setUserId("U001");
            when(userRepository.findByUserId("U001")).thenReturn(Optional.of(user));
            when(storageQuotaService.getQuotaInfo(1L))
                    .thenReturn(new QuotaInfoVO(10_000_000L, 3_000_000L, 7_000_000L));

            AdminQuotaVO result = adminQuotaService.getQuotaInfo("U001");

            assertThat(result.userId()).isEqualTo("U001");
            assertThat(result.totalQuota()).isEqualTo(10_000_000L);
            assertThat(result.usedQuota()).isEqualTo(3_000_000L);
            assertThat(result.availableQuota()).isEqualTo(7_000_000L);
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND for unknown userId")
        void throwsForUnknownUser() {
            when(userRepository.findByUserId("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminQuotaService.getQuotaInfo("UNKNOWN"))
                    .isInstanceOf(AdminModuleException.class)
                    .satisfies(ex -> assertThat(((AdminModuleException) ex).getErrorCode())
                            .isEqualTo(AdminErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("setQuota")
    class SetQuota {

        @Test
        @DisplayName("sets quota for existing user")
        void setsQuota() {
            User user = new User();
            user.setId(2L);
            user.setUserId("U002");
            when(userRepository.findByUserId("U002")).thenReturn(Optional.of(user));

            adminQuotaService.setQuota("U002", 5_000_000L);

            verify(storageQuotaService).setQuota(2L, 5_000_000L);
        }

        @Test
        @DisplayName("throws INVALID_QUOTA for non-positive value")
        void throwsForNonPositive() {
            assertThatThrownBy(() -> adminQuotaService.setQuota("U002", 0))
                    .isInstanceOf(AdminModuleException.class)
                    .satisfies(ex -> assertThat(((AdminModuleException) ex).getErrorCode())
                            .isEqualTo(AdminErrorCode.INVALID_QUOTA));

            assertThatThrownBy(() -> adminQuotaService.setQuota("U002", -100))
                    .isInstanceOf(AdminModuleException.class);
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND for unknown userId")
        void throwsForUnknownUser() {
            when(userRepository.findByUserId("NOBODY")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminQuotaService.setQuota("NOBODY", 1000))
                    .isInstanceOf(AdminModuleException.class)
                    .satisfies(ex -> assertThat(((AdminModuleException) ex).getErrorCode())
                            .isEqualTo(AdminErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("batchSetQuota")
    class BatchSetQuota {

        @Test
        @DisplayName("sets quota for all valid users, skips unknown")
        void setsAndSkips() {
            User user1 = new User();
            user1.setId(10L);
            user1.setUserId("U010");
            when(userRepository.findByUserId("U010")).thenReturn(Optional.of(user1));
            when(userRepository.findByUserId("U_MISSING")).thenReturn(Optional.empty());

            BatchSetQuotaDTO dto = new BatchSetQuotaDTO(List.of(
                    new BatchSetQuotaDTO.QuotaItem("U010", 1_000_000L),
                    new BatchSetQuotaDTO.QuotaItem("U_MISSING", 2_000_000L)
            ));

            List<String> skipped = adminQuotaService.batchSetQuota(dto);

            assertThat(skipped).containsExactly("U_MISSING");
            verify(storageQuotaService).setQuota(10L, 1_000_000L);
            verify(storageQuotaService, never()).setQuota(eq(null), anyLong());
        }
    }
}
