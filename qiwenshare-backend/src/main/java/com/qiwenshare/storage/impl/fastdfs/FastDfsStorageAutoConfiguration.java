package com.qiwenshare.storage.impl.fastdfs;

import com.github.tobato.fastdfs.FdfsClientConfig;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.qiwenshare.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * FastDFS 存储后端自动配置。
 *
 * <p>当 {@code storage.type=fastdfs} 时激活，引入 tobato {@link FdfsClientConfig}，
 * 注册 {@link FastDfsStorageBackend} Bean。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "storage.type", havingValue = "fastdfs")
@Import(FdfsClientConfig.class)
public class FastDfsStorageAutoConfiguration {

    @Bean
    public FastDfsStorageBackend fastDfsStorageBackend(FastFileStorageClient storageClient,
                                                        StorageProperties properties) {
        return new FastDfsStorageBackend(storageClient, properties.getFastdfs().getGroup());
    }
}
