package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.user.User;
import com.qiwenshare.file.mapper.FileBeanMapper;
import com.qiwenshare.file.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final FileBeanMapper fileBeanMapper;
    private final UserMapper userMapper;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 文件统计
        LambdaQueryWrapper<FileBean> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(FileBean::getIsFolder, false);
        long fileCount = fileBeanMapper.selectCount(fileWrapper);

        Long totalSize = fileBeanMapper.selectList(fileWrapper)
                .stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum();

        // 用户统计
        long userCount = userMapper.selectCount(null);

        stats.put("fileCount", fileCount);
        stats.put("totalSize", totalSize);
        stats.put("userCount", userCount);
        return stats;
    }
}
