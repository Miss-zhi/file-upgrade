# Design: 操作日志

## 1. OperationLog Entity

**文件**: `com.qiwenshare.file.domain.log.OperationLog`

```java
@Data @TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String username;
    private String operation;   // 操作名称
    private String method;      // 请求方法
    private String params;      // 请求参数
    private Long costTime;      // 耗时(ms)
    private String ip;          // 请求 IP
    private LocalDateTime createTime;
}
```

> 日志写入由 AOP 切面 `com.qiwenshare.file.aop.WebLogAspect` 配合 `@MyLog` 注解自动完成，无需业务代码手动写日志。

## 2. OperationLogService

**文件**: `com.qiwenshare.file.service.OperationLogService`

```java
@Service @RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper mapper;

    public IPage<OperationLog> page(Integer pageNo, Integer pageSize,
            String operation, String startTime, String endTime) {
        // LambdaQueryWrapper:
        //   可选 eq(operation) + ge(createTime, startTime) + le(createTime, endTime)
        //   orderByDesc(createTime)
    }
}
```

## 3. AdminController 日志端点

**文件**: `com.qiwenshare.file.controller.AdminController`

```java
// 在 AdminController 中新增
@GetMapping("/logs")
public RestResult<Map<String, Object>> getLogs(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "20") Integer size,
    @RequestParam(required = false) String operation,
    @RequestParam(required = false) String startTime,
    @RequestParam(required = false) String endTime) {

    IPage<OperationLog> result = logService.page(page, size, operation, startTime, endTime);
    return RestResult.success(Map.of("records", result.getRecords(), "total", result.getTotal()));
}
```

## 4. 前端组件

| 文件 | 说明 |
|------|------|
| `src/views/OperationLog.vue` | 日志表格（用户/操作/方法/耗时/IP/时间）+ 日期范围筛选 + 操作类型筛选 |
| `src/api/admin.js` | `getOperationLogs(page, size, operation, startTime, endTime)` |
| `src/router/index.js` | 添加 `/operation-log` 路由 |

## 5. 数据流

```
用户操作 → @MyLog 注解 → WebLogAspect 切面 → 自动写入 operation_log 表
OperationLog.vue → api/admin.js → GET /admin/logs → OperationLogService.page → OperationLogMapper
```

## 6. 测试

- `OperationLogServiceTest` 验证分页查询和时间筛选
