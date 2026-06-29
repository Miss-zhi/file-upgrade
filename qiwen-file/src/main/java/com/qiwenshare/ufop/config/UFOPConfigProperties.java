package com.qiwenshare.ufop.config;

import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.preview.ThumbImage;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * UFOP 总配置属性 — 对应 application.yml 中 ufop.* 前缀
 * <p>
 * Bean 由 {@link com.qiwenshare.ufop.autoconfiguration.UFOPAutoConfiguration}
 * 的 {@code @EnableConfigurationProperties} 注册。
 */
@Data
@ConfigurationProperties(prefix = "ufop")
public class UFOPConfigProperties {

    /** 存储类型，默认本地存储 */
    private StorageType storageType = StorageType.LOCAL;

    /** 桶名称 / 根路径名称 */
    private String bucketName = "upload";

    /** 本地存储路径 */
    private String localStoragePath = "./uploads";

    /** 阿里云 OSS 配置 */
    private AliyunConfig aliyun = new AliyunConfig();

    /** MinIO 配置 */
    private MinioConfig minio = new MinioConfig();

    /** 七牛云配置 */
    private QiniuyunConfig qiniuyun = new QiniuyunConfig();

    /** 缩略图配置 */
    private ThumbImage thumbImage = new ThumbImage();

    /**
     * @deprecated 请使用 {@link #getLocalStoragePath()}，保留此方法以兼容旧代码
     */
    public String getRootPath() {
        return localStoragePath;
    }
}
