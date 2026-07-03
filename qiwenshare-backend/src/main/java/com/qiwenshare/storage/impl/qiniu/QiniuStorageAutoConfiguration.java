package com.qiwenshare.storage.impl.qiniu;

import com.qiniu.util.Auth;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiwenshare.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 七牛云存储后端自动配置。
 *
 * <p>当 {@code storage.type=qiniu} 时激活，注册 Auth/UploadManager/BucketManager 单例 Bean。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "storage.type", havingValue = "qiniu")
public class QiniuStorageAutoConfiguration {

    @Bean
    public Auth qiniuAuth(StorageProperties properties) {
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        return Auth.create(qiniu.getAccessKey(), qiniu.getSecretKey());
    }

    @Bean
    public Configuration qiniuConfiguration() {
        return new Configuration();
    }

    @Bean
    public UploadManager qiniuUploadManager(Configuration configuration) {
        return new UploadManager(configuration);
    }

    @Bean
    public BucketManager qiniuBucketManager(Auth auth, Configuration configuration) {
        return new BucketManager(auth, configuration);
    }

    @Bean
    public QiniuStorageBackend qiniuStorageBackend(Auth auth, UploadManager uploadManager,
                                                    BucketManager bucketManager, StorageProperties properties) {
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        return new QiniuStorageBackend(auth, uploadManager, bucketManager,
                qiniu.getBucket(), qiniu.getDomain(), qiniu.getExpireSeconds());
    }
}
