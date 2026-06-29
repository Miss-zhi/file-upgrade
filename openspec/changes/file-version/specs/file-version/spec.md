# 文件版本历史 — 详细需求规格

## ADDED Requirements

### Requirement: 版本记录存储
FileVersion entity SHALL store metadata for each file version: id, fileId, fileName, filePath, fileSize, version number, userId, storagePath, createTime

#### Scenario: 首次上传创建版本1
- **GIVEN** 文件不存在
- **WHEN** 用户上传文件 file.txt
- **THEN** FileVersion 表新增一条记录，version=1，storagePath 指向 UFOP 存储路径

#### Scenario: 覆盖上传创建版本2
- **GIVEN** 文件 file.txt 已存在（版本1）
- **WHEN** 用户重新上传同名文件
- **THEN** 原文件元数据更新，FileVersion 新增 version=2 的记录

### Requirement: 版本列表查询
GET /file/{fileId}/versions SHALL return a list of all versions for that file, ordered by version DESC

#### Scenario: 查询版本列表
- **GIVEN** 文件有 3 个版本历史
- **WHEN** GET /file/file123/versions
- **THEN** 返回 [{version:3,...}, {version:2,...}, {version:1,...}]

### Requirement: 版本回滚
POST /file/{fileId}/restore/{versionId} SHALL revert the current file metadata to the specified version's snapshot, and create a new version entry for the rollback action

#### Scenario: 回滚到版本1
- **GIVEN** 文件当前为版本3
- **WHEN** POST /file/file123/restore/version1Id
- **THEN** 文件元数据（fileName/filePath/fileSize）恢复为版本1的值，生成新版本4

### Requirement: 版本数量限制
FileService.saveVersion SHALL keep at most 10 versions per file, removing the oldest when exceeding

#### Scenario: 超过10个版本时清理
- **GIVEN** 文件已有10个版本
- **WHEN** 再次上传触发 saveVersion
- **THEN** 最旧的版本（version=1）被删除，新版本（version=11）被创建

### Requirement: 前端版本历史入口
FileTable.vue SHALL provide a "版本历史" menu item for files, which opens VersionHistory.vue dialog

#### Scenario: 用户查看版本历史
- **GIVEN** 文件列表展示
- **WHEN** 用户展开右键菜单 → 点击"版本历史"
- **THEN** VersionHistory 对话框显示所有版本（版本号/大小/时间/回滚按钮）

### Requirement: CI 兼容
Backend SHALL pass mvn test (including FileVersionTest), frontend SHALL pass vue-tsc + vite build

#### Scenario: 全量测试通过
- **WHEN** 执行 mvn test + vue-tsc + vite build
- **THEN** 全部成功，无失败
