package com.qiwenshare.ufop.operation.copy.product;

import com.qiwenshare.ufop.operation.Copier;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliyunOSSCopier implements Copier {

    @Override
    public void copy(String sourcePath, String destPath) {
        log.info("{} copy: {} -> {}", getStorageType(), sourcePath, destPath);
    }

    public StorageType getStorageType() {
        return StorageType.ALIYUN_OSS;
    }
}
