# 文件回收站

## What Changes

### 后端
1. **FileBean**：添加 `@TableLogic` deleted 字段，原 delete 改为软删除
2. **IFileService + FileService**：新增 restore、permanentDelete、listDeleted
3. **FileController**：新增 POST /file/recycle（回收站列表）、POST /file/restore、DELETE /file/permanent/{id}
4. **test yml**：加回 logic-delete 配置

### 前端
1. **RecycleBin.vue**：回收站页面（列表+恢复+彻底删除+清空）
2. **router**：添加 /recycle 路由
