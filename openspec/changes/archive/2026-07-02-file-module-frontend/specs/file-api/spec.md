## ADDED Requirements

### Requirement: 文件列表 API 封装
前端 SHALL 封装 `GET /api/v1/file/getfilelist` 端点，接受 filePath、fileType、page、size、order、sort 参数，返回分页文件列表。函数 MUST 解包 RestResult 并返回 `PageResult<FileInfo>`。

#### Scenario: 获取根目录文件列表
- **WHEN** 调用 `getFileList({ filePath: '/', page: 0, size: 20 })`
- **THEN** 发送 GET 请求到 `/api/v1/file/getfilelist?filePath=/&page=0&size=20`
- **THEN** 返回解包后的分页文件列表

#### Scenario: 获取指定路径文件列表
- **WHEN** 调用 `getFileList({ filePath: '/文档', page: 0, size: 50 })`
- **THEN** 发送 GET 请求并返回该路径下的文件列表

### Requirement: 按分类浏览 API 封装
前端 SHALL 封装 `GET /api/v1/file/getfilelist/bycategory` 端点，接受 category、page、size 参数。

#### Scenario: 按图片分类浏览
- **WHEN** 调用 `getFileListByCategory({ category: 'image', page: 0, size: 20 })`
- **THEN** 发送 GET 请求并返回所有图片文件的分页列表

### Requirement: 文件操作 API 封装
前端 SHALL 封装重命名、移动、批量移动、复制、批量复制、创建文件夹、创建文件、文件详情、文件树等 9 个端点。

#### Scenario: 重命名文件
- **WHEN** 调用 `renameFile({ userFileId: 1, newName: '新名称.txt' })`
- **THEN** 发送 POST 请求到 `/api/v1/file/renamefile`，返回 void

#### Scenario: 移动文件
- **WHEN** 调用 `moveFile({ userFileId: 1, targetFolderId: 5 })`
- **THEN** 发送 POST 请求到 `/api/v1/file/movefile`，返回 void

#### Scenario: 批量移动文件
- **WHEN** 调用 `batchMoveFile({ userFileIds: [1, 2, 3], targetFolderId: 5 })`
- **THEN** 发送 POST 请求到 `/api/v1/file/batchmovefile`，返回 BatchOperationResult

#### Scenario: 复制文件
- **WHEN** 调用 `copyFile({ userFileId: 1, targetFolderId: 5 })`
- **THEN** 发送 POST 请求到 `/api/v1/file/copyfile`，返回 void

#### Scenario: 批量复制文件
- **WHEN** 调用 `batchCopyFile({ userFileIds: [1, 2], targetFolderId: 5 })`
- **THEN** 发送 POST 请求到 `/api/v1/file/batchcopyfile`，返回 BatchOperationResult

#### Scenario: 创建文件夹
- **WHEN** 调用 `createFolder({ folderName: '新文件夹', filePath: '/' })`
- **THEN** 发送 POST 请求到 `/api/v1/file/createfold`，返回新建文件夹 ID

#### Scenario: 创建文件
- **WHEN** 调用 `createFile({ fileName: '新建文档', filePath: '/', extendName: 'docx' })`
- **THEN** 发送 POST 请求到 `/api/v1/file/createfile`，返回新建文件 ID

#### Scenario: 获取文件详情
- **WHEN** 调用 `getFileDetail(1)`
- **THEN** 发送 GET 请求到 `/api/v1/file/getfiledetail/1`，返回 FileDetail

#### Scenario: 获取文件树
- **WHEN** 调用 `getFileTree()`
- **THEN** 发送 GET 请求到 `/api/v1/file/getfiletree`，返回 TreeNode 数组

### Requirement: 上传下载 API 封装
前端 SHALL 封装普通上传、秒传、分片上传初始化/上传/合并、下载、批量下载等 7 个端点。

#### Scenario: 普通上传
- **WHEN** 调用 `uploadFile(file, '/')`
- **THEN** 发送 POST multipart 请求到 `/api/v1/filetransfer/upload`，返回 UploadResult

