package com.qiwenshare.ufop.operation.preview.product;

import com.qiwenshare.ufop.operation.preview.PreviewFile;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.constant.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class QiniuyunKodoPreviewer implements Previewer {

    @Override
    public InputStream preview(PreviewFile previewFile) {
        log.info("{} preview: {}", getStorageType(), previewFile.getFileUrl());
        return InputStream.nullInputStream();
    }

    public StorageType getStorageType() {
        return StorageType.QINIU;
    }
}
