package com.qiwenshare.file.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户存储配额实体。
 *
 * <p>记录用户总配额、已用空间和预扣空间。Redis 作为实时缓存，
 * 此表作为持久化备份。</p>
 */
@Entity
@Table(name = "storage_bean")
@Getter
@Setter
public class StorageBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_quota", nullable = false)
    private Long totalQuota;

    @Column(name = "used_size", nullable = false)
    private Long usedSize;

    @Column(name = "pre_used_size", nullable = false)
    private Long preUsedSize;

    @Column(name = "modify_time", nullable = false)
    private LocalDateTime modifyTime;

    @PrePersist
    protected void onCreate() {
        this.modifyTime = LocalDateTime.now();
        if (this.totalQuota == null) {
            this.totalQuota = 10737418240L; // 10GB
        }
        if (this.usedSize == null) {
            this.usedSize = 0L;
        }
        if (this.preUsedSize == null) {
            this.preUsedSize = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifyTime = LocalDateTime.now();
    }
}
