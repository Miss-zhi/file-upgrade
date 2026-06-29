package com.qiwenshare.ufop.operation.download.product;

import com.qiwenshare.ufop.operation.Downloader;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Slf4j
@Component
public class FastDFSDownloader implements Downloader {

    @Override
    public InputStream download(String path) {
        log.info("{} download: {}", getStorageType(), path);
        return null;
    }

    public StorageType getStorageType() {
        return StorageType.FAST_DFS;
    }
}
