package com.qiwenshare.storage.impl.local;

import com.qiwenshare.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 本地存储后端自动配置。
 *
 * <p>当 {@code storage.type=local} 时激活，注册 {@link LocalStorageBackend} Bean。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageAutoConfiguration {

    @Bean
    public LocalStorageBackend localStorageBackend(StorageProperties properties) {
        return new LocalStorageBackend(properties.getLocal());
    }
}
