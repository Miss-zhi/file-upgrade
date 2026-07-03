package com.qiwenshare.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 用户-角色关联实体（联合主键）。
 */
@Entity
@Table(name = "user_role")
@IdClass(UserRole.UserRoleId.class)
@Getter
@Setter
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "role_id")
    private Integer roleId;

    /**
     * 联合主键类。
     */
    public static class UserRoleId implements Serializable {

        private Long userId;
        private Integer roleId;

        public UserRoleId() {}

        public UserRoleId(Long userId, Integer roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserRoleId that = (UserRoleId) o;
            return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}
