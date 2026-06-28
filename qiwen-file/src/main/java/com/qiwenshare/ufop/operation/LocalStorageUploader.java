package com.qiwenshare.ufop.operation;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.exception.UFOPException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalStorageUploader implements Uploader {

    private final UFOPConfigProperties config;

    @Override
    public void upload(String path, InputStream inputStream) {
        try {
            Path target = Paths.get(config.getRootPath(), path);
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UFOPException("上传失败: " + e.getMessage(), e);
        }
    }
}
