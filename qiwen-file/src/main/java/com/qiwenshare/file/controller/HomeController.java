package com.qiwenshare.file.controller;

import com.qiwenshare.file.api.IFileService;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.file.domain.file.FileBean;
import com.qiwenshare.file.domain.user.User;
import com.qiwenshare.file.mapper.FileBeanMapper;
import com.qiwenshare.file.mapper.UserMapper;
import com.qiwenshare.file.util.RestResult;
import com.qiwenshare.file.vo.file.FileVO;
import com.qiwenshare.file.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final IFileService fileService;
    private final IUserService userService;
    private final FileBeanMapper fileBeanMapper;
    private final UserMapper userMapper;

    @GetMapping("/stats")
    public RestResult<Map<String, Object>> stats() {
        String userId = getCurrentUserId();
        Map<String, Object> result = new HashMap<>();

        // 用户信息
        User user = userService.getUserById(userId);
        result.put("user", user != null ? UserVO.fromUser(user) : null);

        // 最近文件（按更新时间倒序，前 8 条）
        List<FileBean> recent = fileBeanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileBean>()
                        .eq(FileBean::getUserId, userId)
                        .eq(FileBean::getDeleted, 0)
                        .eq(FileBean::getIsFolder, false)
                        .orderByDesc(FileBean::getUpdateTime)
                        .last("LIMIT 8")
        );
        result.put("recentFiles", recent.stream().map(FileVO::fromEntity).collect(Collectors.toList()));

        // 存储统计
        long fileCount = fileBeanMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileBean>()
                        .eq(FileBean::getUserId, userId)
                        .eq(FileBean::getDeleted, 0)
                        .eq(FileBean::getIsFolder, false)
        );
        Long totalSize = fileBeanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileBean>()
                        .eq(FileBean::getUserId, userId)
                        .eq(FileBean::getDeleted, 0)
                        .eq(FileBean::getIsFolder, false)
        ).stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum();

        Map<String, Object> storage = new HashMap<>();
        storage.put("fileCount", fileCount);
        storage.put("totalSize", totalSize);
        result.put("storage", storage);

        return RestResult.success(result);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (String) auth.getPrincipal();
    }
}
