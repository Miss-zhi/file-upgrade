package com.qiwenshare.ufop.operation;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.exception.UFOPException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalStorageReader implements Reader {

    private final UFOPConfigProperties config;

    @Override
    public String read(String path) {
        try {
            return Files.readString(Paths.get(config.getRootPath(), path));
        } catch (IOException e) {
            throw new UFOPException("读取失败: " + e.getMessage(), e);
        }
    }
}
