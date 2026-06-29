# Design: 全文搜索

## 1. FileSearchService

**文件**: `com.qiwenshare.file.service.FileSearchService`

```java
@Slf4j @Service @RequiredArgsConstructor
public class FileSearchService {
    private static final String INDEX_NAME = "qiwen_file";
    private final ElasticsearchClient esClient;

    /** 创建 ES 索引（在文件上传时调用） */
    public void createIndex(String fileId, String fileName, String filePath,
            String fileType, Long fileSize, String userId) {
        // IndexRequest.of → esClient.index()
    }

    /** 删除 ES 索引（在文件删除时调用） */
    public void deleteIndex(String fileId) {
        // esClient.delete(d -> d.index(INDEX_NAME).id(fileId))
    }

    /** 搜索：userId 过滤 + fileName/fileType 多字段匹配 + 高亮 */
    public List<Map<String, Object>> search(String keyword, String userId) {
        // bool.must: term(userId) + multiMatch(fileName, fileType)
        // highlight: fileName, fileType → <em>...</em>
        // 返回: [{ fileName, filePath, fileType, fileSize, _score, fileNameHighlight, ... }]
    }
}
```

## 2. SearchController

**文件**: `com.qiwenshare.file.controller.SearchController`

```java
@RestController @RequestMapping("/search") @RequiredArgsConstructor
public class SearchController {
    private final FileSearchService searchService;

    @PostMapping
    public RestResult<List<Map<String, Object>>> search(@RequestParam String keyword) {
        String userId = getCurrentUserId();
        return RestResult.success(searchService.search(keyword, userId));
    }
}
```

## 3. FileService 集成

**文件**: `com.qiwenshare.file.service.FileService`

```java
// upload() 方法末尾:
try { searchService.createIndex(file.getId(), ...); } catch (Exception e) { log.warn(...); }

// delete() 方法末尾:
try { searchService.deleteIndex(fileId); } catch (Exception e) { log.warn(...); }

// permanentDelete() 方法中:
try { searchService.deleteIndex(fileId); } catch (Exception e) { log.warn(...); }
```

## 4. ES 配置

**文件**: `com.qiwenshare.file.config.es.ElasticsearchConfig`
- 测试环境通过 `application-test.yml` 禁用 ES 健康检查

## 5. 前端组件

| 文件 | 说明 |
|------|------|
| `src/components/common/SearchBar.vue` | 全局搜索输入框（AppHeader 中） |
| `src/views/SearchResult.vue` | 搜索结果页面（表格展示 + `<em>` 关键词高亮） |
| `src/api/search.js` | `search(keyword)` |
| `src/router/index.js` | 添加 `/search?q=keyword` 路由 |
| `src/components/AppHeader.vue` | 集成 SearchBar 组件 |

## 6. 数据流

```
AppHeader.SearchBar → 搜索 → /search?q=keyword
  → SearchResult.vue → POST /search → SearchController
  → FileSearchService.search(keyword, userId) → ES bool query + highlight
  → 返回高亮结果 → 前端 v-html 渲染 <em> 标签

上传文件 → FileService.upload() → searchService.createIndex() → ES 索引
删除文件 → FileService.delete() → searchService.deleteIndex() → ES 删除
```

## 7. 测试

- `FileSearchServiceTest` 验证索引创建/删除/搜索+高亮
