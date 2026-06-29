package com.qiwenshare.ufop.operation.preview.product;

import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.preview.PreviewFile;
import com.qiwenshare.ufop.operation.preview.Previewer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Slf4j
@Component
public class LocalStoragePreviewer implements Previewer {

    @Override
    public InputStream preview(PreviewFile previewFile) {
        try {
            Path path = Paths.get(previewFile.getFileUrl());
            return new ByteArrayInputStream(Files.readAllBytes(path));
        } catch (IOException e) {
            log.warn("本地预览失败: {}", e.getMessage());
            return InputStream.nullInputStream();
        }
    }

    public StorageType getStorageType() { return StorageType.LOCAL; }
}
