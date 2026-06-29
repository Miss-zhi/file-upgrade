# Design: 管理面板

## 1. 系统配置 Entity

**文件**: `com.qiwenshare.file.domain.config.SysConfig`

```java
@Data @Entity @Table(name = "sys_config") @TableName("sys_config")
public class SysConfig {
    @Id @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String configKey;
    private String configValue;
    private String description;
}
```

**文件**: `com.qiwenshare.file.mapper.SysConfigMapper`
```java
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {}
```

## 2. 系统配置 Service

**文件**: `com.qiwenshare.file.service.SysConfigService`

```java
@Service @RequiredArgsConstructor
public class SysConfigService {
    private final SysConfigMapper configMapper;

    public Map<String, String> getAllConfig() { ... }
    public void saveConfig(Map<String, String> config) { ... }
}
```

## 3. 统计 Service

**文件**: `com.qiwenshare.file.service.StatsService`

```java
@Service @RequiredArgsConstructor
public class StatsService {
    private final FileBeanMapper fileBeanMapper;
    private final UserMapper userMapper;

    public Map<String, Object> getStats() { ... }
    // 返回: { fileCount, totalSize, userCount }
}
```

## 4. AdminController 扩展

**文件**: `com.qiwenshare.file.controller.AdminController`

```java
@RestController @RequestMapping("/admin") @PreAuthorize("hasRole('ADMIN')") @RequiredArgsConstructor
public class AdminController {
    private final StatsService statsService;
    private final SysConfigService sysConfigService;
    private final OperationLogService logService;

    // 文件+用户统计
    @GetMapping("/stats")
    public RestResult<Map<String, Object>> getStats() { ... }

    // 获取系统配置
    @GetMapping("/config")
    public RestResult<Map<String, String>> getConfig() { ... }

    // 保存系统配置
    @PutMapping("/config")
    public RestResult<Void> saveConfig(@RequestBody Map<String, String> config) { ... }

    // 操作日志分页
    @GetMapping("/logs")
    public RestResult<Map<String, Object>> getLogs(int page, int size, String operation, String startTime, String endTime) { ... }
}
```

## 5. 前端组件

| 文件 | 说明 |
|------|------|
| `src/views/Dashboard.vue` | 仪表盘页面：4 统计卡片（文件数/总大小/用户数/分享数）+ 系统配置表单 |
| `src/api/admin.js` | `getStats()`、`getConfig()`、`saveConfig()` |
| `src/router/index.js` | `/admin` 默认子路由渲染 Dashboard |

## 6. 数据流

```
Dashboard.vue → api/admin.js → GET /admin/stats  → StatsService  → FileBeanMapper.count / UserMapper.count
Dashboard.vue → api/admin.js → GET /admin/config → SysConfigService → SysConfigMapper.selectList
Dashboard.vue → api/admin.js → PUT /admin/config → SysConfigService → SysConfigMapper.insert/updateById
```

## 7. 测试

- `AdminControllerTest` 验证 `/admin/stats` 和 `/admin/config` 端点
