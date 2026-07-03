package com.qiwenshare.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 分片上传任务实体。
 *
 * <p>记录分片上传任务元数据，包括文件 hash、总分片数、上传状态。</p>
 */
@Entity
@Table(name = "upload_task")
@Getter
@Setter
public class UploadTask {

    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "uploaded_chunks", nullable = false)
    private Integer uploadedChunks;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "modify_time", nullable = false)
    private LocalDateTime modifyTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.modifyTime = LocalDateTime.now();
        if (this.uploadedChunks == null) {
            this.uploadedChunks = 0;
        }
        if (this.status == null) {
            this.status = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifyTime = LocalDateTime.now();
    }
}
