package com.qiwenshare.ufop.domain;

import lombok.Data;

/**
 * 七牛云 Kodo 配置属性
 */
@Data
public class QiniuyunKodo {

    private String domain;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
