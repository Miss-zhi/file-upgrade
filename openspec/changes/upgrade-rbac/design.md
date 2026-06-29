# Design: upgrade-rbac — 技术方案

## 1. 数据模型（4 实体）

### Role

**文件**：`com.qiwenshare.file.domain.user.RoleEntity`

```java
package com.qiwenshare.file.domain.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "role")
@TableName("role")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long roleId;

    private String roleName;      // 角色名
    private String description;   // 描述
    private Integer available;    // 0=禁用, 1=可用
}
```

### Permission

**文件**：`com.qiwenshare.file.domain.user.PermissionEntity`

```java
@Data
@Entity
@Table(name = "permission")
@TableName("permission")
public class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long permissionId;

    private Long parentId;        // 父权限ID（树形）
    private String permissionName;
    private Integer resourceType; // 0=菜单, 1=按钮
    private String permissionCode;// 权限码（如 file:upload）
    private Integer orderNum;
}
```

### UserRole

**文件**：`com.qiwenshare.file.domain.user.UserRoleEntity`

```java
@Data
@Entity
@Table(name = "user_role")
@TableName("user_role")
public class UserRoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;        // User.id
    private Long roleId;          // Role.roleId
}
```

### RolePermission

**文件**：`com.qiwenshare.file.domain.user.RolePermissionEntity`

```java
@Data
@Entity
@Table(name = "role_permission")
@TableName("role_permission")
public class RolePermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;
    private Long permissionId;
}
```

## 2. Mapper（4 个）

**文件**：`com.qiwenshare.file.mapper.RoleMapper`

```java
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
}
```

**文件**：`com.qiwenshare.file.mapper.PermissionMapper`

```java
@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
}
```

**文件**：`com.qiwenshare.file.mapper.UserRoleMapper`

```java
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
}
```

**文件**：`com.qiwenshare.file.mapper.RolePermissionMapper`

```java
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {
}
```

## 3. Service 层

### RoleService

**文件**：`com.qiwenshare.file.service.RoleService`

```java
@Service @RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /** 列出所有角色 */
    public List<RoleEntity> listAll() { ... }

    /** 创建角色 */
    public RoleEntity create(RoleEntity role) { ... }

    /** 更新角色 */
    public RoleEntity update(RoleEntity role) { ... }

    /** 删除角色（级联清理） */
    public void delete(Long roleId) { ... }

    /** 查询用户的所有角色 */
    public List<RoleEntity> getUserRoles(String userId) { ... }

    /** 分配用户角色 */
    public void assignUserRoles(String userId, List<Long> roleIds) { ... }

    /** 查询角色的权限列表 */
    public List<Long> getRolePermissionIds(Long roleId) { ... }

    /** 分配角色权限 */
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) { ... }

    @PostConstruct
    public void initDefaults() {
        // 创建 admin/editor/viewer 三种默认角色 + 10 个默认权限
    }
}
```

### PermissionService

**文件**：`com.qiwenshare.file.service.PermissionService`

```java
@Service @RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper mapper;

    /** 获取权限树 */
    public List<PermissionVO> getTree() { ... }

    /** 递归构建树 */
    private List<PermissionVO> buildTree(Long parentId, List<PermissionEntity> all) { ... }
}
```

### PermissionVO

**文件**：`com.qiwenshare.file.vo.user.PermissionVO`

```java
@Data
public class PermissionVO {
    private Long permissionId;
    private Long parentId;
    private String permissionName;
    private Integer resourceType;
    private String permissionCode;
    private Integer orderNum;
    private List<PermissionVO> children;
}
```

## 4. Controller

### AdminController 扩展

**文件**：`com.qiwenshare.file.controller.AdminController`（在现有基础上添加）

