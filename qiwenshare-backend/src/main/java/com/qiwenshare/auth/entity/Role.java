package com.qiwenshare.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色表实体。
 *
 * <p>{@code roleName} 存储不含 {@code ROLE_} 前缀的角色标识（如 ADMIN、USER）。
 * {@code available} 字段在认证时检查，0 表示禁用，不参与权限计算。</p>
 */
@Entity
@Table(name = "role")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false, unique = true, length = 30)
    private String roleName;

    @Column(name = "role_desc", length = 100)
    private String roleDesc;

    @Column(nullable = false)
    private Integer available;
}
