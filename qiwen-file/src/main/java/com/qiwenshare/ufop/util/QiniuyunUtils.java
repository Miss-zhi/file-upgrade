package com.qiwenshare.ufop.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QiniuyunUtils {

    public static String getUploadToken(String accessKey, String secretKey, String bucket) {
        log.info("Qiniuyun getUploadToken: bucket={}", bucket);
        return "";
    }

    public static String getDownloadUrl(String domain, String key) {
        return domain.replaceAll("/$", "") + "/" + key;
    }
}
