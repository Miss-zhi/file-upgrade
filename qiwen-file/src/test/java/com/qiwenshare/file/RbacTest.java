package com.qiwenshare.file;

import com.qiwenshare.file.domain.user.*;
import com.qiwenshare.file.service.RoleService;
import com.qiwenshare.file.service.PermissionService;
import com.qiwenshare.file.vo.user.PermissionVO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RbacTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Test
    @DisplayName("默认角色已初始化")
    void testDefaultsExist() {
        List<Role> roles = roleService.listAll();
        assertTrue(roles.size() >= 3);
        assertTrue(roles.stream().anyMatch(r -> "admin".equals(r.getRoleName())));
    }

    @Test
    @DisplayName("创建+更新+分配用户角色")
    void testCreateUpdateAndAssign() {
        // 创建
        Role role = new Role();
        role.setRoleName("tester");
        role.setDescription("测试角色");
        role.setAvailable(1);
        Role created = roleService.create(role);
        assertNotNull(created.getRoleId());

        // 更新
        created.setRoleName("tester-updated");
        Role updated = roleService.update(created);
        assertEquals("tester-updated", updated.getRoleName());

        // 分配用户
        roleService.assignUserRoles("user001", List.of(created.getRoleId()));
        List<Role> userRoles = roleService.getUserRoles("user001");
        assertTrue(userRoles.stream().anyMatch(r -> created.getRoleId().equals(r.getRoleId())));
    }

    @Test
    @DisplayName("分配角色权限成功")
    void testAssignRolePermissions() {
        Role role = new Role();
        role.setRoleName("p-test");
        role.setDescription("权限测试");
        role.setAvailable(1);
        roleService.create(role);

        List<Permission> all = permissionService.listAll();
        assertFalse(all.isEmpty());
        List<Long> ids = all.stream().map(Permission::getPermissionId).limit(3).toList();
        roleService.assignRolePermissions(role.getRoleId(), ids);
        assertEquals(3, roleService.getRolePermissionIds(role.getRoleId()).size());
    }

    @Test
    @DisplayName("权限树不为空")
    void testPermissionTree() {
        List<PermissionVO> tree = permissionService.getTree();
        assertNotNull(tree);
        assertFalse(tree.isEmpty());
        assertTrue(tree.stream().anyMatch(n -> "文件管理".equals(n.getPermissionName())));
        assertTrue(tree.stream().anyMatch(n -> n.getChildren() != null && !n.getChildren().isEmpty()));
    }

    @Test
    @DisplayName("删除角色清理关联")
    void testDeleteRoleCleansUp() {
        Role role = new Role();
        role.setRoleName("tmp");
        role.setDescription("临时");
        role.setAvailable(1);
        roleService.create(role);
        roleService.assignUserRoles("user002", List.of(role.getRoleId()));
        roleService.delete(role.getRoleId());
        assertTrue(roleService.getUserRoles("user002").isEmpty());
    }
}
