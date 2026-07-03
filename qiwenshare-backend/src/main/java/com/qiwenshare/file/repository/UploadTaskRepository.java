package com.qiwenshare.file.repository;

import com.qiwenshare.file.entity.UploadTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分片上传任务 Repository。
 */
@Repository
public interface UploadTaskRepository extends JpaRepository<UploadTask, String> {

    /**
     * 查询超时的进行中任务。
     *
     * @param status           任务状态
     * @param createTimeBefore 创建时间阈值
     * @return 超时任务列表
     */
    List<UploadTask> findByStatusAndCreateTimeBefore(Integer status, LocalDateTime createTimeBefore);
}
