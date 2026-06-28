package com.qiwenshare.ufop.operation;

import com.qiwenshare.ufop.config.UFOPConfigProperties;
import com.qiwenshare.ufop.exception.UFOPException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
@RequiredArgsConstructor
public class LocalStorageDeleter implements Deleter {

    private final UFOPConfigProperties config;

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(Paths.get(config.getRootPath(), path));
        } catch (IOException e) {
            throw new UFOPException("删除失败: " + e.getMessage(), e);
        }
    }
}
