package com.qiwenshare.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文档编辑版本历史实体。
 *
 * <p>记录每次编辑保存时的版本信息，指向对应的 {@code FileBean}。</p>
 */
@Entity
@Table(name = "document_version")
@Getter
@Setter
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "user_file_id", nullable = false)
    private Long userFileId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "editor_id", nullable = false)
    private Long editorId;

    /** 操作类型：EDIT（编辑保存）、RESTORE（版本回滚） */
    @Column(name = "type", nullable = false, length = 16)
    private String type = "EDIT";

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
    }
}
