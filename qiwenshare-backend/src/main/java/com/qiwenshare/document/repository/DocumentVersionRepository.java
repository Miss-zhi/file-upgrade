package com.qiwenshare.document.repository;

import com.qiwenshare.document.entity.DocumentVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档版本历史 Repository。
 */
@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    /**
     * 按 userFileId 查询版本列表，按版本号降序。
     *
     * @param userFileId 用户文件 ID
     * @return 版本列表
     */
    List<DocumentVersion> findByUserFileIdOrderByVersionNumberDesc(Long userFileId);

    /**
     * 查询指定版本。
     *
     * @param userFileId    用户文件 ID
     * @param versionNumber 版本号
     * @return 版本记录
     */
    Optional<DocumentVersion> findByUserFileIdAndVersionNumber(Long userFileId, int versionNumber);

    /**
     * 统计文件的版本数量。
     *
     * @param userFileId 用户文件 ID
     * @return 版本数量
     */
    long countByUserFileId(Long userFileId);

    /**
     * 查询最大版本号。
     *
     * @param userFileId 用户文件 ID
     * @return 最大版本号，无版本时返回 null
     */
    @Query("SELECT MAX(v.versionNumber) FROM DocumentVersion v WHERE v.userFileId = :userFileId")
    Integer findMaxVersionNumber(@Param("userFileId") Long userFileId);

    /**
     * 查询最旧的版本（用于超出最大保留数时删除）。
     *
     * <p>使用 Spring Data 派生查询 + {@code findFirst} 替代非标准 JPQL LIMIT 语法。</p>
     *
     * @param userFileId 用户文件 ID
     * @return 最旧版本
     */
    Optional<DocumentVersion> findFirstByUserFileIdOrderByVersionNumberAsc(Long userFileId);
}
