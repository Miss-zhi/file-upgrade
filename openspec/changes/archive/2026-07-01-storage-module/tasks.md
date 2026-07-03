## 1. 依赖与配置准备

- [x] 1.1 在 pom.xml 的 `<dependencyManagement>` 中锁定 4 个 SDK 版本号，并在 `<dependencies>` 中添加依赖：`io.minio:minio:8.5.7`、`com.aliyun.oss:aliyun-sdk-oss:3.17.4`、`com.qiniu:qiniu-java-sdk:7.15.1`、`com.github.tobato:fastdfs-client:1.27.2`
- [x] 1.2 在 `StorageProperties` 中补充缺失的配置字段（如 `Qiniu.expireSeconds`、`Fastdfs.group`、`Minio.presignedUrlExpiry`）
- [x] 1.3 在 application.yml 中添加所有后端的完整配置示例（含注释，默认仅激活 local）

## 2. Local 后端修复（storage-local-fix）

- [x] 2.1 修复 `LocalStorageBackend.resolvePath()`：增加 `normalize()` + `startsWith(basePath)` 路径遍历防护校验，非法路径抛 `SecurityException`
- [x] 2.2 修复 `LocalStorageBackend.downloadRange()`：改用 try-with-resources 包裹 `RandomAccessFile`，确保异常时资源释放
- [x] 2.3 补充 LocalStorageBackend 单元测试：`write()` / `getPreviewUrl()` / `checkConnectivity()` 断言 / 路径遍历安全测试

## 3. MinIO 后端实现（storage-minio）

- [x] 3.1 实现 `MinioStorageBackend`：注入 `MinioClient`，实现全部 12 个接口方法（upload/download/downloadRange/getFileSize/copy/delete/getPreviewUrl/exists/checkConnectivity/read/write）
- [x] 3.2 创建 `MinioStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "minio")`，注册 `MinioClient` 单例 Bean + `MinioStorageBackend` Bean
- [x] 3.3 在 `MinioStorageAutoConfiguration` 中实现 `checkAndCreateBucket()`：启动时检查 bucket 是否存在，不存在则自动创建
- [x] 3.4 注册 `MinioStorageAutoConfiguration` 到 `AutoConfiguration.imports`
- [x] 3.5 编写 MinIO 后端单元测试（Mock `MinioClient`，验证各操作参数传递）

## 4. Aliyun OSS 后端实现（storage-aliyun-oss）

- [x] 4.1 实现 `AliyunOssStorageBackend`：注入 `OSS` 客户端，实现全部 12 个接口方法
- [x] 4.2 创建 `AliyunOssStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "aliyun")`，注册 `OSS` 单例 Bean（`destroyMethod = "shutdown"`）+ `AliyunOssStorageBackend` Bean
- [x] 4.3 注册 `AliyunOssStorageAutoConfiguration` 到 `AutoConfiguration.imports`
- [x] 4.4 编写 Aliyun OSS 后端单元测试（Mock `OSS` 客户端）

## 5. Qiniu 后端实现（storage-qiniu）

- [x] 5.1 实现 `QiniuStorageBackend`：注入 `Auth` / `UploadManager` / `BucketManager`，实现全部 12 个接口方法
- [x] 5.2 创建 `QiniuStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "qiniu")`，注册 `Auth` / `UploadManager` / `BucketManager` 单例 Bean + `QiniuStorageBackend` Bean
- [x] 5.3 注册 `QiniuStorageAutoConfiguration` 到 `AutoConfiguration.imports`
- [x] 5.4 编写 Qiniu 后端单元测试（Mock `Auth` / `UploadManager` / `BucketManager`）

## 6. FastDFS 后端实现（storage-fastdfs）

- [x] 6.1 实现 `FastDfsStorageBackend`：注入 `FastFileStorageClient`，实现全部 12 个接口方法（含 downloadRange 降级、copy 降级、getPreviewUrl 返回 null、storagePath group/path 解析）
- [x] 6.2 创建 `FastDfsStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "fastdfs")`，引入 tobato `FdfsClientConfig` + 注册 `FastDfsStorageBackend` Bean
- [x] 6.3 注册 `FastDfsStorageAutoConfiguration` 到 `AutoConfiguration.imports`
- [x] 6.4 编写 FastDFS 后端单元测试（Mock `FastFileStorageClient`，含 storagePath 解析测试）

## 7. 基础设施增强（storage-infrastructure）

- [x] 7.1 在 `StorageFactory` 中新增 `getBackendForStorageType(String storageType)` 方法：按存储类型字符串路由，未注册时 fallback 到全局活跃后端 + `log.warn`（参数为 String 而非 FileBean，避免 storage 反向依赖 file）
- [x] 7.2 编写 `StorageFactory` 单元测试：验证 `getBackend()` / `getBackend(type)` / `getBackendForStorageType()` / 未知类型异常 / fallback 行为
- [x] 7.3 编写 `StorageHealthChecker` 单元测试：验证连通性验证成功/失败/内容校验失败场景
- [x] 7.4 编写 `StorageProperties` 配置绑定测试：验证默认值和 yml 绑定

## 8. 验证

- [x] 8.1 确保全部单元测试通过（`mvn test`）
- [x] 8.2 确保编译无错误和警告（`mvn compile`）
- [x] 8.3 验证 4 个 AutoConfiguration 类在 `AutoConfiguration.imports` 中正确注册
