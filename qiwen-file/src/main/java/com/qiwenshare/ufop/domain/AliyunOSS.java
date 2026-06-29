package com.qiwenshare.ufop.domain;

import lombok.Data;

/**
 * 阿里云 OSS 配置属性
 */
@Data
public class AliyunOSS {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String objectName;
}
