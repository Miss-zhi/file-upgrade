package com.qiwenshare.file.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.file.domain.Notice;

public interface INoticeService {
    IPage<Notice> list(int page, int size, String title, String beginTime, String endTime);
    Notice getById(Long id);
    Notice save(Notice notice);
    Notice update(Notice notice);
    void delete(Long id);
}
