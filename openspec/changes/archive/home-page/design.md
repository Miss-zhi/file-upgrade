# Design: 首页增强

## 1. HomeController

**文件**: `com.qiwenshare.file.controller.HomeController`

```java
@RestController @RequestMapping("/home") @RequiredArgsConstructor
public class HomeController {
    private final IFileService fileService;
    private final IUserService userService;
    private final FileBeanMapper fileBeanMapper;

    @GetMapping("/stats")
    public RestResult<Map<String, Object>> stats() {
        // 返回:
        // - user: 当前用户信息 (UserVO)
        // - recentFiles: 最近 8 个文件 (fileVO[], 按 updateTime 倒序)
        // - storage: { fileCount, totalSize }
    }
}
```

## 2. 核心逻辑

1. 从 `SecurityContext` 获取当前用户 ID
2. `UserService.getUserById()` 获取用户信息
3. `FileBeanMapper.selectList()` 查询当前用户最近文件（limit 8，按 updateTime DESC）
4. 统计存储：`fileBeanMapper.selectCount()` + 总大小求和

## 3. 前端组件

| 文件 | 说明 |
|------|------|
| `src/views/Home.vue` | 首页：欢迎卡片（用户名+头像）、最近文件网格（8 卡片）、快捷操作按钮（上传/新建文件夹）、存储统计条（文件数/总大小） |
| `src/api/home.js` | `getHomeStats()` |

## 4. 数据流

```
Home.vue → api/home.js → GET /home/stats → HomeController → FileBeanMapper + UserMapper
```

## 5. 测试

- `HomeControllerTest` 验证 stats 端点返回 user/recentFiles/storage 结构
