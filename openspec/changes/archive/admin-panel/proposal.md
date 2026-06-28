# 管理面板：统计仪表盘 + 系统配置

## What Changes

### 后端
1. **SysConfigService**：读写系统配置（存储类型/站点名称等）
2. **StatsService**：文件统计（总数/总大小）、用户统计
3. **AdminController 扩展**：/admin/stats（文件+用户统计）、/admin/config（GET/PUT 系统配置）
4. **SysConfig Entity + Mapper**

### 前端
1. **Dashboard.vue**：仪表盘页面（4 统计卡片 + 系统配置表单）
2. **router**：/admin 默认展示 Dashboard
3. **api/admin.js**：扩展统计和配置接口
