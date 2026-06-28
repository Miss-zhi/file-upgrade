# 管理面板

## ADDED Requirements

### Requirement: 文件统计
GET /admin/stats SHALL return totalFileCount and totalFileSize

### Requirement: 用户统计
Stats SHALL include registered user count

### Requirement: 系统配置
GET/PUT /admin/config SHALL read/write site settings (storageType, siteName)

### Requirement: 前端仪表盘
Dashboard.vue SHALL render stat cards + config form

### Requirement: CI 兼容
mvn compile + vue-tsc + vite build pass
