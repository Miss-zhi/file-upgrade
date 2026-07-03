package com.qiwenshare.auth.repository;

import com.qiwenshare.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 用户角色关联数据访问接口。
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    /**
     * 根据用户 ID（自增主键）查询角色绑定。
     *
     * @param userId 用户自增主键 ID
     * @return 用户角色列表
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 根据多个用户 ID 批量查询角色绑定。
     *
     * @param userIds 用户自增主键 ID 集合
     * @return 用户角色列表
     */
    List<UserRole> findByUserIdIn(Collection<Long> userIds);

    /**
     * 根据用户 ID（自增主键）删除所有角色绑定。
     *
     * @param userId 用户自增主键 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 根据角色 ID 查询角色绑定。
     *
     * @param roleId 角色 ID
     * @return 用户角色列表
     */
    List<UserRole> findByRoleId(Integer roleId);
}
