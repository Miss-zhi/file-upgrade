package com.qiwenshare.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiwenshare.file.domain.user.*;
import com.qiwenshare.file.mapper.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<Role> listAll() {
        return roleMapper.selectList(null);
    }

    public Role create(Role role) {
        roleMapper.insert(role);
        return role;
    }

    public Role update(Role role) {
        roleMapper.updateById(role);
        return role;
    }

    @Transactional
    public void delete(Long roleId) {
        roleMapper.deleteById(roleId);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
    }

    public List<Role> getUserRoles(String userId) {
        List<UserRole> urs = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (urs.isEmpty()) return List.of();
        List<Long> roleIds = urs.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds);
    }

    @Transactional
    public void assignUserRoles(String userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long rid : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            userRoleMapper.insert(ur);
        }
    }

    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId))
            .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
    }

    @Transactional
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        for (Long pid : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
    }

    @PostConstruct
    @Transactional
    public void initDefaults() {
        if (roleMapper.selectCount(null) > 0) return;

        // 创建角色
        String[][] roleData = {{"admin", "管理员", "1"}, {"editor", "编辑者", "1"}, {"viewer", "只读者", "1"}};
        for (String[] r : roleData) {
            Role role = new Role();
            role.setRoleName(r[0]);
            role.setDescription(r[1]);
            role.setAvailable(Integer.parseInt(r[2]));
            roleMapper.insert(role);
        }
    }
}
