# storage-module — 统一文件存储（UFOP）模块升级提案

## 背景

storage 模块是 UFOP（统一文件操作平台）框架，为所有模块提供文件读写、上传下载、复制删除等存储抽象。当前只有 Local 后端完整实现（12/12 方法），其余 4 个云存储后端（MinIO、Aliyun OSS、Qiniu Kodo、FastDFS）全部是 stub 实现——方法体抛 `UnsupportedOperationException`，无 `@Component` 注解，无 AutoConfiguration 类，无 SDK 依赖，配置 `storage.type=minio` 直接 `IllegalStateException`。

此外 Local 后端存在路径遍历安全漏洞和 `downloadRange()` 资源泄露问题。

### 当前问题清单

| # | 问题 | 严重性 |
|---|------|--------|
| 1 | `LocalStorageBackend.resolvePath()` 无路径校验，`../../etc/passwd` 可逃逸 basePath | 安全漏洞 |
| 2 | MinIO 后端全部方法抛 `UnsupportedOperationException`，无 AutoConfig/SDK | 功能缺失 |
| 3 | Aliyun OSS 后端同上 | 功能缺失 |
| 4 | Qiniu Kodo 后端同上 | 功能缺失 |
| 5 | FastDFS 后端同上 | 功能缺失 |
| 6 | `downloadRange()` 使用 `RandomAccessFile` 未 try-with-resources，异常时资源泄露 | 资源泄露 |
| 7 | 4 个 stub 后端无 `AutoConfiguration.imports` 注册，运行时不可发现 | 配置缺失 |
| 8 | pom.xml 缺少 4 个第三方存储 SDK 依赖 | 编译阻断 |
| 9 | 无 `StorageFactory` 单元测试，后端选择逻辑无覆盖 | 测试缺口 |
| 10 | `StorageHealthChecker` 无测试 | 测试缺口 |

### 旧项目 UFOP 设计评价

旧项目存储模块有 35 个 product 类（7 操作抽象类 x 5 后端），加上 domain 对象、Factory、工具类共 60+ 文件。设计上有两个值得借鉴的模式和若干应避免的反模式：

**值得借鉴：**
- 按文件级存储类型路由：`UFOPFactory.getDownloader(int storageType)` 允许不同文件存在不同后端，支持存储迁移场景
- Redis 协调的分片上传：分布式锁 + 分片序号追踪 + 乱序等待，内建在 `Uploader` 基类中

**应避免：**
- Aliyun 类型泄露到抽象基类（`abstract class Downloader` 中 `import com.aliyun.oss.OSS`）
- SDK 客户端每次操作 `new` 一个（无连接复用）
- DI 策略混用（`new` 和 `@Resource` 混合）
- 静态全局状态（`UFOPUtils.LOCAL_STORAGE_PATH`）
- 异常吞掉用 `e.printStackTrace()`

## 升级目标

实现全部 5 个存储后端的完整功能，修复 Local 安全漏洞，补齐 AutoConfiguration 注册和 SDK 依赖，补充测试覆盖。保持新项目已有的扁平接口设计（5 个 `StorageBackend` 实现 vs 旧项目 35 个 product 类），同时从旧项目借鉴按文件级存储类型路由能力。

## Capabilities

### 1. storage-local-fix（Local 后端修复）

修复 Local 后端的安全漏洞和资源泄露，保持现有功能不变。

**范围：**
- 路径遍历防护：`resolvePath()` 增加 `normalize()` + `startsWith()` 校验，非法路径抛 `SecurityException`
- `downloadRange()` 改用 try-with-resources 包裹 `RandomAccessFile`
- 补充 `write()` 和 `getPreviewUrl()` 的单元测试
- 补充 `checkConnectivity()` 的断言（当前测试无 assertion）
- 补充路径遍历攻击的安全测试用例

**约束：**
- `resolvePath()` 必须在返回前验证解析后路径仍在 basePath 下
- 安全异常必须有明确的错误消息（包含被拒绝的路径）
- 不改变任何 public 方法签名

### 2. storage-minio（MinIO 后端实现）

实现 MinIO 对象存储的完整 StorageBackend，使用 MinIO Java SDK 8.5.x。

