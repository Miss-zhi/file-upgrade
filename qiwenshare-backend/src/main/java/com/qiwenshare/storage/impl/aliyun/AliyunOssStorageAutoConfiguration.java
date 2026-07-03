package com.qiwenshare.storage.impl.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.qiwenshare.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 阿里云 OSS 存储后端自动配置。
 *
 * <p>当 {@code storage.type=aliyun} 时激活，注册 {@link OSS} 客户端和 {@link AliyunOssStorageBackend} Bean。
 * 应用关闭时通过 {@code @Bean(destroyMethod = "shutdown")} 自动释放连接池。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "storage.type", havingValue = "aliyun")
public class AliyunOssStorageAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(StorageProperties properties) {
        StorageProperties.Aliyun aliyun = properties.getAliyun();
        return new OSSClientBuilder().build(
                aliyun.getEndpoint(),
                aliyun.getAccessKeyId(),
                aliyun.getAccessKeySecret());
    }

    @Bean
    public AliyunOssStorageBackend aliyunOssStorageBackend(OSS ossClient, StorageProperties properties) {
        return new AliyunOssStorageBackend(ossClient, properties.getAliyun().getBucket(),
                properties.getAliyun().getPresignedUrlExpiry());
    }
}
