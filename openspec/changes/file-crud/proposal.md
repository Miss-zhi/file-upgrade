# 文件管理 CRUD：上传/下载/删除/列表

## Why

文件管理是奇文网盘的核心功能。用户需要能浏览文件列表、上传文件、下载文件、删除文件、创建文件夹。已有用户认证模块，文件操作需要 JWT 认证。

## What Changes

### 后端

1. **FileBean Entity** (`domain/file/FileBean.java`)：双注解，字段：id / fileName / filePath / fileSize / fileType / isFolder / parentPath / userId / createTime / updateTime
2. **FileBeanMapper** (`mapper/FileBeanMapper.java`)：继承 BaseMapper
3. **IFileService + FileService**：listByPath、upload（仅记录元数据，上传逻辑后续 UFOP 实现）、delete（逻辑删除）、createFolder、getById
4. **FileController**：POST /file/list、POST /file/upload、GET /file/download/{id}、POST /file/delete、POST /file/create-folder
5. **DTO/VO**：ListFileDTO、UploadFileDTO、DeleteFileDTO、FileVO

### 前端

1. **File.vue**（重写占位页）：面包屑 + 上传按钮 + 文件卡片/表格 + 右键菜单
2. **FileList 组件**：文件列表渲染，支持文件夹/文件图标区分
3. **stores/fileList.js**（重写骨架）：对接后端 API
4. **api/file.js**（完善）：list / upload / delete / createFolder / download

### 不涉及

- 大文件分片上传（留待后续）
- 真正的文件存储上传（UFOP 本地实现留待后续，现在仅记录元数据）
- 文件分享/全文检索

## Impact

- **后端新增**：FileBean.java、FileBeanMapper.java、IFileService.java、FileService.java、FileController.java、ListFileDTO.java、UploadFileDTO.java、DeleteFileDTO.java、FileVO.java
- **前端新增/修改**：File.vue（重写）、components/file/FileList.vue（新建）、stores/fileList.js（重写）、api/file.js（完善）