**范围：**
- `MinioStorageBackend`：实现全部 12 个接口方法
  - `upload()` / `write()`：`client.putObject(PutObjectArgs)`
  - `download()` / `read()`：`client.getObject(GetObjectArgs)` 返回 InputStream
  - `downloadRange()`：`GetObjectArgs` 带 `offset` 和 `length`
  - `getFileSize()`：`client.statObject(StatObjectArgs).size()`
  - `copy()`：`client.copyObject(CopyObjectArgs)`，同 bucket 内拷贝
  - `delete()`：`client.removeObject(RemoveObjectArgs)`
  - `getPreviewUrl()`：`client.getPresignedObjectUrl()`，默认 1 小时过期
  - `exists()`：`client.statObject()` catch `ErrorResponseException` 判断 404
  - `checkConnectivity()`：`client.bucketExists()`
- `MinioStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "minio")`
  - `MinioClient` 单例 Bean（连接池复用，不每次操作 new）
  - bucket 不存在时自动创建（启动时 `checkAndCreateBucket()`）
- 注册到 `AutoConfiguration.imports`
- 集成测试（Testcontainers MinIO 容器 或 Mock MinioClient）

**约束：**
- `MinioClient` 必须作为单例 `@Bean`，禁止每次操作 new builder
- endpoint / accessKey / secretKey / bucket 从 `StorageProperties.Minio` 读取，禁止硬编码
- 操作失败统一抛 `UncheckedIOException`（与 Local 后端一致）
- presigned URL 过期时间可配置（默认 1 小时）

### 3. storage-aliyun-oss（Aliyun OSS 后端实现）

实现阿里云 OSS 对象存储的完整 StorageBackend，使用 `aliyun-sdk-oss` 3.17.x。

**范围：**
- `AliyunOssStorageBackend`：实现全部 12 个接口方法
  - `upload()` / `write()`：`client.putObject(bucket, key, inputStream, metadata)`
  - `download()` / `read()`：`client.getObject(bucket, key).getObjectContent()`
  - `downloadRange()`：`GetObjectRequest.setRange(start, end)`
  - `getFileSize()`：`client.getObjectMetadata(bucket, key).getContentLength()`
  - `copy()`：`client.copyObject(bucket, srcKey, bucket, destKey)`
  - `delete()`：`client.deleteObject(bucket, key)`
  - `getPreviewUrl()`：`client.generatePresignedUrl(bucket, key, expiration)`
  - `exists()`：`client.doesObjectExist(bucket, key)`
  - `checkConnectivity()`：`client.doesBucketExist(bucket)`
- `AliyunOssStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "aliyun")`
  - `OSS` 客户端单例 Bean
  - AutoConfiguration 实现 `DisposableBean`，`destroy()` 时调用 `ossClient.shutdown()` 释放连接池
- 注册到 `AutoConfiguration.imports`
- 单元测试（Mock `OSS` 客户端）

**约束：**
- `OSS` 客户端必须在应用关闭时 `shutdown()`，通过 `@Bean(destroyMethod = "shutdown")` 或 `DisposableBean` 实现
- endpoint / accessKeyId / accessKeySecret / bucket 从 `StorageProperties.Aliyun` 读取
- 禁止在抽象层引入 `com.aliyun.oss` 包（旧项目反模式，Aliyun 类型不得污染 StorageBackend 接口）

### 4. storage-qiniu（Qiniu Kodo 后端实现）

实现七牛云 Kodo 对象存储的完整 StorageBackend，使用 `qiniu-java-sdk` 7.15.x。

**范围：**
- `QiniuStorageBackend`：实现全部 12 个接口方法
  - `upload()` / `write()`：`UploadManager.put(inputStream, key, getUpToken(), null, null)`
  - `download()` / `read()`：构建签名 URL -> `new URL(signedUrl).openStream()`
  - `downloadRange()`：HTTP GET 带 `Range: bytes=start-end` header
  - `getFileSize()`：`BucketManager.stat(bucket, key).getFsize()`
  - `copy()`：`BucketManager.copy(bucket, srcKey, bucket, destKey, true)`
  - `delete()`：`BucketManager.delete(bucket, key)`
  - `getPreviewUrl()`：`Auth.privateDownloadUrl(domain + "/" + key, expireSeconds)`
  - `exists()`：`BucketManager.stat()` catch `QiniuException` 判断错误码 612
  - `checkConnectivity()`：`BucketManager.listBucket(bucket, "", 1)`
