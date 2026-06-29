# 操作日志

## What Changes

### 后端
1. **OperationLog Entity**：复用 AOP `@MyLog` 切面存储的数据
2. **OperationLogMapper** + **OperationLogService**：分页查询
3. **AdminController**：GET /admin/logs（分页+时间筛选+类型筛选）

### 前端
1. **OperationLog.vue**：日志表格+日期筛选+操作类型筛选
2. **router**：/operation-log 路由
3. **api/admin.js**：getOperationLogs
