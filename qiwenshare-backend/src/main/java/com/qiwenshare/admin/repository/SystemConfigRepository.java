package com.qiwenshare.admin.repository;

import com.qiwenshare.admin.entity.SystemConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统参数数据访问接口。
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据参数键名查询。
     *
     * @param configKey 参数键名
     * @return 系统参数实体
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 检查参数键名是否已存在。
     *
     * @param configKey 参数键名
     * @return 是否存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 按关键字模糊搜索（匹配 key 或 description）。
     *
     * @param keyword  搜索关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT c FROM SystemConfig c WHERE " +
           "LOWER(c.configKey) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SystemConfig> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
