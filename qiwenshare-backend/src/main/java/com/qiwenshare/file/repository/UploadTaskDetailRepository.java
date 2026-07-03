package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.UploadTaskDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分片上传详情 Repository。
 */
@Repository
public interface UploadTaskDetailRepository extends JpaRepository<UploadTaskDetail, Long> {

    /**
     * 根据任务 ID 和分片序号查找。
     *
     * @param taskId     任务 ID
     * @param chunkIndex 分片序号
     * @return 分片详情
     */
    Optional<UploadTaskDetail> findByTaskIdAndChunkIndex(String taskId, Integer chunkIndex);

    /**
     * 根据任务 ID 查找所有分片。
     *
     * @param taskId 任务 ID
     * @return 分片列表
     */
    List<UploadTaskDetail> findByTaskId(String taskId);

    /**
     * 根据任务 ID 删除所有分片记录。
     *
     * @param taskId 任务 ID
     */
    void deleteByTaskId(String taskId);
}
