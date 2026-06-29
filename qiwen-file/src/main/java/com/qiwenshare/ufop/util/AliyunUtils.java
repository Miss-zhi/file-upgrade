package com.qiwenshare.ufop.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AliyunUtils {

    public static String getOssUrl(String endpoint, String bucket, String key) {
        return "https://" + bucket + "." + endpoint + "/" + key;
    }

    public static String getDownloadUrl(String endpoint, String bucket, String key) {
        log.info("Aliyun getDownloadUrl: bucket={}, key={}", bucket, key);
        return getOssUrl(endpoint, bucket, key);
    }
}
