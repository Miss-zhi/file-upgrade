# Design: UFOP 本地存储实现

## 1. UFOPConfigProperties

**文件**: `com.qiwenshare.ufop.config.UFOPConfigProperties`

```java
@Data @Component @ConfigurationProperties(prefix = "ufop.local")
public class UFOPConfigProperties {
    private String rootPath = "./uploads";
}
```

## 2. UFOPFactory

**文件**: `com.qiwenshare.ufop.UFOPFactory`

```java
@Component @RequiredArgsConstructor
public class UFOPFactory {
    private final UFOPConfigProperties config;
    private final LocalStorageUploader uploader;
    private final LocalStorageDownloader downloader;
    private final LocalStorageDeleter deleter;
    private final LocalStorageReader reader;
    private final LocalStorageWriter writer;
    private final LocalStorageRenamer renamer;
    private final LocalStorageCopier copier;

    public Uploader getUploader() { return uploader; }
    public Downloader getDownloader() { return downloader; }
    public Deleter getDeleter() { return deleter; }
    public Reader getReader() { return reader; }
    public Writer getWriter() { return writer; }
    public Renamer getRenamer() { return renamer; }
    public Copier getCopier() { return copier; }
}
```

## 3. 7 个本地存储操作实现

所有实现类位于 `com.qiwenshare.ufop.operation`，使用 `@Component` + `@RequiredArgsConstructor`，通过 `UFOPConfigProperties.rootPath` 获取上传根目录。

| 实现类 | 实现接口 | 关键方法 |
|--------|---------|---------|
| `LocalStorageUploader` | `Uploader` | `void upload(String path, InputStream is)` — Files.copy 写入 |
| `LocalStorageDownloader` | `Downloader` | `InputStream download(String path)` — FileInputStream 读取 |
| `LocalStorageDeleter` | `Deleter` | `void delete(String path)` — Files.deleteIfExists |
| `LocalStorageReader` | `Reader` | `String read(String path)` — Files.readString |
| `LocalStorageWriter` | `Writer` | `void write(String path, String content)` — Files.writeString |
| `LocalStorageRenamer` | `Renamer` | `void rename(String src, String dst)` — Files.move |
| `LocalStorageCopier` | `Copier` | `void copy(String src, String dst)` — Files.copy |

## 4. UFOP 7 个操作接口

所有接口位于 `com.qiwenshare.ufop.operation`：

| 接口 | 方法签名 |
|------|---------|
| `Uploader` | `void upload(String path, InputStream inputStream)` |
| `Downloader` | `InputStream download(String path)` |
| `Deleter` | `void delete(String path)` |
| `Reader` | `String read(String path)` |
| `Writer` | `void write(String path, String content)` |
| `Renamer` | `void rename(String sourcePath, String destPath)` |
| `Copier` | `void copy(String sourcePath, String destPath)` |

## 5. StorageType 枚举

**文件**: `com.qiwenshare.ufop.constant.StorageType`

```java
public enum StorageType { LOCAL, ALIYUN_OSS, FAST_DFS, MINIO, QINIU }
```

## 6. 异常类

**文件**: `com.qiwenshare.ufop.exception.UFOPException`

```java
public class UFOPException extends RuntimeException {
    public UFOPException(String message) { ... }
    public UFOPException(String message, Throwable cause) { ... }
}
```

## 7. 集成点

**FileService** (`com.qiwenshare.file.service.FileService`):
- `upload()` 内：`ufopFactory.getUploader().upload(filePath, inputStream)`
- `delete()` 内：`ufopFactory.getDeleter().delete(filePath)`
- `permanentDelete()` 内：`ufopFactory.getDeleter().delete(filePath)`

**FileController** (`com.qiwenshare.file.controller.FileController`):
- `upload()`：`ufopFactory.getUploader().upload(filePath, inputStream)`
- `download()`：`ufopFactory.getDownloader().download(filePath)`

## 8. 测试

- `UFOPLocalTest` 验证 7 个本地操作（写入→读取→重命名→复制→删除）
