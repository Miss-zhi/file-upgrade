# Design: 系统通知公告

## 1. Notice Entity

**文件**: `com.qiwenshare.file.domain.Notice`

```java
@Data @Entity @Table(name = "notice") @TableName("notice")
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;            // 标题
    private Integer platform;        // 平台（网盘/Web）
    private String markdownContent;  // Markdown 内容
    private String content;          // 纯文本内容
    private String validDateTime;    // 有效期
    private Integer isLongValidData; // 是否长期有效
    private String createTime;
    private String createUserId;
    private String modifyTime;
    private String modifyUserId;
}
```

## 2. INoticeService 接口

**文件**: `com.qiwenshare.file.api.INoticeService`

```java
public interface INoticeService {
    IPage<Notice> list(int page, int size, String title, String beginTime, String endTime);
    Notice getById(Long id);
    Notice save(Notice notice);
    Notice update(Notice notice);
    void delete(Long id);
}
```

## 3. NoticeService 实现

**文件**: `com.qiwenshare.file.service.NoticeService`

```java
@Service @RequiredArgsConstructor
public class NoticeService implements INoticeService {
    private final NoticeMapper mapper;

    @Override
    public IPage<Notice> list(int page, int size, String title, String beginTime, String endTime) {
        // LambdaQueryWrapper: 可选 title 模糊 + beginTime/endTime 范围 + orderByDesc(createTime)
    }

    @Override
    public Notice getById(Long id) { return mapper.selectById(id); }

    @Override
    public Notice save(Notice notice) {
        // 设置 createTime → mapper.insert
    }

    @Override
    public Notice update(Notice notice) {
        // 设置 modifyTime → mapper.updateById
    }

    @Override
    public void delete(Long id) { mapper.deleteById(id); }
}
```

## 4. NoticeController

**文件**: `com.qiwenshare.file.controller.NoticeController`

```java
@RestController @RequestMapping("/notice") @RequiredArgsConstructor
public class NoticeController {
    private final INoticeService noticeService;

    @GetMapping("/list")         // 分页查询（可选 title/beginTime/endTime）
    public RestResult<Map<String, Object>> list(int page, int size, String title, String beginTime, String endTime)

    @GetMapping("/{id}")         // 详情
    public RestResult<Notice> getById(@PathVariable Long id)

    @PostMapping                 // 发布
    public RestResult<Notice> create(@RequestBody Notice notice)

    @PutMapping("/{id}")         // 更新
    public RestResult<Notice> update(@PathVariable Long id, @RequestBody Notice notice)

    @DeleteMapping("/{id}")      // 删除
    public RestResult<Void> delete(@PathVariable Long id)
}
```

## 5. 前端组件

| 文件 | 说明 |
|------|------|
| `src/views/NoticeList.vue` | 公告列表（卡片展示 + 时间筛选） |
| `src/views/NoticeEdit.vue` | 管理端发布/编辑弹窗（Markdown 编辑） |
| `src/api/notice.js` | `getNoticeList()`, `getNotice()`, `createNotice()`, `updateNotice()`, `deleteNotice()` |
| `src/router/index.js` | 添加 `/notices` 路由 |

## 6. 测试

- `NoticeServiceTest` 验证 CRUD 和分页查询
