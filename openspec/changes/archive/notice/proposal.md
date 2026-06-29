# 系统通知公告

## What Changes

### 后端
1. Notice Entity（title/content/platform/isLongValid/validDateTime/createTime/createUserId）
2. NoticeMapper
3. INoticeService + NoticeService：分页查询、发布、更新、删除
4. NoticeController：GET /notice/list + POST /notice + PUT /notice/{id} + DELETE /notice/{id}

### 前端
1. NoticeList.vue：公告列表（卡片展示，时间筛选，platform=网盘）
2. NoticeEdit.vue：管理端发布/编辑弹窗（markdown编辑）
3. router：/notices 路由
4. api/notice.js
