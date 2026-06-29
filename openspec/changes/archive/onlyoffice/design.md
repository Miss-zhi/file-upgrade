# Design: OnlyOffice 集成

## 1. 配置类

**文件**: `com.qiwenshare.file.config.onlyoffice.OnlyOfficeProperties`

```java
@Data @Component @ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {
    private String serverUrl = "http://localhost:9980";     // Document Server 地址
    private String apiUrl = "/web-apps/apps/api/documents/api.js";
    private String secret = "qiwen-onlyoffice-secret";      // 回调密钥
    private String callbackUrl = "http://localhost:8080/onlyoffice/callback";
}
```

## 2. OnlyOfficeService

**文件**: `com.qiwenshare.file.service.OnlyOfficeService`

```java
@Service @RequiredArgsConstructor
public class OnlyOfficeService {
    private final OnlyOfficeProperties props;
    private final FileBeanMapper fileBeanMapper;

    /** 生成 OnlyOffice editorConfig */
    public Map<String, Object> getEditorConfig(String fileId, String userId, String mode) {
        // 1. 查询 FileBean → 获取文件名/扩展名
        // 2. 构建 document: { fileType, key, title, url }
        // 3. 构建 editorConfig: { callbackUrl, mode, lang, user }
        // 4. 生成 token (UUID)
    }

    /** 处理 OnlyOffice 保存回调 */
    public void handleCallback(int status, String fileId, String downloadUrl) {
        // status==2 (保存): 更新 FileBean.updateTime
    }

    private String getFileType(String fileName) {
        // 根据扩展名返回 docx/xlsx/pptx
    }
}
```

## 3. OnlyOfficeController

**文件**: `com.qiwenshare.file.controller.OnlyOfficeController`

```java
@RestController @RequiredArgsConstructor
public class OnlyOfficeController {
    private final OnlyOfficeService ooService;

    // 获取编辑器配置（前端 iframe 使用）
    @GetMapping("/onlyoffice/edit/{fileId}")
    public RestResult<Map<String, Object>> getEditorConfig(@PathVariable String fileId, @RequestParam String mode) { ... }

    // 保存回调（OnlyOffice Document Server → 后端）
    @PostMapping("/onlyoffice/callback")
    public Map<String, Integer> callback(@RequestBody Map<String, Object> body) { ... }
}
```

## 4. SecurityConfig 修改

**文件**: `com.qiwenshare.file.config.security.SecurityConfig`
- 添加 `.requestMatchers("/onlyoffice/callback").permitAll()`

## 5. 前端组件

| 文件 | 说明 |
|------|------|
| `src/views/OnlyOfficeEditor.vue` | iframe 嵌入 OnlyOffice，通过 `postMessage` 通信 |
| `src/api/onlyoffice.js` | `getEditorConfig(fileId, mode)` |
| `src/router/index.js` | 添加 `/onlyoffice/:fileId` 路由（meta: { fullscreen: true }） |
| `src/components/file/FileTable.vue` | 对 `docx/xlsx/pptx` 类型文件显示"编辑"按钮 |

## 6. 支持的编辑类型

| 扩展名 | OnlyOffice fileType |
|--------|-------------------|
| .docx | docx |
| .xlsx | xlsx |
| .pptx | pptx |

## 7. 数据流

```
FileTable → 点击"编辑" → router.navigate("/onlyoffice/{fileId}")
  → OnlyOfficeEditor.vue → GET /onlyoffice/edit/{fileId} → OnlyOfficeService.getEditorConfig()
  → iframe 嵌入 Document Server API
  → 保存回调 → POST /onlyoffice/callback → OnlyOfficeService.handleCallback() → 更新 updateTime
```

## 8. 测试

- `OnlyOfficeServiceTest` 验证 editorConfig 生成和回调处理
