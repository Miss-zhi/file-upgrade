package com.qiwenshare.ufop.operation.rename.product;

import com.qiwenshare.ufop.operation.Renamer;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FastDFSRenamer implements Renamer {

    @Override
    public void rename(String sourcePath, String destPath) {
        log.info("{} rename: {} -> {}", getStorageType(), sourcePath, destPath);
    }

    public StorageType getStorageType() {
        return StorageType.FAST_DFS;
    }
}
