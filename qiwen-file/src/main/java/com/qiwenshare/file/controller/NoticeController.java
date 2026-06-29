package com.qiwenshare.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.file.api.INoticeService;
import com.qiwenshare.file.domain.Notice;
import com.qiwenshare.file.util.RestResult;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final INoticeService noticeService;

    @GetMapping("/list")
    public RestResult<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        IPage<Notice> result = noticeService.list(page, size, title, beginTime, endTime);
        return RestResult.success(Map.of("records", result.getRecords(), "total", result.getTotal()));
    }

    @GetMapping("/{id}")
    public RestResult<Notice> getById(@PathVariable Long id) {
        return RestResult.success(noticeService.getById(id));
    }

    @PostMapping
    public RestResult<Notice> create(@RequestBody Notice notice) {
        return RestResult.success(noticeService.save(notice));
    }

    @PutMapping("/{id}")
    public RestResult<Notice> update(@PathVariable Long id, @RequestBody Notice notice) {
        notice.setId(id);
        return RestResult.success(noticeService.update(notice));
    }

    @DeleteMapping("/{id}")
    public RestResult<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return RestResult.success();
    }
}
