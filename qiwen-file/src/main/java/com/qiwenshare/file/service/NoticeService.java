package com.qiwenshare.file.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiwenshare.file.api.INoticeService;
import com.qiwenshare.file.domain.Notice;
import com.qiwenshare.file.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NoticeService implements INoticeService {

    private final NoticeMapper mapper;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public IPage<Notice> list(int page, int size, String title, String beginTime, String endTime) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(title)) w.like(Notice::getTitle, title);
        if (StrUtil.isNotBlank(beginTime)) w.ge(Notice::getCreateTime, beginTime);
        if (StrUtil.isNotBlank(endTime)) w.le(Notice::getCreateTime, endTime);
        w.orderByDesc(Notice::getCreateTime);
        return mapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Notice getById(Long id) { return mapper.selectById(id); }

    @Override
    public Notice save(Notice notice) {
        notice.setCreateTime(LocalDateTime.now().format(DT));
        mapper.insert(notice);
        return notice;
    }

    @Override
    public Notice update(Notice notice) {
        notice.setModifyTime(LocalDateTime.now().format(DT));
        mapper.updateById(notice);
        return notice;
    }

    @Override
    public void delete(Long id) { mapper.deleteById(id); }
}
