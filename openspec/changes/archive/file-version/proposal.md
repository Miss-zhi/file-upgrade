# 文件版本历史：快照记录 + 版本回滚

## Why

文件可能被反复修改，用户需要追溯历史版本并能够在需要时回滚到之前的版本。当前系统仅保留最新版本，没有历史记录。

## What Changes

### 后端

1. **FileVersion Entity**（双 ORM）：id / fileId / fileName / filePath / fileSize / version / userId / storagePath / createTime
2. **FileVersionMapper**：继承 BaseMapper
3. **FileVersionService**：saveVersion / listVersions / restoreVersion / cleanupOldVersions
4. **FileVersionController**：GET /file/{fileId}/versions、POST /file/{fileId}/restore/{versionId}
5. **FileService 集成**：upload 和 rename 时自动调用 saveVersion，保留最近 10 个版本

### 前端

1. **VersionHistory.vue**：版本列表对话框（版本号/大小/时间/回滚按钮）
2. **FileTable.vue**：右键菜单增加"版本历史"入口
3. **FileManager.vue**：集成 VersionHistory 对话框

### 不涉及

- 版本差异化对比（diff）
- 版本合并
- 文件夹版本管理

## Impact

- **新增**：FileVersion.java, FileVersionMapper.java, FileVersionService.java, FileVersionController.java, VersionHistory.vue
- **修改**：FileService.upload（+saveVersion），FileTable.vue（+菜单项），FileManager.vue（+对话框）
