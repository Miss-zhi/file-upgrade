# 文件管理 CRUD — 上传/下载/删除/列表

## Purpose

实现奇文网盘文件管理的核心 CRUD 操作：按路径查询文件列表、上传文件元数据、下载文件、删除文件（逻辑删除）、创建文件夹。

## ADDED Requirements

### Requirement: 文件实体
FileBean Entity SHALL use @Entity + @TableName with snowflake ID, and SHALL include fileName/filePath/fileSize/isFolder/parentPath/userId fields

#### Scenario: FileBean 可编译
- **GIVEN** 数据库连接配置就绪
- **WHEN** 执行 mvn compile
- **THEN** FileBean.java 编译通过

### Requirement: 文件列表查询
POST /file/list SHALL accept a path parameter and return files/folders in that directory for the authenticated user

#### Scenario: 查询根目录
- **GIVEN** 用户已认证，根目录有 2 个文件和 1 个文件夹
- **WHEN** POST /file/list { path: "/" }
- **THEN** 返回 RestResult { dataList: [...], total: 3 }

### Requirement: 文件上传
POST /file/upload SHALL accept multipart file + path, save metadata to DB, and return FileVO

#### Scenario: 上传文件
- **GIVEN** 用户已认证
- **WHEN** POST /file/upload (multipart) 携带文件
- **THEN** 返回 RestResult { success: true, data: { id, fileName, fileSize } }

### Requirement: 文件删除
POST /file/delete SHALL accept file ID and perform logical delete

#### Scenario: 删除文件
- **GIVEN** 文件存在
- **WHEN** POST /file/delete { id: "..." }
- **THEN** 文件标记为已删除，列表不再显示

### Requirement: 文件夹创建
POST /file/create-folder SHALL create a folder record at specified path

#### Scenario: 创建文件夹
- **GIVEN** 用户在 /docs/ 目录
- **WHEN** POST /file/create-folder { path: "/docs/", folderName: "notes" }
- **THEN** 返回成功，列表中出现新文件夹

### Requirement: 前端文件管理页
File.vue SHALL render breadcrumb navigation, upload button, and file list table; FileList.vue SHALL distinguish files from folders with appropriate icons and click actions

#### Scenario: 文件列表渲染
- **GIVEN** 文件管理页加载
- **WHEN** 获取到文件列表数据
- **THEN** 表格展示文件名、大小、修改时间，文件夹可点击进入

### Requirement: CI 兼容
Backend SHALL pass mvn test, frontend SHALL pass npx vite build

#### Scenario: 后端测试通过
- **WHEN** 执行 mvn test -Dspring.profiles.active=test
- **THEN** FileServiceTest 全部通过

#### Scenario: 前端构建通过
- **WHEN** 执行 npx vite build
- **THEN** 无编译错误
