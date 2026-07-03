package com.qiwenshare.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 物理文件元数据实体。
 *
 * <p>对应存储后端的物理文件，通过 {@code fileHash + fileSize} 去重。
 * 多个 {@link UserFile} 可引用同一个 FileBean（文件去重/秒传）。</p>
 */
@Entity
@Table(name = "file_bean")
@Getter
@Setter
public class FileBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "storage_type", nullable = false, length = 20)
    private String storageType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_status", nullable = false)
    private Integer fileStatus;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "modify_time", nullable = false)
    private LocalDateTime modifyTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.modifyTime = LocalDateTime.now();
        if (this.fileStatus == null) {
            this.fileStatus = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifyTime = LocalDateTime.now();
    }
}
