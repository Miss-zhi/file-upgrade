package com.qiwenshare.auth.repository;

import com.qiwenshare.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色数据访问接口。
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * 根据可用性查询角色列表。
     *
     * @param available 1-启用 0-禁用
     * @return 角色列表
     */
    List<Role> findByAvailable(Integer available);
}
