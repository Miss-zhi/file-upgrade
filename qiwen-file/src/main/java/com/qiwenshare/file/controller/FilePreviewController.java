package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.ufop.UFOPFactory;
import com.qiwenshare.file.util.RestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FilePreviewController {

    private final IFileService fileService;
    private final UFOPFactory ufopFactory;

    @GetMapping("/preview/{id}")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> preview(@PathVariable String id) {
        FileBean file = fileService.getById(id);
        if (file == null) return ResponseEntity.notFound().build();
        InputStream is = ufopFactory.getDownloader().download(file.getFilePath());
        String contentType = file.getFileType() != null ? file.getFileType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new org.springframework.core.io.InputStreamResource(is));
    }

    @GetMapping("/preview/text/{id}")
    public RestResult<String> previewText(@PathVariable String id) {
        FileBean file = fileService.getById(id);
        if (file == null) return RestResult.fail("文件不存在");
        String content = ufopFactory.getReader().read(file.getFilePath());
        return RestResult.success(content);
    }
}
