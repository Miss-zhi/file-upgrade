package com.qiwenshare.ufop.operation.upload.product;

import com.qiwenshare.ufop.operation.Uploader;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Slf4j
@Component
public class AliyunOSSUploader implements Uploader {

    @Override
    public void upload(String path, InputStream inputStream) {
        log.info("{} upload: {}", getStorageType(), path);
    }

    public StorageType getStorageType() {
        return StorageType.ALIYUN_OSS;
    }
}