- `QiniuStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "qiniu")`
  - `Auth` 单例 Bean（由 accessKey + secretKey 构建）
  - `UploadManager` 单例 Bean
  - `BucketManager` 单例 Bean
- 注册到 `AutoConfiguration.imports`
- 单元测试（Mock `Auth` / `UploadManager` / `BucketManager`）

**约束：**
- `Auth` / `UploadManager` / `BucketManager` 必须作为单例 Bean
- accessKey / secretKey / bucket / domain 从 `StorageProperties.Qiniu` 读取
- 上传 token 过期时间可配置（默认 1 小时）

### 5. storage-fastdfs（FastDFS 后端实现）

实现 FastDFS 分布式文件系统的完整 StorageBackend，使用 `fastdfs-client`（tobato）1.27.x。

**范围：**
- `FastDfsStorageBackend`：实现全部 12 个接口方法
  - `upload()` / `write()`：`client.uploadFile(bytes, ext, metaDataSet)` -> 返回 `StorePath`，storagePath 格式为 `group/path`
  - `download()` / `read()`：`client.downloadFile(group, path, DownloadByteArray)` -> byte[] -> ByteArrayInputStream
  - `downloadRange()`：FastDFS 无原生 range API，下载全量后截取（大文件场景记录 WARN 日志）
  - `getFileSize()`：`client.getFileInfo(group, path).getFileSize()`
  - `copy()`：下载源文件 -> 重新上传到目标路径（FastDFS 无服务端 copy）
  - `delete()`：`client.deleteFile(group, path)`
  - `getPreviewUrl()`：返回 `null`（FastDFS 无内置 HTTP 预览，需配合 Nginx）
  - `exists()`：`client.getFileInfo()` catch `FdfsServerException`
  - `checkConnectivity()`：`client.getTrackerClient().getTrackerServer()` 连通性测试
- `FastDfsStorageAutoConfiguration`：`@ConditionalOnProperty(name = "storage.type", havingValue = "fastdfs")`
  - 引入 tobato `FdfsClientConfig`（tracker 连接池配置）
  - `FastFileStorageClient` 由 tobato 自动配置注入
- 注册到 `AutoConfiguration.imports`
- 单元测试（Mock `FastFileStorageClient`）

**约束：**
- storagePath 格式为 `group/path`（如 `group1/M00/00/01/xxx.jpg`），解析时需拆分 group 和 path
- tracker server 地址从 `StorageProperties.Fastdfs.trackerServers` 读取（支持多节点逗号分隔）
- group 名称可配置（默认 `group1`），不硬编码
- `downloadRange()` 对大文件（>50MB）记录 WARN 日志，建议调用方避免 range 请求

### 6. storage-infrastructure（基础设施增强）

提升 StorageFactory 能力、补充测试覆盖、完善配置管理。

**范围：**
- `StorageFactory` 增强：
  - 新增 `getBackendForStorageType(String storageType)` 方法：按文件级 `storageType` 字段路由到对应后端（借鉴旧项目 `UFOPFactory.getDownloader(int storageType)` 设计）。调用方从 `FileBean.getStorageType()` 取值后传入，避免 storage 模块反向依赖 file 模块
  - 当前 `getBackend()` 仅返回全局活跃后端，`getBackend(String type)` 支持按类型获取但需要确保该后端已注册
- 测试补充：
  - `StorageFactory` 单元测试：验证后端选择、未知类型异常、按文件路由
  - `StorageHealthChecker` 单元测试：验证健康检查流程（写->读->删）和失败行为
  - `StorageProperties` 配置绑定测试：验证默认值和 yml 绑定
- application.yml 配置段：补充所有后端的完整配置示例（含注释说明）

**约束：**
- `getBackendForStorageType()` 在目标后端未注册时返回全局活跃后端并 log.warn（兼容旧文件迁移场景）
- 测试不依赖真实云存储环境（全部使用 Mock 或 Testcontainers）

## 与现有模块的关系

