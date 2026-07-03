package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.UserFile;
import com.qiwenshare.file.repository.ShareFileRepository;
import com.qiwenshare.file.repository.UserFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 文件权限检查服务实现。
 *
 * <p>权限规则：
 * <ul>
 *   <li>文件所有者：直接通过</li>
 *   <li>非所有者：检查是否存在有效的分享记录（查看权限）</li>
 *   <li>编辑权限：目前仅文件所有者可编辑（分享模型暂无编辑权限字段）</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilePermissionServiceImpl implements FilePermissionService {

    private final UserFileRepository userFileRepository;
    private final ShareFileRepository shareFileRepository;

    @Override
    public boolean canView(Long userId, Long userFileId) {
        // 文件所有者直接通过
        var userFile = userFileRepository.findById(userFileId);
        if (userFile.isEmpty()) {
            return false;
        }
        if (userId.equals(userFile.get().getUserId())) {
            return true;
        }
        // 检查是否有有效分享
        return hasValidShare(userFileId);
    }

    @Override
    public boolean canEdit(Long userId, Long userFileId) {
        // 目前仅文件所有者可编辑
        var userFile = userFileRepository.findById(userFileId);
        if (userFile.isEmpty()) {
            return false;
        }
        return userId.equals(userFile.get().getUserId());
    }

    /**
     * 检查文件是否有有效的分享记录（未过期）。
     * 使用 Repository 查询方法，避免加载整张 share_file 表（红线 #14）。
     */
    private boolean hasValidShare(Long userFileId) {
        return shareFileRepository.countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull(
                userFileId, LocalDateTime.now()) > 0;
    }
}
