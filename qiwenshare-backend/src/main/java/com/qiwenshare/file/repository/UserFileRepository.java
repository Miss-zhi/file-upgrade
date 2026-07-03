package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.UserFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户文件关联 Repository。
 */
@Repository
public interface UserFileRepository extends JpaRepository<UserFile, Long> {

    /**
     * 按用户 ID 和路径查询文件列表（排除已删除）。
     *
     * @param userId   用户 ID
     * @param filePath 目录路径
     * @return 文件列表
     */
    List<UserFile> findByUserIdAndFilePathAndDeleteStatus(Long userId, String filePath, Integer deleteStatus);

    /**
     * 分页查询用户指定目录下的文件。
     *
     * @param userId      用户 ID
     * @param filePath    目录路径
     * @param deleteStatus 删除状态
     * @param pageable    分页参数
     * @return 分页结果
     */
    Page<UserFile> findByUserIdAndFilePathAndDeleteStatus(Long userId, String filePath, Integer deleteStatus, Pageable pageable);

    /**
     * 分页查询用户指定目录和类型的文件。
     *
     * @param userId      用户 ID
     * @param filePath    目录路径
     * @param deleteStatus 删除状态
     * @param fileType    文件类型
     * @param pageable    分页参数
     * @return 分页结果
     */
    Page<UserFile> findByUserIdAndFilePathAndDeleteStatusAndFileType(Long userId, String filePath, Integer deleteStatus, Integer fileType, Pageable pageable);

    /**
     * 按文件类型跨目录查询（分类浏览）。
     *
     * @param userId      用户 ID
     * @param deleteStatus 删除状态
     * @param fileType    文件类型
     * @param pageable    分页参数
     * @return 分页结果
     */
    Page<UserFile> findByUserIdAndDeleteStatusAndFileType(Long userId, Integer deleteStatus, Integer fileType, Pageable pageable);

    /**
     * 查询用户回收站文件。
     *
     * @param userId   用户 ID
     * @param pageable 分页参数
     * @return 已删除文件分页列表
     */
    Page<UserFile> findByUserIdAndDeleteStatusOrderByDeleteTimeDesc(Long userId, Integer deleteStatus, Pageable pageable);

    /**
     * 检查同目录下是否存在同名文件。
     *
     * @param userId      用户 ID
     * @param filePath    目录路径
     * @param fileName    文件名
     * @param extendName  扩展名
     * @param deleteStatus 删除状态
     * @param fileType    文件类型
     * @return 存在返回 true
     */
    boolean existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
            Long userId, String filePath, String fileName, String extendName, Integer deleteStatus, Integer fileType);

    /**
     * 查找指定目录下同名文件（用于恢复冲突检测）。
     *
     * @param userId      用户 ID
     * @param filePath    目录路径
     * @param fileName    文件名
     * @param extendName  扩展名
     * @param deleteStatus 删除状态
     * @param fileType    文件类型
     * @return 匹配的文件列表
     */
    List<UserFile> findByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatusAndFileType(
            Long userId, String filePath, String fileName, String extendName, Integer deleteStatus, Integer fileType);

    /**
     * 查询某目录下的所有子文件和子文件夹（用于文件夹删除/移动）。
     *
     * @param userId       用户 ID
     * @param filePathPrefix 路径前缀
     * @param deleteStatus 删除状态
     * @return 子文件列表
     */
    List<UserFile> findByUserIdAndFilePathStartingWithAndDeleteStatus(Long userId, String filePathPrefix, Integer deleteStatus);

    /**
     * 批量更新删除状态（软删除）。
     *
     * @param userFileIds    文件 ID 列表
     * @param deleteStatus   删除状态
     * @param deleteTime     删除时间
     * @param deleteBatchNum 删除批次号
     */
    @Modifying
    @Query("UPDATE UserFile u SET u.deleteStatus = :deleteStatus, u.deleteTime = :deleteTime, u.deleteBatchNum = :deleteBatchNum WHERE u.userFileId IN :userFileIds")
    void batchUpdateDeleteStatus(@Param("userFileIds") List<Long> userFileIds,
                                  @Param("deleteStatus") Integer deleteStatus,
                                  @Param("deleteTime") LocalDateTime deleteTime,
                                  @Param("deleteBatchNum") String deleteBatchNum);

    /**
     * 查询需要自动清理的过期回收站文件。
     *
     * @param deleteTimeBefore 删除时间阈值
     * @return 过期文件列表
     */
    List<UserFile> findByDeleteStatusAndDeleteTimeBefore(Integer deleteStatus, LocalDateTime deleteTimeBefore);

    /**
     * 查询用户所有正常文件（不含已删除）。
     *
     * @param userId 用户 ID
     * @return 正常文件列表
     */
    List<UserFile> findByUserIdAndDeleteStatus(Long userId, Integer deleteStatus);

    /**
     * 检查某个 FileBean 是否还有其他 UserFile 引用。
     *
     * @param fileId       物理文件 ID
     * @param deleteStatus 删除状态
     * @return 引用数量
     */
    long countByFileIdAndDeleteStatus(Long fileId, Integer deleteStatus);

    /**
     * 查询用户指定目录下的文件夹（用于文件树）。
     *
     * @param userId       用户 ID
     * @param deleteStatus 删除状态
     * @param fileType     文件类型（2=文件夹）
     * @return 文件夹列表
     */
    List<UserFile> findByUserIdAndDeleteStatusAndFileType(Long userId, Integer deleteStatus, Integer fileType);

    /**
     * 按文件扩展名集合查询（分类浏览）。
     */
    @Query("SELECT u FROM UserFile u WHERE u.userId = :userId AND u.deleteStatus = :deleteStatus AND u.extendName IN :extensions AND u.fileType = 1")
    Page<UserFile> findByUserIdAndDeleteStatusAndExtendNameIn(
            @Param("userId") Long userId,
            @Param("deleteStatus") Integer deleteStatus,
            @Param("extensions") Collection<String> extensions,
            Pageable pageable);

    /**
     * 分页查询所有指定删除状态的文件（用于全量重建索引）。
     *
     * @param deleteStatus 删除状态
     * @param pageable     分页参数
     * @return 分页结果
     */
    Page<UserFile> findByDeleteStatus(Integer deleteStatus, Pageable pageable);

    /**
     * 检查用户在指定目录下是否存在同名文件。
     *
     * @param userId       用户 ID
     * @param filePath     目录路径
     * @param fileName     文件名
     * @param extendName   扩展名
     * @param deleteStatus 删除标志（0=正常）
     * @return 存在返回 true
     */
    boolean existsByUserIdAndFilePathAndFileNameAndExtendNameAndDeleteStatus(
            Long userId, String filePath, String fileName, String extendName, Integer deleteStatus);
}
