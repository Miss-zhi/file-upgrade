package com.qiwenshare.ufop.operation;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.exception.UFOPException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalStorageDownloader implements Downloader {

    private final UFOPConfigProperties config;

    @Override
    public InputStream download(String path) {
        try {
            return new FileInputStream(Paths.get(config.getRootPath(), path).toFile());
        } catch (FileNotFoundException e) {
            throw new UFOPException("文件不存在: " + path);
        }
    }
}
