package com.qiwenshare.ufop.operation.delete.product;

import com.qiwenshare.ufop.operation.Deleter;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliyunOSSDeleter implements Deleter {

    @Override
    public void delete(String path) {
        log.info("{} delete: {}", getStorageType(), path);
    }

    public StorageType getStorageType() {
        return StorageType.ALIYUN_OSS;
    }
}
