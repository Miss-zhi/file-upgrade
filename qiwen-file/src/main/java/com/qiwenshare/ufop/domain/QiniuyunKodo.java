package com.qiwenshare.ufop.domain;

import lombok.Data;

@Data
public class QiniuyunKodo {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String domainOfBucket;
}
