package com.qiwenshare.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户文件关联实体。
 *
 * <p>记录用户维度的文件信息（文件名、目录、软删除标记）。
 * 文件夹记录的 {@code fileId} 为 null，{@code fileType} 为 2。</p>
 */
@Entity
@Table(name = "user_file")
@Getter
@Setter
public class UserFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_file_id")
    private Long userFileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "extend_name", length = 20)
    private String extendName;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_type", nullable = false)
    private Integer fileType;

    @Column(name = "delete_status", nullable = false)
    private Integer deleteStatus;

    @Column(name = "delete_time")
    private LocalDateTime deleteTime;

    @Column(name = "delete_batch_num", length = 32)
    private String deleteBatchNum;

    @Column(name = "upload_time", nullable = false, updatable = false)
    private LocalDateTime uploadTime;

    @Column(name = "modify_time", nullable = false)
    private LocalDateTime modifyTime;

    @PrePersist
    protected void onCreate() {
        this.uploadTime = LocalDateTime.now();
        this.modifyTime = LocalDateTime.now();
        if (this.deleteStatus == null) {
            this.deleteStatus = 0;
        }
        if (this.fileType == null) {
            this.fileType = 1;
        }
        if (this.filePath == null) {
            this.filePath = "/";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifyTime = LocalDateTime.now();
    }
}
