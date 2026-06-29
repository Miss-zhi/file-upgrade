# UFOP 远程存储完整实现

## Why

当前 UFOP 模块仅实现了 LocalStorage 7 个操作类。原项目有完整的 AliyunOSS/Minio/FastDFS/Qiniuyun 4 种远程存储各 7 操作实现，共 28 个类。需补全以实现多存储后端接入能力。

## What Changes

| 存储 | Uploader | Downloader | Deleter | Reader | Writer | Renamer | Copier |
|---|---|---|---|---|---|---|---|
| LocalStorage | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| AliyunOSS | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Minio | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| FastDFS | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |
| Qiniuyun | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ | ❌→✅ |

### 架构对齐

完全参照原项目 `E:/file/` 的目录结构：
- `operation/<op>/product/<Storage>Operator.java` 格式
- 每个操作有对应的 domain 对象（CopyFile/DeleteFile/DownloadFile/ReadFile/WriteFile/UploadFile）
- UFOPFactory 根据 StorageType 自动路由

### 不涉及
- Previewer 操作（另立 ufop-preview）
- Config/AutoConfiguration/Util 辅助类（另立 ufop-utils）
- Docker 容器 → CI 中跳过远程存储测试（@Disabled 当无对应服务时）
