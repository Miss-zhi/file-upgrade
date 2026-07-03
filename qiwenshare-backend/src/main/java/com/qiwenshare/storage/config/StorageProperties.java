package com.qiwenshare.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储后端配置属性。
 *
 * <p>从 {@code application.yml} 的 {@code storage} 前缀读取配置。</p>
 */
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {

    /**
     * 存储后端类型：local / minio / aliyun / qiniu / fastdfs
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * MinIO 配置
     */
    private Minio minio = new Minio();

    /**
     * 阿里云 OSS 配置
     */
    private Aliyun aliyun = new Aliyun();

    /**
     * 七牛云配置
     */
    private Qiniu qiniu = new Qiniu();

    /**
     * FastDFS 配置
     */
    private Fastdfs fastdfs = new Fastdfs();

    @Getter
    @Setter
    public static class Local {
        private String basePath = "/data/qiwenshare/files";
    }

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey;
        private String secretKey;
        private String bucket = "qiwenshare";
        /** 预签名 URL 过期时间（秒），默认 1 小时 */
        private int presignedUrlExpiry = 3600;
    }

    @Getter
    @Setter
    public static class Aliyun {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
        /** 预签名 URL 过期时间（秒），默认 1 小时 */
        private long presignedUrlExpiry = 3600;
    }

    @Getter
    @Setter
    public static class Qiniu {
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String domain;
        /** 上传/下载 token 过期时间（秒），默认 1 小时 */
        private long expireSeconds = 3600;
    }

    @Getter
    @Setter
    public static class Fastdfs {
        private String trackerServers;
        private String connectTimeout = "5";
        /** FastDFS 组名，默认 group1 */
        private String group = "group1";
    }
}