| 模块 | 关系 | 说明 |
|------|------|------|
| file/ | 被依赖 | file 模块所有文件上传/下载/删除通过 StorageFactory 调用 StorageBackend |
| search/ | 被依赖 | 内容索引时通过 StorageBackend.read() 读取文件内容 |
| document/ | 被依赖 | OnlyOffice 回调保存时通过 StorageBackend.write() 写回编辑后文件 |
| admin/ | 被依赖 | 管理端可查看存储健康状态 |
| auth/ | 无关 | 存储层不涉及用户认证 |

## 技术方案要点

### 接口设计（保持不变）

新项目已有的扁平接口设计（1 个 `StorageBackend` 组合接口，12 个方法）优于旧项目的 35 个 product 类方案，保持现有设计：

```
StorageBackend (composite)
+-- Uploader      -- upload()
+-- Downloader    -- download(), downloadRange(), getFileSize()
+-- Copier        -- copy()
+-- Deleter       -- delete()
+-- Previewer     -- getPreviewUrl()
+-- Reader        -- read(), exists()
+-- Writer        -- write()
+-- (own)         -- getStorageType(), checkConnectivity()
```

### SDK 客户端生命周期

每个后端的 SDK 客户端作为单例 `@Bean`，通过 AutoConfiguration 条件注册：

```java
// MinioStorageAutoConfiguration
@Bean
public MinioClient minioClient(StorageProperties props) {
    return MinioClient.builder()
        .endpoint(props.getMinio().getEndpoint())
        .credentials(props.getMinio().getAccessKey(), props.getMinio().getSecretKey())
        .build();
}
```

禁止每次操作 `new` 客户端（旧项目反模式）。Aliyun OSS 客户端必须在应用关闭时 `shutdown()`。

### AutoConfiguration 模式

每个后端一个 `@AutoConfiguration` 类，使用 `@ConditionalOnProperty` 条件激活：

```java
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageAutoConfiguration { ... }
```

注册到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。未激活的后端不创建任何 Bean，不引入运行时开销。

### 按文件存储类型路由（借鉴旧项目）

新增 `StorageFactory.getBackendForStorageType(String storageType)` 方法，支持不同文件存储在不同后端：

```
文件 A（storageType="local"）-> LocalStorageBackend
文件 B（storageType="minio"）-> MinioStorageBackend
```

方法接受 `String storageType` 参数（而非 `FileBean`），调用方从 `fileBean.getStorageType()` 取值后传入，避免 storage 模块反向依赖 file 模块。用于存储迁移场景：全局 `storage.type` 切换到新后端后，旧文件仍可通过文件级 storageType 正确访问。

### SDK 依赖版本

| SDK | 版本 | 说明 |
|-----|------|------|
| `io.minio:minio` | 8.5.7 | MinIO 官方 Java SDK |
| `com.aliyun.oss:aliyun-sdk-oss` | 3.17.4 | 阿里云 OSS 官方 SDK |
| `com.qiniu:qiniu-java-sdk` | 7.15.1 | 七牛云官方 SDK |
| `com.github.tobato:fastdfs-client` | 1.27.2 | FastDFS tobato 客户端（社区维护） |

## 不在范围内

- 分片上传的 Redis 协调机制（属于 file 模块的上传流程，不在 storage 抽象层）
- 缩略图生成 / 图片处理（可作为 Previewer 扩展，后续迭代）
- 存储后端热切换（运行时动态切换 storage.type，需要分布式配置中心支持）
- 对象存储跨区域复制（云厂商原生功能，不在应用层实现）
- 存储用量统计/计费（属于 admin 模块）

## 影响评估

| 影响项 | 说明 |
|--------|------|
| 新增文件 | ~14 个 Java 文件（4 个 AutoConfiguration + ~10 个测试） |
| 修改文件 | 4 个 stub 实现 -> 完整实现、LocalStorageBackend（安全修复）、StorageFactory（增强）、pom.xml（SDK 依赖）、AutoConfiguration.imports（注册） |
| 新增依赖 | `minio`、`aliyun-sdk-oss`、`qiniu-java-sdk`、`fastdfs-client` |
| Flyway | 无新表 |
| 外部依赖 | 需要 MinIO / Aliyun OSS / Qiniu / FastDFS 服务实例（按配置激活） |
