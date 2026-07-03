package com.qiwenshare.storage.impl.minio;

import com.qiwenshare.storage.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MinIO 存储后端自动配置。
 *
 * <p>当 {@code storage.type=minio} 时激活，注册 {@link MinioClient} 和 {@link MinioStorageBackend} Bean。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
@Slf4j
public class MinioStorageAutoConfiguration {

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        StorageProperties.Minio minio = properties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Bean
    public MinioStorageBackend minioStorageBackend(MinioClient minioClient, StorageProperties properties) {
        StorageProperties.Minio minio = properties.getMinio();
        checkAndCreateBucket(minioClient, minio.getBucket());
        return new MinioStorageBackend(minioClient, minio.getBucket(), minio.getPresignedUrlExpiry());
    }

    /**
     * 启动时检查 bucket 是否存在，不存在则自动创建。
     */
    private void checkAndCreateBucket(MinioClient minioClient, String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket [{}] 不存在，已自动创建", bucket);
            } else {
                log.debug("MinIO bucket [{}] 已存在", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 检查/创建失败: {}", bucket, e);
            throw new RuntimeException("MinIO bucket 检查/创建失败: " + bucket, e);
        }
    }
}
