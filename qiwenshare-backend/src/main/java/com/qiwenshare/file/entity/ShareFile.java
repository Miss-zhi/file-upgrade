package com.qiwenshare.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件分享记录实体。
 *
 * <p>记录分享链接信息，包含分享码、提取码、过期时间等。</p>
 */
@Entity
@Table(name = "share_file")
@Getter
@Setter
public class ShareFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "share_id")
    private Long shareId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_file_id", nullable = false)
    private Long userFileId;

    @Column(name = "share_code", nullable = false, length = 8)
    private String shareCode;

    @Column(name = "extract_code", length = 6)
    private String extractCode;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
    }
}
