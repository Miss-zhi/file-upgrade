# Design: UFOP 工具类补全

## 1. Config 配置类 (3 个)

### AliyunConfig

**文件**: `com.qiwenshare.ufop.config.AliyunConfig`

```java
@Data @Component @ConfigurationProperties(prefix = "ufop.aliyun")
public class AliyunConfig {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
}
```

### MinioConfig

**文件**: `com.qiwenshare.ufop.config.MinioConfig`

```java
@Data @Component @ConfigurationProperties(prefix = "ufop.minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
```

### QiniuyunConfig

**文件**: `com.qiwenshare.ufop.config.QiniuyunConfig`

```java
@Data @Component @ConfigurationProperties(prefix = "ufop.qiniu")
public class QiniuyunConfig {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String domainOfBucket;
}
```

## 2. Enum 枚举类 (2 个)

### UploadFileStatusEnum

**文件**: `com.qiwenshare.ufop.constant.UploadFileStatusEnum`

```java
public enum UploadFileStatusEnum {
    UPLOADING(0, "上传中"),
    SUCCESS(1, "成功"),
    FAIL(2, "失败");
}
```

### FilePermissionEnum

**文件**: `com.qiwenshare.ufop.constant.FilePermissionEnum`

```java
public enum FilePermissionEnum {
    DEFAULT(0, "默认"),
    PUBLIC(1, "公开"),
    PRIVATE(2, "私有");
}
```

## 3. Exception 操作异常 (7 个)

全部位于 `com.qiwenshare.ufop.exception.operation`，继承自 `UFOPException`：

| 异常类 | 说明 |
|--------|------|
| `CopyException` | 文件复制异常 |
| `DeleteException` | 文件删除异常 |
| `DownloadException` | 文件下载异常 |
| `PreviewException` | 文件预览异常 |
| `ReadException` | 文件读取异常 |
| `UploadException` | 文件上传异常 |
| `WriteException` | 文件写入异常 |

## 4. Domain 类 (2 个)

### AliyunOSS

**文件**: `com.qiwenshare.ufop.domain.AliyunOSS`

```java
@Data
public class AliyunOSS {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String objectName;
}
```

### QiniuyunKodo

**文件**: `com.qiwenshare.ufop.domain.QiniuyunKodo`

```java
@Data
public class QiniuyunKodo {
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String domainOfBucket;
}
```

## 5. Util 工具类 (6 个)

全部位于 `com.qiwenshare.ufop.util`：

| 工具类 | 关键方法 |
|--------|---------|
| `UFOPUtils` | `getUploadPath(rootPath, fileName)`, `getLocalRootPath()`, `getParentPath(path)`, `getFileName(path)` |
| `AliyunUtils` | OSSClient 构建工具，连接阿里云 OSS |
| `QiniuyunUtils` | UploadManager/Auth 构建工具，连接七牛云 |
| `ReadFileUtils` | 文件读取辅助（按行/按字节/指定编码） |
| `CharsetUtils` | 字符集检测（自动识别文件编码） |
| `RedisUtil` | Redis 操作封装（set/get/expire/hash） |

## 6. 测试

- `UFOPLocalTest` 验证工具类和枚举
