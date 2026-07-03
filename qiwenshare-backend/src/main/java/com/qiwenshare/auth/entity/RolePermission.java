package com.qiwenshare.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 角色-权限关联实体（联合主键）。
 */
@Entity
@Table(name = "role_permission")
@IdClass(RolePermission.RolePermissionId.class)
@Getter
@Setter
public class RolePermission {

    @Id
    @Column(name = "role_id")
    private Integer roleId;

    @Id
    @Column(name = "permission_id")
    private Integer permissionId;

    /**
     * 联合主键类。
     */
    public static class RolePermissionId implements Serializable {

        private Integer roleId;
        private Integer permissionId;

        public RolePermissionId() {}

        public RolePermissionId(Integer roleId, Integer permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RolePermissionId that = (RolePermissionId) o;
            return Objects.equals(roleId, that.roleId) && Objects.equals(permissionId, that.permissionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permissionId);
        }
    }
}
