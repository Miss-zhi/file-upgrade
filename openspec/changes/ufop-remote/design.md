# Design: ufop-remote — 完整技术方案

## 1. 目录结构（与 LocalStorage 对齐）

```
ufop/operation/
├── Uploader.java                         已有
├── Downloader.java                       已有
├── Deleter.java                          已有
├── Reader.java                           已有
├── Writer.java                           已有
├── Renamer.java                          已有
├── Copier.java                           已有
│
├── copy/
│   └── product/
│       ├── LocalStorageCopier.java       已有
│       ├── AliyunOSSCopier.java          新增
│       ├── FastDFSCopier.java            新增
│       ├── MinioCopier.java              新增
│       └── QiniuyunKodoCopier.java       新增
├── delete/product/  ...A / ...F / ...M / ...Q    各自新增
├── download/product/ ...A / ...F / ...M / ...Q   各自新增
├── read/product/    ...A / ...F / ...M / ...Q    各自新增
├── upload/product/  ...A / ...F / ...M / ...Q    各自新增
├── write/product/   ...A / ...F / ...M / ...Q    各自新增
└── rename/product/                            (暂无 rename)
```

## 2. 操作接口现有签名（每个实现类遵循）

### Uploader
**文件**：`com.qiwenshare.ufop.operation.Uploader`

```java
public abstract class Uploader {
    public abstract void upload(InputStream inputStream, String fileUrl);
}
```

### Downloader
**文件**：`com.qiwenshare.ufop.operation.Downloader`

```java
public abstract class Downloader {
    public abstract InputStream getInputStream(String fileUrl);
}
```

### Deleter
**文件**：`com.qiwenshare.ufop.operation.Deleter`

```java
public abstract class Deleter {
    public abstract void delete(String fileUrl);
}
```

### Reader
**文件**：`com.qiwenshare.ufop.operation.Reader`

```java
public abstract class Reader {
    public abstract String read(String fileUrl);
}
```

### Writer
**文件**：`com.qiwenshare.ufop.operation.Writer`

```java
public abstract class Writer {
    public abstract void write(String fileUrl, String content);
}
```

### Renamer
**文件**：`com.qiwenshare.ufop.operation.Renamer`

```java
public abstract class Renamer {
    public abstract void rename(String fileUrl, String newFileUrl);
}
```

### Copier
**文件**：`com.qiwenshare.ufop.operation.Copier`

```java
public abstract class Copier {
    public abstract void copy(String sourceUrl, String targetUrl);
}
```

## 3. 各存储实现类清单（28 个新增文件）

| # | 文件路径 | 依赖 SDK |
|---|---|---|
| 1 | `upload/product/AliyunOSSUploader.java` | aliyun-sdk-oss |
| 2 | `upload/product/FastDFSUploader.java` | fastdfs-client |
| 3 | `upload/product/MinioUploader.java` | minio |
| 4 | `upload/product/QiniuyunKodoUploader.java` | qiniu-java-sdk |
| 5 | `download/product/AliyunOSSDownloader.java` | ↑ |
| 6 | `download/product/FastDFSDownloader.java` | ↑ |
| 7 | `download/product/MinioDownloader.java` | ↑ |
| 8 | `download/product/QiniuyunKodoDownloader.java` | ↑ |
| 9 | `delete/product/AliyunOSSDeleter.java` | ↑ |
| 10 | `delete/product/FastDFSDeleter.java` | ↑ |
| 11 | `delete/product/MinioDeleter.java` | ↑ |
| 12 | `delete/product/QiniuyunKodoDeleter.java` | ↑ |
| 13 | `read/product/AliyunOSSReader.java` | ↑ |
| 14 | `read/product/FastDFSReader.java` | ↑ |
| 15 | `read/product/MinioReader.java` | ↑ |
| 16 | `read/product/QiniuyunKodoReader.java` | ↑ |
| 17 | `write/product/AliyunOSSWriter.java` | ↑ |
| 18 | `write/product/FastDFSWriter.java` | ↑ |
| 19 | `write/product/MinioWriter.java` | ↑ |
| 20 | `write/product/QiniuyunKodoWriter.java` | ↑ |
| 21 | `copy/product/AliyunOSSCopier.java` | ↑ |
| 22 | `copy/product/FastDFSCopier.java` | ↑ |
| 23 | `copy/product/MinioCopier.java` | ↑ |
| 24 | `copy/product/QiniuyunKodoCopier.java` | ↑ |
| 25 | `rename/product/AliyunOSSRenamer.java` (rename=copy+delete) | ↑ |
| 26 | `rename/product/FastDFSRenamer.java` | ↑ |
| 27 | `rename/product/MinioRenamer.java` | ↑ |
| 28 | `rename/product/QiniuyunKodoRenamer.java` | ↑ |

