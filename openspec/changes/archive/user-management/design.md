# Design: user-management

## API

| 方法 | 端点 | 说明 |
|---|---|---|
| POST | /admin/user/list | 分页搜索用户列表 |
| PUT | /admin/user | 更新用户信息 |
| DELETE | /admin/user/{id} | 删除用户 |
| PUT | /admin/user/{id}/status?enabled=true | 启用/禁用用户 |

## 分页

使用 MyBatis-Plus `Page<T>` + `IPage<T>`：

```java
IPage<User> page = new Page<>(pageNum, pageSize);
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
if (StrUtil.isNotBlank(keyword)) {
    wrapper.like(User::getUsername, keyword)
           .or().like(User::getEmail, keyword);
}
userMapper.selectPage(page, wrapper);
```

返回格式：
```json
{ "success": true, "data": { "records": [...], "total": 100, "size": 10, "current": 1 } }
```

## 前端页面

```
Admin.vue
├── 搜索栏（el-input + 搜索按钮）
├── 用户表格
│   ├── 头像/用户名/邮箱/角色/状态/创建时间
│   └── 操作列（编辑 / 启禁 / 删除）
├── 分页器（el-pagination）
└── UserEditDialog.vue
```

## 文件清单

### 后端
```
dto/user/UserQueryDTO.java (新增)
dto/user/UserUpdateDTO.java (新增)
vo/user/UserAdminVO.java (新增)
domain/user/User.java (修改: +status/enabled字段)
api/IUserService.java (修改)
service/UserService.java (修改)
controller/UserController.java (修改)
test/.../UserServiceTest.java (修改)
```

### 前端
```
views/Admin.vue (重写)
components/admin/UserEditDialog.vue (新建)
stores/user.js (修改)
api/admin.js (修改)
```
