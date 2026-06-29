package com.qiwenshare.ufop.config;

import lombok.Data;

/**
 * MinIO 配置
 */
@Data
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
