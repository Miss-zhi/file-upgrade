# UFOP 本地存储实现

## Why

当前文件只有元数据，文件内容未实际存储。实现 UFOP 本地存储操作类，打通文件上传/下载的物理读写。

## What Changes

### 后端
1. **LocalStorageUploader/Downloader/Deleter/Reader/Writer/Renamer/Copier** — 7 个本地存储实现
2. **UFOPFactory** — 重写，根据配置返回 LocalStorage* 实例
3. **UFOPConfigProperties** — 读取 ufop.local.root-path 配置
4. **FileService.upload** — 调用 UFOP Uploader 写入文件
5. **FileController.download** — 调用 UFOP Downloader 返回文件流
6. **FileService.delete** — 调用 UFOP Deleter 删除物理文件
