package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.StorageBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户存储配额 Repository。
 */
@Repository
public interface StorageBeanRepository extends JpaRepository<StorageBean, Long> {

    /**
     * 根据用户 ID 查询配额信息。
     *
     * @param userId 用户 ID
     * @return 配额记录
     */
    Optional<StorageBean> findByUserId(Long userId);
}
