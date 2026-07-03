package com.qiwenshare.auth.repository;

import com.qiwenshare.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 角色权限关联数据访问接口。
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

    /**
     * 根据角色 ID 查询权限绑定。
     *
     * @param roleId 角色 ID
     * @return 角色权限列表
     */
    List<RolePermission> findByRoleId(Integer roleId);

    /**
     * 根据多个角色 ID 批量查询权限绑定。
     *
     * @param roleIds 角色 ID 集合
     * @return 角色权限列表
     */
    List<RolePermission> findByRoleIdIn(Collection<Integer> roleIds);

    /**
     * 根据角色 ID 删除所有权限绑定。
     *
     * @param roleId 角色 ID
     */
    void deleteByRoleId(Integer roleId);
}
