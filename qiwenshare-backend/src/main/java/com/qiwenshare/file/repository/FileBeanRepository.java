package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.FileBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 物理文件元数据 Repository。
 */
@Repository
public interface FileBeanRepository extends JpaRepository<FileBean, Long> {

    /**
     * 根据文件 hash 和大小查找物理文件（用于去重/秒传）。
     *
     * @param fileHash 文件 SHA-256 hash
     * @param fileSize 文件大小（字节）
     * @return 匹配的 FileBean，不存在时返回 empty
     */
    Optional<FileBean> findByFileHashAndFileSize(String fileHash, Long fileSize);

    /**
     * 检查是否存在指定 hash 和大小的物理文件。
     *
     * @param fileHash 文件 hash
     * @param fileSize 文件大小
     * @return 存在返回 true
     */
    boolean existsByFileHashAndFileSize(String fileHash, Long fileSize);
}
