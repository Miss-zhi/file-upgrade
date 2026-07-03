package com.qiwenshare.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户表实体。
 *
 * <p>自增主键 {@code id} + Snowflake 业务主键 {@code userId}。
 * {@code oldPassword} 和 {@code salt} 用于 MD5→BCrypt 透明迁移，迁移完成后清空。</p>
 */
@Entity
@Table(name = "user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, length = 32)
    private String userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 20)
    private String telephone;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "old_password", length = 64)
    private String oldPassword;

    @Column(length = 64)
    private String salt;

    @Column(length = 255)
    private String avatar;

    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;

    @Column(nullable = false)
    private Integer available;
}
