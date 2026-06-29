package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFileVersionService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.file.FileVersion;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.file.FileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileVersionController {

    private final IFileVersionService versionService;

    @GetMapping("/{fileId}/versions")
    public RestResult<List<FileVersion>> listVersions(@PathVariable String fileId) {
        return RestResult.success(versionService.listVersions(fileId));
    }

    @PostMapping("/{fileId}/restore/{versionId}")
    public RestResult<FileVO> restoreVersion(@PathVariable String fileId,
                                              @PathVariable String versionId) {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        FileBean restored = versionService.restoreVersion(fileId, versionId, userId);
        return RestResult.success(FileVO.fromEntity(restored));
    }
}
