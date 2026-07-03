package com.qiwenshare.auth.repository;

import com.qiwenshare.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限数据访问接口。
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    /**
     * 根据权限编码列表查询权限。
     *
     * @param permKeys 权限编码列表
     * @return 权限列表
     */
    List<Permission> findByPermKeyIn(List<String> permKeys);
}
