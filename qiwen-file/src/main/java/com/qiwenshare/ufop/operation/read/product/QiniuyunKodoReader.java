package com.qiwenshare.ufop.operation.read.product;

import com.qiwenshare.ufop.operation.Reader;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QiniuyunKodoReader implements Reader {

    @Override
    public String read(String path) {
        log.info("{} read: {}", getStorageType(), path);
        return null;
    }

    public StorageType getStorageType() {
        return StorageType.QINIU;
    }
}