```java
// ===== 角色管理 =====

@Operation(summary = "角色列表")
@GetMapping("/roles")
public RestResult<List<RoleEntity>> listRoles();

@Operation(summary = "创建角色")
@PostMapping("/roles")
public RestResult<RoleEntity> createRole(@RequestBody RoleEntity role);

@Operation(summary = "更新角色")
@PutMapping("/roles/{id}")
public RestResult<RoleEntity> updateRole(@PathVariable Long id, @RequestBody RoleEntity role);

@Operation(summary = "删除角色")
@DeleteMapping("/roles/{id}")
public RestResult<Void> deleteRole(@PathVariable Long id);

// ===== 权限管理 =====

@Operation(summary = "权限树")
@GetMapping("/permissions/tree")
public RestResult<List<PermissionVO>> getPermissionTree();

// ===== 分配接口 =====

@Operation(summary = "查询用户角色")
@GetMapping("/users/{userId}/roles")
public RestResult<List<RoleEntity>> getUserRoles(@PathVariable String userId);

@Operation(summary = "分配用户角色")
@PutMapping("/users/{userId}/roles")
public RestResult<Void> assignUserRoles(@PathVariable String userId,
                                         @RequestBody Map<String, List<Long>> body);

@Operation(summary = "查询角色的权限")
@GetMapping("/roles/{roleId}/permissions")
public RestResult<List<Long>> getRolePermissions(@PathVariable Long roleId);

@Operation(summary = "分配角色权限")
@PutMapping("/roles/{roleId}/permissions")
public RestResult<Void> assignRolePermissions(@PathVariable Long roleId,
                                               @RequestBody Map<String, List<Long>> body);
```

## 5. SecurityConfig 动态权限

**文件**：`com.qiwenshare.file.config.security.SecurityConfig`（修改）

```java
// 注入 PermissionService，在 configure 时动态构建 URL 拦截规则
// 查找 permissionCode 与实际 URL 的映射关系
// 例如 permissionCode="admin:user" → 拦截 /admin/**，所需角色为拥有该权限的角色
```

## 6. 前端

### RoleManage.vue

**文件**：`src/views/RoleManage.vue`

```vue
<script setup lang="ts">
// 角色表格 + 新增/编辑弹窗 + 权限分配对话框（el-tree checkbox）
// 使用 api/rbac.js 中的 getRoles/createRole/updateRole/deleteRole/assignPermissions
</script>
```

### Admin.vue 用户角色分配改造

**文件**：`src/views/Admin.vue`（修改）

```html
<!-- 用户列表的"角色"列：从 text 改为 el-select 下拉多选 -->
<el-select v-model="row.roleIds" multiple @change="assignRoles(row)">
  <el-option v-for="r in allRoles" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
</el-select>
```

### api/rbac.js

**文件**：`src/api/rbac.js`

```js
export async function getRoles() {
  return http.get('/admin/roles')
}
export async function createRole(data) {
  return http.post('/admin/roles', data)
}
export async function updateRole(id, data) {
  return http.put(`/admin/roles/${id}`, data)
}
export async function deleteRole(id) {
  return http.delete(`/admin/roles/${id}`)
}
export async function getPermissionTree() {
  return http.get('/admin/permissions/tree')
}
export async function getRolePermissions(roleId) {
  return http.get(`/admin/roles/${roleId}/permissions`)
}
export async function assignRolePermissions(roleId, ids) {
  return http.put(`/admin/roles/${roleId}/permissions`, { permissionIds: ids })
}
export async function getUserRoles(userId) {
  return http.get(`/admin/users/${userId}/roles`)
}
export async function assignUserRoles(userId, roleIds) {
  return http.put(`/admin/users/${userId}/roles`, { roleIds })
}
```

## 7. 文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `domain/user/RoleEntity.java` | 新增 | 角色实体 |
| `domain/user/PermissionEntity.java` | 新增 | 权限实体 |
| `domain/user/UserRoleEntity.java` | 新增 | 用户角色关联 |
| `domain/user/RolePermissionEntity.java` | 新增 | 角色权限关联 |
| `mapper/RoleMapper.java` | 新增 | |
| `mapper/PermissionMapper.java` | 新增 | |
| `mapper/UserRoleMapper.java` | 新增 | |
| `mapper/RolePermissionMapper.java` | 新增 | |
| `service/RoleService.java` | 新增 | 角色+分配逻辑 |
| `service/PermissionService.java` | 新增 | 权限树 |
| `vo/user/PermissionVO.java` | 新增 | 权限树 VO |
| `controller/AdminController.java` | 修改 | +10 端点 |
| `config/security/SecurityConfig.java` | 修改 | 动态权限 |
| `api/rbac.js` | 新增 | 前端 API |
| `views/RoleManage.vue` | 新增 | 角色管理页 |
| `views/Admin.vue` | 修改 | 角色下拉 |
| `router/index.js` | 修改 | +/roles 路由 |
| `test/.../RbacTest.java` | 新增 | 测试 |
