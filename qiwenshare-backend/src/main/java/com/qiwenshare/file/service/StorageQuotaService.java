package com.qiwenshare.file.service;

import com.qiwenshare.file.entity.StorageBean;
import com.qiwenshare.file.exception.FileErrorCode;
import com.qiwenshare.file.exception.FileModuleException;
import com.qiwenshare.file.repository.StorageBeanRepository;
import com.qiwenshare.file.vo.QuotaInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 存储配额管理服务。
 *
 * <p>使用 Redis 原子操作管理实时配额，DB 作为持久化备份。
 * Redis key 格式：{@code file:quota:used:{userId}}。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageQuotaService {

    private static final String QUOTA_USED_KEY_PREFIX = "file:quota:used:";

    private final StorageBeanRepository storageBeanRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 校验用户配额是否充足。
     *
     * @param userId   用户 ID
     * @param fileSize 待上传文件大小（字节）
     * @throws FileModuleException 配额不足时抛出 UPLOAD_QUOTA_EXCEEDED
     */
    public void checkQuota(Long userId, long fileSize) {
        long usedSize = getUsedSize(userId);
        long totalQuota = getTotalQuota(userId);
        if (usedSize + fileSize > totalQuota) {
            throw new FileModuleException(FileErrorCode.UPLOAD_QUOTA_EXCEEDED);
        }
    }

    /**
     * 预扣存储空间（上传开始时调用）。
     *
     * @param userId   用户 ID
     * @param fileSize 预扣大小（字节）
     */
    public void preDeduct(Long userId, long fileSize) {
        String key = QUOTA_USED_KEY_PREFIX + userId;
        redisTemplate.opsForValue().increment(key, fileSize);
        log.debug("预扣配额: userId={}, size={}", userId, fileSize);
    }

    /**
     * 确认实际使用空间（上传完成时调用）。
     *
     * @param userId     用户 ID
     * @param preDeducted 预扣大小
     * @param actualSize  实际大小
     */
    public void confirmQuota(Long userId, long preDeducted, long actualSize) {
        long diff = preDeducted - actualSize;
        if (diff != 0) {
            String key = QUOTA_USED_KEY_PREFIX + userId;
            redisTemplate.opsForValue().increment(key, diff);
        }
        // 同步到 DB
        syncToDb(userId);
        log.debug("确认配额: userId={}, preDeducted={}, actual={}", userId, preDeducted, actualSize);
    }

    /**
     * 释放预扣空间（上传失败时调用）。
     *
     * @param userId   用户 ID
     * @param fileSize 释放大小（字节）
     */
    public void releaseQuota(Long userId, long fileSize) {
        String key = QUOTA_USED_KEY_PREFIX + userId;
        redisTemplate.opsForValue().increment(key, -fileSize);
        log.debug("释放配额: userId={}, size={}", userId, fileSize);
    }

    /**
     * 查询用户配额信息。
     *
     * @param userId 用户 ID
     * @return 配额信息 VO
     */
    public QuotaInfoVO getQuotaInfo(Long userId) {
        long totalQuota = getTotalQuota(userId);
        long usedSize = getUsedSize(userId);
        return new QuotaInfoVO(totalQuota, usedSize, totalQuota - usedSize);
    }

    /**
     * 管理员设置用户配额。
     *
     * @param userId     用户 ID
     * @param totalQuota 新配额值（字节）
     */
    @Transactional(rollbackFor = Exception.class)
    public void setQuota(Long userId, long totalQuota) {
        StorageBean bean = storageBeanRepository.findByUserId(userId)
                .orElseGet(() -> {
                    StorageBean newBean = new StorageBean();
                    newBean.setUserId(userId);
                    return newBean;
                });
        bean.setTotalQuota(totalQuota);
        storageBeanRepository.save(bean);
    }

    /**
     * 从 DB 同步配额到 Redis（定时任务调用）。
     *
     * @param userId 用户 ID
     */
    public void syncFromDb(Long userId) {
        storageBeanRepository.findByUserId(userId).ifPresent(bean -> {
            String key = QUOTA_USED_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(key, String.valueOf(bean.getUsedSize()));
        });
    }

    private long getUsedSize(Long userId) {
        String key = QUOTA_USED_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Long.parseLong(value);
        }
        // Redis 无数据，从 DB 加载
        return storageBeanRepository.findByUserId(userId)
                .map(StorageBean::getUsedSize)
                .orElse(0L);
    }

    private long getTotalQuota(Long userId) {
        return storageBeanRepository.findByUserId(userId)
                .map(StorageBean::getTotalQuota)
                .orElse(10737418240L); // 默认 10GB
    }

    private void syncToDb(Long userId) {
        String key = QUOTA_USED_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            storageBeanRepository.findByUserId(userId).ifPresent(bean -> {
                bean.setUsedSize(Long.parseLong(value));
                storageBeanRepository.save(bean);
            });
        }
    }
}
