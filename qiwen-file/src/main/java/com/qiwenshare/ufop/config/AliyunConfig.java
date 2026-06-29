package com.qiwenshare.ufop.config;

import lombok.Data;

@Data
public class AliyunConfig {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
