package com.qiwenshare.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 分片上传详情实体。
 *
 * <p>记录每个分片的上传状态、大小、hash 和临时存储路径。</p>
 */
@Entity
@Table(name = "upload_task_detail")
@Getter
@Setter
public class UploadTaskDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "chunk_hash", length = 64)
    private String chunkHash;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "storage_path", length = 500)
    private String storagePath;
}
