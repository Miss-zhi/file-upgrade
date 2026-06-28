# 文件分享：创建/提取码/匿名下载/管理

## Why

用户需要将文件分享给他人。分享功能需生成唯一链接（带提取码和过期时间），被分享者可匿名访问下载。管理员可查看和取消分享记录。

## What Changes

### 后端

1. **ShareFile Entity**：id/shareBatchNum/userId/filePath/shareToken/shareCode/expireTime/createTime
2. **ShareFileMapper**：继承 BaseMapper
3. **IShareFileService + ShareFileService**：createShare/cancelShare/listShares/verifyShareCode/getShareByToken
4. **ShareController**：POST /share/create、POST /share/list、POST /share/cancel、GET /share/verify、GET /anonymous/download/{token}
5. **SecurityConfig**：白名单添加 /anonymous/**
6. **DTO/VO**：CreateShareDTO、ShareVO

### 前端

1. **ShareDialog.vue**：分享对话框（设置过期时间+提取码，生成链接）
2. **Share.vue**：分享管理页面（分享列表+取消分享+复制链接）
3. **FileManager/FileTable**：增加"分享"操作入口
4. **api/share.js**：分享 API

## Impact
- **后端新增**：ShareFile、ShareFileMapper、IShareFileService、ShareFileService、ShareController、CreateShareDTO、ShareVO
- **后端修改**：SecurityConfig
- **前端新增**：ShareDialog.vue、api/share.js
- **前端修改**：Share.vue、FileTable.vue、FileManager.vue
