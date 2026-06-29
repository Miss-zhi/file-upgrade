# Design: UFOP Previewer 预览操作

## 1. Previewer 接口

**文件**: `com.qiwenshare.ufop.operation.preview.Previewer`

```java
public interface Previewer {
    InputStream preview(PreviewFile previewFile);
}
```

## 2. PreviewFile Domain

**文件**: `com.qiwenshare.ufop.operation.preview.PreviewFile`

```java
@Data
public class PreviewFile {
    private String fileUrl;        // 文件 URL 或本地路径
    private ThumbImage thumbImage; // 缩略图参数
}
```

## 3. ThumbImage Domain

**文件**: `com.qiwenshare.ufop.operation.preview.ThumbImage`

```java
@Data
public class ThumbImage {
    private int width;      // 缩略图宽度
    private int height;     // 缩略图高度
    private String quality; // 图片质量
    private String format;  // 输出格式 (jpg/png/webp)
}
```

## 4. 5 种存储 Previewer 实现

| 实现类 | 路径 | 说明 |
|--------|------|------|
| `LocalStoragePreviewer` | `com.qiwenshare.ufop.operation.preview.product.LocalStoragePreviewer` | Files.readAllBytes + ByteArrayInputStream |
| `AliyunOSSPreviewer` | `com.qiwenshare.ufop.operation.preview.product.AliyunOSSPreviewer` | OSSClient.getObject |
| `MinioPreviewer` | `com.qiwenshare.ufop.operation.preview.product.MinioPreviewer` | MinioClient.getObject |
| `FastDFSPreviewer` | `com.qiwenshare.ufop.operation.preview.product.FastDFSPreviewer` | FastDFS storageClient |
| `QiniuyunKodoPreviewer` | `com.qiwenshare.ufop.operation.preview.product.QiniuyunKodoPreviewer` | Qiniu BucketManager |

每个实现类提供 `StorageType getStorageType()` 方法用于工厂匹配。

## 5. UFOPFactory 扩展

**文件**: `com.qiwenshare.ufop.UFOPFactory`

```java
// 新增
private final LocalStoragePreviewer previewer;

public Previewer getPreviewer() { return previewer; }
```

## 6. 使用场景

```java
// FilePreviewController 中:
PreviewFile pf = new PreviewFile();
pf.setFileUrl(config.getRootPath() + "/" + file.getFilePath());
ThumbImage thumb = new ThumbImage();
thumb.setWidth(800); thumb.setHeight(600);
pf.setThumbImage(thumb);
InputStream is = ufopFactory.getPreviewer().preview(pf);
```

## 7. 测试

- `UfopPreviewTest` 验证 preview 操作返回非空流
