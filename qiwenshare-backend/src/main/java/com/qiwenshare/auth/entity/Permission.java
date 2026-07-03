package com.qiwenshare.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 权限表实体。
 *
 * <p>{@code permKey} 存储 {@code resource:action} 格式的权限编码。
 * {@code parentId} 支持层级继承，0 表示顶级权限。</p>
 */
@Entity
@Table(name = "permission")
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    @Column(name = "perm_key", nullable = false, unique = true, length = 50)
    private String permKey;

    @Column(name = "perm_name", nullable = false, length = 50)
    private String permName;

    @Column(name = "parent_id", nullable = false)
    private Integer parentId;

    @Column(name = "perm_type", nullable = false)
    private Integer permType;
}
