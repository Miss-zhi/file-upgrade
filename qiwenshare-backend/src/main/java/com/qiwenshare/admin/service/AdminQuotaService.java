package com.qiwenshare.admin.service;

import com.qiwenshare.admin.common.AdminErrorCode;
import com.qiwenshare.admin.common.AdminModuleException;
import com.qiwenshare.admin.dto.BatchSetQuotaDTO;
import com.qiwenshare.admin.vo.AdminQuotaVO;
import com.qiwenshare.auth.entity.User;
import com.qiwenshare.auth.repository.UserRepository;
import com.qiwenshare.file.service.StorageQuotaService;
import com.qiwenshare.file.vo.QuotaInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员配额管理服务。
 *
 * <p>调用 file-module 的 {@link StorageQuotaService} 实现配额操作。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminQuotaService {

    private final StorageQuotaService storageQuotaService;
    private final UserRepository userRepository;

    /**
     * 查询用户配额信息。
     *
     * @param userId 用户业务 ID
     * @return 配额信息
     */
    public AdminQuotaVO getQuotaInfo(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AdminModuleException(AdminErrorCode.USER_NOT_FOUND));

        QuotaInfoVO quotaInfo = storageQuotaService.getQuotaInfo(user.getId());
        return new AdminQuotaVO(
                userId,
                quotaInfo.totalQuota(),
                quotaInfo.usedSize(),
                quotaInfo.availableQuota()
        );
    }

    /**
     * 设置用户配额。
     *
     * @param userId     用户业务 ID
     * @param totalQuota 新配额值（字节）
     */
    @Transactional(rollbackFor = Exception.class)
    public void setQuota(String userId, long totalQuota) {
        if (totalQuota <= 0) {
            throw new AdminModuleException(AdminErrorCode.INVALID_QUOTA);
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AdminModuleException(AdminErrorCode.USER_NOT_FOUND));

        storageQuotaService.setQuota(user.getId(), totalQuota);
        log.info("管理员设置用户配额: userId={}, totalQuota={}", userId, totalQuota);
    }

    /**
     * 批量设置用户配额。
     *
     * @param dto 批量设置请求
     * @return 跳过的 userId 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> batchSetQuota(BatchSetQuotaDTO dto) {
        List<String> skippedUserIds = new ArrayList<>();

        for (BatchSetQuotaDTO.QuotaItem item : dto.items()) {
            User user = userRepository.findByUserId(item.userId()).orElse(null);
            if (user == null) {
                skippedUserIds.add(item.userId());
                continue;
            }
            storageQuotaService.setQuota(user.getId(), item.totalQuota());
        }

        log.info("管理员批量设置配额: total={}, skipped={}", dto.items().size(), skippedUserIds.size());
        return skippedUserIds;
    }
}
