package com.qiwenshare.file.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiwenshare.file.domain.log.OperationLog;
import com.qiwenshare.file.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper mapper;

    public IPage<OperationLog> page(Integer pageNo, Integer pageSize, String operation, String startTime, String endTime) {
        Page<OperationLog> page = new Page<>(pageNo != null ? pageNo : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(operation)) w.eq(OperationLog::getOperation, operation);
        if (StrUtil.isNotBlank(startTime)) w.ge(OperationLog::getCreateTime, LocalDateTime.parse(startTime));
        if (StrUtil.isNotBlank(endTime)) w.le(OperationLog::getCreateTime, LocalDateTime.parse(endTime));
        w.orderByDesc(OperationLog::getCreateTime);
        return mapper.selectPage(page, w);
    }
}
