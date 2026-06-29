package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.file.domain.file.*;
import com.qiwenshare.file.service.FileTypeService;
import com.qiwenshare.file.util.RestResult;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/filetypes")
@RequiredArgsConstructor
public class FileTypeController {

    private final FileTypeService typeService;

    @Operation(summary = "所有分类")
    @GetMapping
    public RestResult<List<FileType>> listTypes() {
        return RestResult.success(typeService.listTypes());
    }

    @Operation(summary = "按分类查文件")
    @GetMapping("/{id}/files")
    public RestResult<Map<String, Object>> listByType(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<FileBean> result = typeService.listByType(id, page, size);
        return RestResult.success(new HashMap<>(Map.of(
            "records", result.getRecords(),
            "total", result.getTotal()
        )));
    }
}
