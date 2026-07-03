package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.ShareFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件分享 Repository。
 */
@Repository
public interface ShareFileRepository extends JpaRepository<ShareFile, Long> {

    /**
     * 根据分享码查找分享记录。
     *
     * @param shareCode 分享码
     * @return 分享记录
     */
    Optional<ShareFile> findByShareCode(String shareCode);

    /**
     * 查询用户的所有分享记录。
     *
     * @param userId 用户 ID
     * @return 分享列表
     */
    List<ShareFile> findByUserId(Long userId);

    /**
     * 查询已过期的分享记录。
     *
     * @param expireTimeBefore 过期时间阈值
     * @return 过期分享列表
     */
    List<ShareFile> findByExpireTimeIsNotNullAndExpireTimeBefore(LocalDateTime expireTimeBefore);

    /**
     * 检查指定文件是否存在有效的分享记录（未过期）。
     *
     * @param userFileId 用户文件 ID
     * @param now        当前时间
     * @return 有效分享数量
     */
    long countByUserFileIdAndExpireTimeAfterOrExpireTimeIsNull(Long userFileId, LocalDateTime now);
}
