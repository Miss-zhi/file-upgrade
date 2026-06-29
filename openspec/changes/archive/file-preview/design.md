# Design: 文件在线预览

## 1. 预览控制器

**文件**: `com.qiwenshare.file.controller.FilePreviewController`

```java
@RestController @RequestMapping("/file") @RequiredArgsConstructor
public class FilePreviewController {
    private final IFileService fileService;
    private final UFOPFactory ufopFactory;

    // 返回二进制流（图片/PDF/视频）
    @GetMapping("/preview/{id}")
    public ResponseEntity<InputStreamResource> preview(@PathVariable String id) { ... }

    // 返回文本内容（txt/md/json/xml/js/css/html）
    @GetMapping("/preview/text/{id}")
    public RestResult<String> previewText(@PathVariable String id) { ... }
}
```

## 2. 核心逻辑

1. 根据 `fileId` 查询 `FileBean` 元数据
2. 二进制预览：通过 `UFOPFactory.getDownloader().download(filePath)` 获取文件流，设置正确的 `Content-Type`
3. 文本预览：通过 `UFOPFactory.getReader().read(filePath)` 读取文件内容为字符串

## 3. 前端组件

| 文件 | 说明 |
|------|------|
| `src/components/file/dialog/PreviewDialog.vue` | 预览对话框，根据文件类型渲染：`<img>` / `<video>` / `<iframe>` / `<pre>` |
| `src/components/file/FileTable.vue` | 增加"预览"按钮，对 `jpg|png|gif|webp|svg|mp4|webm|txt|md|json|xml|js|css|html|pdf` 类型显示 |

## 4. 数据流

```
FileTable → 点击"预览" → PreviewDialog.vue
  → GET /file/preview/{id} (图片/视频/PDF) → FilePreviewController → ufopFactory.getDownloader() → 文件流
  → GET /file/preview/text/{id} (文本) → FilePreviewController → ufopFactory.getReader() → 文本
```

## 5. 测试

- `FilePreviewTest` 验证二进制流和文本预览端点
