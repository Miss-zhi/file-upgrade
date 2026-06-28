package com.qiwenshare.file.service;

import com.qiwenshare.file.config.onlyoffice.OnlyOfficeProperties;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.mapper.FileBeanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnlyOfficeService {

    private final OnlyOfficeProperties props;
    private final FileBeanMapper fileBeanMapper;

    public Map<String, Object> getEditorConfig(String fileId, String userId, String mode) {
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null) return null;

        String key = fileId + "_" + (file.getUpdateTime() != null ? file.getUpdateTime().hashCode() : 0);

        Map<String, Object> config = new HashMap<>();

        // document
        Map<String, Object> doc = new HashMap<>();
        doc.put("fileType", getFileType(file.getFileName()));
        doc.put("key", key);
        doc.put("title", file.getFileName());
        doc.put("url", "http://localhost:8080/file/download/" + fileId);
        config.put("document", doc);

        // editorConfig
        Map<String, Object> editor = new HashMap<>();
        editor.put("callbackUrl", props.getCallbackUrl());
        editor.put("mode", mode != null ? mode : "edit");
        editor.put("lang", "zh-CN");
        editor.put("user", Map.of("id", userId, "name", userId));
        config.put("editorConfig", editor);

        // token (simplified)
        config.put("token", UUID.randomUUID().toString());

        return config;
    }

    public void handleCallback(int status, String fileId, String downloadUrl) {
        if (status == 2) {
            // Status 2 = document saved, download the updated file
            FileBean file = fileBeanMapper.selectById(fileId);
            if (file != null) {
                file.setUpdateTime(java.time.LocalDateTime.now());
                fileBeanMapper.updateById(file);
            }
        }
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "docx" -> "docx";
            case "xlsx" -> "xlsx";
            case "pptx" -> "pptx";
            default -> "docx";
        };
    }
}
