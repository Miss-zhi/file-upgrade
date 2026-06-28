# Design: file-crud

## 数据模型

```
FileBean
├── id (雪花ID)
├── fileName (文件名)
├── filePath (完整路径，如 /docs/note.txt)
├── fileSize (字节)
├── fileType (MIME 或扩展名)
├── isFolder (true=文件夹)
├── parentPath (父目录路径)
├── userId (拥有者)
├── createTime / updateTime
```

路径规则：`/` 为根目录，`/docs/` 为文件夹，`/docs/note.txt` 为文件。

## API 设计

| 方法 | 端点 | 说明 |
|---|---|---|
| POST | /file/list | 按路径列出文件和文件夹 |
| POST | /file/upload | 上传文件（记录元数据） |
| GET | /file/download/{id} | 下载文件（返回文件流占位） |
| POST | /file/delete | 删除文件/文件夹 |
| POST | /file/create-folder | 创建文件夹 |

## 文件列表查询

```
POST /file/list { path: "/" }
→ FileController.list(ListFileDTO)
→ FileService.listByPath(path, userId)
→ 查询 parentPath = path 的所有 FileBean
← RestResult { dataList: FileVO[], total }
```

返回以文件/文件夹的 FileVO 列表，前端据此渲染。

## 上传流程

```
POST /file/upload (multipart/form-data)
→ 接收 file + path 参数
→ 生成雪花 ID
→ 记录元数据到数据库
→ 返回 FileVO
（文件实体存储留待 UFOP 实现）
```

## 前端页面

```
文件管理页
├── 面包屑导航（当前路径，可点击跳转）
├── 工具栏（上传文件、新建文件夹、刷新）
└── 文件列表
    ├── 文件夹（点击进入）
    └── 文件（下载/删除操作）
```

## 文件清单

### 后端新增
```
domain/file/FileBean.java
mapper/FileBeanMapper.java
api/IFileService.java
service/FileService.java
controller/FileController.java
dto/file/ListFileDTO.java
dto/file/UploadFileDTO.java
dto/file/DeleteFileDTO.java
vo/file/FileVO.java
```

### 前端新增/修改
```
views/File.vue              (重写)
components/file/FileList.vue (新建)
stores/fileList.js          (重写)
api/file.js                 (完善)
```
