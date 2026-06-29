package com.qiwenshare.ufop.operation.write.product;

import com.qiwenshare.ufop.operation.Writer;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QiniuyunKodoWriter implements Writer {

    @Override
    public void write(String path, String content) {
        log.info("{} write: {}", getStorageType(), path);
    }

    public StorageType getStorageType() {
        return StorageType.QINIU;
    }
}
