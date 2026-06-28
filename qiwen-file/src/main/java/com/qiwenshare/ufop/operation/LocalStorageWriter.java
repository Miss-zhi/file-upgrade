package com.qiwenshare.ufop.operation;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.exception.UFOPException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalStorageWriter implements Writer {

    private final UFOPConfigProperties config;

    @Override
    public void write(String path, String content) {
        try {
            Path target = Paths.get(config.getRootPath(), path);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new UFOPException("写入失败: " + e.getMessage(), e);
        }
    }
}
