package com.qiwenshare.admin.service;

import com.qiwenshare.admin.common.AdminErrorCode;
import com.qiwenshare.admin.common.AdminModuleException;
import com.qiwenshare.admin.dto.CreateConfigDTO;
import com.qiwenshare.admin.dto.UpdateConfigDTO;
import com.qiwenshare.admin.entity.SystemConfig;
import com.qiwenshare.admin.repository.SystemConfigRepository;
import com.qiwenshare.admin.vo.ConfigVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SystemConfigService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @InjectMocks
    private SystemConfigService systemConfigService;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SystemConfig createConfig(Long id, String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setId(id);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription("desc-" + key);
        config.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        config.setUpdateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        return config;
    }

    @Nested
    @DisplayName("listConfigs")
    class ListConfigs {

        @Test
        @DisplayName("searches by keyword when keyword is provided")
        void searchByKeyword() {
            SystemConfig config = createConfig(1L, "upload.max-size", "10MB");
            Page<SystemConfig> page = new PageImpl<>(List.of(config));
            PageRequest pageable = PageRequest.of(0, 10);

            when(systemConfigRepository.searchByKeyword("upload", pageable)).thenReturn(page);

            Page<ConfigVO> result = systemConfigService.listConfigs("upload", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).configKey()).isEqualTo("upload.max-size");
            verify(systemConfigRepository).searchByKeyword("upload", pageable);
            verify(systemConfigRepository, never()).findAll(any(PageRequest.class));
        }

        @Test
        @DisplayName("returns all when keyword is null")
        void returnsAllWhenNoKeyword() {
            Page<SystemConfig> page = new PageImpl<>(List.of());
            PageRequest pageable = PageRequest.of(0, 10);

            when(systemConfigRepository.findAll(pageable)).thenReturn(page);

            Page<ConfigVO> result = systemConfigService.listConfigs(null, pageable);

            verify(systemConfigRepository).findAll(pageable);
            verify(systemConfigRepository, never()).searchByKeyword(anyString(), any());
        }

        @Test
        @DisplayName("returns all when keyword is blank")
        void returnsAllWhenBlankKeyword() {
            Page<SystemConfig> page = new PageImpl<>(List.of());
            PageRequest pageable = PageRequest.of(0, 10);

            when(systemConfigRepository.findAll(pageable)).thenReturn(page);

            systemConfigService.listConfigs("  ", pageable);

            verify(systemConfigRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("createConfig")
    class CreateConfig {

        @Test
        @DisplayName("creates config and returns VO")
        void createsConfig() {
            when(systemConfigRepository.existsByConfigKey("new.key")).thenReturn(false);
            when(systemConfigRepository.save(any(SystemConfig.class)))
                    .thenAnswer(inv -> {
                        SystemConfig c = inv.getArgument(0);
                        c.setId(1L);
                        c.setCreateTime(LocalDateTime.now());
                        c.setUpdateTime(LocalDateTime.now());
                        return c;
                    });

            CreateConfigDTO dto = new CreateConfigDTO("new.key", "new-value", "description");
            ConfigVO result = systemConfigService.createConfig(dto);

            assertThat(result.configKey()).isEqualTo("new.key");
            assertThat(result.configValue()).isEqualTo("new-value");
            verify(systemConfigRepository).save(any(SystemConfig.class));
        }

        @Test
        @DisplayName("throws CONFIG_KEY_DUPLICATE for existing key")
        void throwsForDuplicateKey() {
            when(systemConfigRepository.existsByConfigKey("existing.key")).thenReturn(true);

            CreateConfigDTO dto = new CreateConfigDTO("existing.key", "value", "desc");

            assertThatThrownBy(() -> systemConfigService.createConfig(dto))
                    .isInstanceOf(AdminModuleException.class)
                    .satisfies(ex -> assertThat(((AdminModuleException) ex).getErrorCode())
                            .isEqualTo(AdminErrorCode.CONFIG_KEY_DUPLICATE));
        }
    }

    @Nested
    @DisplayName("updateConfig")
    class UpdateConfig {

        @Test
        @DisplayName("updates config value and description")
        void updatesConfig() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                SystemConfig config = createConfig(1L, "my.key", "old-value");
                when(systemConfigRepository.findById(1L)).thenReturn(Optional.of(config));

                UpdateConfigDTO dto = new UpdateConfigDTO("new-value", "new-desc");
                ConfigVO result = systemConfigService.updateConfig(1L, dto);

                assertThat(result.configValue()).isEqualTo("new-value");
                assertThat(result.description()).isEqualTo("new-desc");
                verify(systemConfigRepository).save(config);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("throws CONFIG_NOT_FOUND for unknown id")
        void throwsForUnknownId() {
            when(systemConfigRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> systemConfigService.updateConfig(999L, new UpdateConfigDTO("v", "d")))
                    .isInstanceOf(AdminModuleException.class)
                    .satisfies(ex -> assertThat(((AdminModuleException) ex).getErrorCode())
                            .isEqualTo(AdminErrorCode.CONFIG_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteConfig")
    class DeleteConfig {

        @Test
        @DisplayName("deletes existing config")
        void deletesConfig() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                SystemConfig config = createConfig(1L, "del.key", "value");
                when(systemConfigRepository.findById(1L)).thenReturn(Optional.of(config));

                systemConfigService.deleteConfig(1L);

                verify(systemConfigRepository).delete(config);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("throws CONFIG_NOT_FOUND for unknown id")
        void throwsForUnknownId() {
            when(systemConfigRepository.findById(888L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> systemConfigService.deleteConfig(888L))
                    .isInstanceOf(AdminModuleException.class);
        }
    }

    @Nested
    @DisplayName("getConfigValue")
    class GetConfigValue {

        @Test
        @DisplayName("returns cached value from Redis")
        void returnsCachedValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("sys:config:my.key")).thenReturn("cached-value");

            String result = systemConfigService.getConfigValue("my.key");

            assertThat(result).isEqualTo("cached-value");
            verify(systemConfigRepository, never()).findByConfigKey(anyString());
        }

        @Test
        @DisplayName("falls back to DB on cache miss and backfills cache")
        void fallsBackToDb() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("sys:config:db.key")).thenReturn(null);

            SystemConfig config = createConfig(1L, "db.key", "db-value");
            when(systemConfigRepository.findByConfigKey("db.key")).thenReturn(Optional.of(config));

            String result = systemConfigService.getConfigValue("db.key");

            assertThat(result).isEqualTo("db-value");
            verify(valueOperations).set(eq("sys:config:db.key"), eq("db-value"), any(Duration.class));
        }

        @Test
        @DisplayName("returns null when key not in cache and not in DB")
        void returnsNullWhenNotFound() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("sys:config:missing")).thenReturn(null);
            when(systemConfigRepository.findByConfigKey("missing")).thenReturn(Optional.empty());

            String result = systemConfigService.getConfigValue("missing");

            assertThat(result).isNull();
        }
    }
}