## 4. 实现模式（以 AliyunOSSUploader 为例）

**文件**：`com.qiwenshare.ufop.operation.upload.product.AliyunOSSUploader`

```java
package com.qiwenshare.ufop.operation.upload.product;

import com.alibaba.fastjson2.JSON;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.qiwenshare.ufop.constant.StorageType;
import com.qiwenshare.ufop.operation.Uploader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class AliyunOSSUploader extends Uploader {

    @Override
    public void upload(InputStream inputStream, String fileUrl) {
        // TODO: 从配置读取 endpoint/accessKey/secretKey/bucketName
        // OSSClient oss = new OSSClient(endpoint, accessKey, secretKey);
        // oss.putObject(bucketName, fileUrl, inputStream);
        // oss.shutdown();
        log.info("AliyunOSS upload: {}", fileUrl);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.ALIYUN_OSS;
    }
}
```

**关键**：每个实现类必须：
1. 继承对应的抽象操作类（Uploader/Downloader 等）
2. `@Component` 注解让 Spring 管理
3. 重写 `getStorageType()` 返回对应的 StorageType 枚举值
4. UFOPFactory 自动发现所有 Uploader Bean，根据 StorageType 路由

## 5. UFOPFactory 扩展

**文件**：`com.qiwenshare.ufop.UFOPFactory`（修改）

```java
@Component
public class UFOPFactory {
    // 注入所有操作实现
    private final List<Uploader> uploaders;
    private final List<Downloader> downloaders;
    private final List<Deleter> deleters;
    private final List<Reader> readers;
    private final List<Writer> writers;
    private final List<Copier> copiers;
    private final List<Renamer> renamers;

    public UFOPFactory(List<Uploader> uploaders, List<Downloader> downloaders, ...) { ... }

    public Uploader getUploader() { return find(uploaders); }
    public Downloader getDownloader() { return find(downloaders); }
    // ...
    private <T extends StorageOperator> T find(List<T> list) {
        return list.stream()
            .filter(op -> op.getStorageType() == currentStorageType)
            .findFirst().orElseThrow(...);
    }
}
```

## 6. CI 测试策略（UfopRemoteTest）

**所有远程存储测试在 CI 中默认跳过**（`@Disabled`），因为 CI 环境没有阿里云/MinIO/FastDFS/七牛服务。

**文件**：`com.qiwenshare.file.UfopRemoteTest`

```java
@SpringBootTest @ActiveProfiles("test")
class UfopRemoteTest {

    @Autowired private UFOPFactory factory;

    @Test @DisplayName("本地默认存储可用")
    void testLocalStorageAvailable() {
        assertNotNull(factory.getUploader());
        assertEquals(StorageType.LOCAL, factory.getUploader().getStorageType());
    }

    @Test @DisplayName("所有实现类被 Spring 扫描并注入")
    void testAllImplementationsScanned() {
        // 验证 5 种存储各 7 个操作共 35 个 Bean（含 LocalStorage）
        // 通过 UFOPFactory 列表验证数量
    }

    @Test @DisplayName("AliyunOSS 操作类可注入")
    void testAliyunOSSUploaderExists() {
        // 切换 storageType=ALIYUN_OSS 后 getUploader() 返回 AliyunOSSUploader
    }

    @Test @DisplayName("Minio 操作类可注入")
    void testMinioUploaderExists() { ... }

    @Test @DisplayName("Qiniuyun 操作类可注入")
    void testQiniuyunUploaderExists() { ... }

    @Test @DisplayName("FastDFS 操作类可注入")
    void testFastDFSUploaderExists() { ... }
}
```

测试策略：
- ✅ 验证所有 28 个实现类被 @Component 扫描并注入 Spring 容器
- ✅ 验证 UFOPFactory 能根据 StorageType 路由到正确实现
- ❌ 不测试实际远程上传/下载（无服务端）

## 7. 文件清单

| 类型 | 数量 | 说明 |
|---|---|---|
| 新增操作实现类 | 28 | AliyunOSS/Minio/FastDFS/Qiniuyun × 7 |
| 修改 UFOPFactory | 1 | 自动注入 + 路由逻辑 |
| 新增 StorageOperator 接口 | 1 | 统一 getStorageType() 方法 |
| 新增测试 | 1 | UfopRemoteTest（6 用例） |
