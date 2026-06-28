package com.qiwenshare.ufop.operation;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.exception.UFOPException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalStorageCopier implements Copier {

    private final UFOPConfigProperties config;

    @Override
    public void copy(String sourcePath, String destPath) {
        try {
            Files.copy(Paths.get(config.getRootPath(), sourcePath),
                       Paths.get(config.getRootPath(), destPath),
                       StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UFOPException("复制失败: " + e.getMessage(), e);
        }
    }
}