#### Scenario: 秒传检测
- **WHEN** 调用 `speedUpload({ fileName, filePath, fileSize, fileHash })`
- **THEN** 发送 POST 请求到 `/api/v1/filetransfer/upload/speed`，秒传成功返回 UploadResult，失败返回 null

#### Scenario: 初始化分片上传
- **WHEN** 调用 `initChunkUpload({ fileName, filePath, fileSize, fileHash, totalChunks })`
- **THEN** 发送 POST 请求到 `/api/v1/filetransfer/upload/chunk/init`，返回 taskId

#### Scenario: 上传分片
- **WHEN** 调用 `uploadChunk(taskId, chunkIndex, chunkBlob)`
- **THEN** 发送 POST multipart 请求到 `/api/v1/filetransfer/upload/chunk`

#### Scenario: 合并分片
- **WHEN** 调用 `mergeChunks(taskId, '/')`
- **THEN** 发送 POST 请求到 `/api/v1/filetransfer/upload/chunk/merge`，返回 UploadResult

#### Scenario: 下载文件
- **WHEN** 调用 `downloadFile(1)`
- **THEN** 触发浏览器下载到 `/api/v1/filetransfer/download/1`

#### Scenario: 批量下载
- **WHEN** 调用 `batchDownload([1, 2, 3])`
- **THEN** 触发浏览器批量下载，POST 到 `/api/v1/filetransfer/batch-download`

### Requirement: 回收站 API 封装
前端 SHALL 封装回收站列表、删除、批量删除、恢复、永久删除、清空等 6 个端点。

#### Scenario: 获取回收站列表
- **WHEN** 调用 `getRecycleList(0, 20)`
- **THEN** 发送 GET 请求到 `/api/v1/recycle/list?page=0&size=20`，返回分页文件列表

#### Scenario: 删除文件到回收站
- **WHEN** 调用 `deleteFile(1)`
- **THEN** 发送 POST 请求到 `/api/v1/recycle/deletefile`

#### Scenario: 批量删除到回收站
- **WHEN** 调用 `batchDeleteFile([1, 2, 3])`
- **THEN** 发送 POST 请求到 `/api/v1/recycle/batchdeletefile`

#### Scenario: 恢复文件
- **WHEN** 调用 `restoreFile([1, 2])`
- **THEN** 发送 POST 请求到 `/api/v1/recycle/restorefile`

#### Scenario: 永久删除
- **WHEN** 调用 `deletePermanent([1])`
- **THEN** 发送 POST 请求到 `/api/v1/recycle/deletepermanent`

#### Scenario: 清空回收站
- **WHEN** 调用 `deleteAllRecycle()`
- **THEN** 发送 POST 请求到 `/api/v1/recycle/deleteall`

### Requirement: 分享 API 封装
前端 SHALL 封装创建分享、查看分享、验证提取码、下载分享文件、我的分享列表、取消分享等 6 个端点。

#### Scenario: 创建分享
- **WHEN** 调用 `createShare({ userFileId: 1, expireType: 7 })`
- **THEN** 发送 POST 请求到 `/api/v1/share/createshare`，返回 ShareInfo

#### Scenario: 查看分享信息
- **WHEN** 调用 `getShareInfo('abc12345')`
- **THEN** 发送 GET 请求到 `/api/v1/share/info/abc12345`，返回 ShareInfo

#### Scenario: 验证提取码
- **WHEN** 调用 `verifyShare('abc12345', '1234')`
- **THEN** 发送 POST 请求到 `/api/v1/share/verifyshare`，验证成功返回 ShareInfo

#### Scenario: 下载分享文件
- **WHEN** 调用 `downloadShareFile('abc12345')`
- **THEN** 触发浏览器下载到 `/api/v1/share/download/abc12345`

#### Scenario: 获取我的分享列表
- **WHEN** 调用 `getMyShares()`
- **THEN** 发送 GET 请求到 `/api/v1/share/myshares`，返回 ShareInfo 数组

#### Scenario: 取消分享
- **WHEN** 调用 `cancelShare(1)`
- **THEN** 发送 DELETE 请求到 `/api/v1/share/cancelshare/1`
