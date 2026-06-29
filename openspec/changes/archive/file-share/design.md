# Design: 文件分享

## 1. ShareFile Entity

**文件**: `com.qiwenshare.file.domain.share.ShareFile`

```java
@Data @Entity @Table(name = "share_file") @TableName("share_file")
public class ShareFile {
    @Id @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String shareBatchNum;    // 批次号
    private String userId;           // 分享者
    private String filePath;         // 文件路径
    private String shareToken;       // 分享令牌（随机 16 位）
    private String shareCode;        // 提取码（随机 4 位）
    private Integer expireDays;      // 有效期天数
    private LocalDateTime expireTime;// 过期时间
    private LocalDateTime createTime;
}
```

## 2. IShareFileService 接口

**文件**: `com.qiwenshare.file.api.IShareFileService`

```java
public interface IShareFileService {
    ShareFile createShare(String filePath, String userId, Integer expireDays, String shareCode);
    List<ShareFile> listShares(String userId);
    void cancelShare(String shareId, String userId);
    ShareFile verifyShare(String token, String code);
    ShareFile getShareByToken(String token);
}
```

## 3. ShareFileService 实现

**文件**: `com.qiwenshare.file.service.ShareFileService`

```java
@Service @RequiredArgsConstructor
public class ShareFileService extends ServiceImpl<ShareFileMapper, ShareFile> implements IShareFileService {
    private final ShareFileMapper shareFileMapper;

    @Override @Transactional
    public ShareFile createShare(String filePath, String userId, Integer expireDays, String shareCode) {
        // 生成 shareToken(16位随机字符串) + shareCode(4位随机数字) + expireTime
    }

    @Override
    public List<ShareFile> listShares(String userId) {
        // LambdaQueryWrapper: eq(userId) + orderByDesc(createTime)
    }

    @Override @Transactional
    public void cancelShare(String shareId, String userId) {
        // 校验归属权 → deleteById
    }

    @Override
    public ShareFile verifyShare(String token, String code) {
        // getShareByToken → 检查过期 → 检查提取码匹配
    }

    @Override
    public ShareFile getShareByToken(String token) {
        // LambdaQueryWrapper: eq(shareToken)
    }
}
```

## 4. ShareController

**文件**: `com.qiwenshare.file.controller.ShareController`

```java
@RestController @RequiredArgsConstructor
public class ShareController {
    private final IShareFileService shareFileService;
    private final IFileService fileService;

    @PostMapping("/share/create")          // 创建分享
    public RestResult<ShareVO> createShare(@RequestBody CreateShareDTO dto) { ... }

    @PostMapping("/share/list")            // 分享列表
    public RestResult<List<ShareVO>> listShares() { ... }

    @PostMapping("/share/cancel")          // 取消分享
    public RestResult<Void> cancelShare(@RequestBody CreateShareDTO dto) { ... }

    @GetMapping("/share/verify")           // 验证提取码
    public RestResult<FileVO> verifyShare(@RequestParam String token, @RequestParam String code) { ... }

    @GetMapping("/anonymous/download/{token}")  // 匿名下载
    public RestResult<FileVO> anonymousDownload(@PathVariable String token, @RequestParam String code) { ... }
}
```

## 5. SecurityConfig 修改

**文件**: `com.qiwenshare.file.config.security.SecurityConfig`
- 添加 `.requestMatchers("/anonymous/**").permitAll()`

## 6. DTO / VO

| 文件 | 说明 |
|------|------|
| `com.qiwenshare.file.dto.share.CreateShareDTO` | `{ fileId, filePath, expireDays, code }` |
| `com.qiwenshare.file.vo.share.ShareVO` | `{ id, shareToken, shareCode, expireTime, link, createTime }` + `fromEntity()` |

## 7. 前端组件

| 文件 | 说明 |
|------|------|
| `src/components/file/dialog/ShareDialog.vue` | 分享弹窗：设置过期时间、提取码、生成链接+复制 |
| `src/views/Share.vue` | 分享管理页面：分享列表、取消分享、复制链接 |
| `src/api/share.js` | `createShare()`, `listShares()`, `cancelShare()` |

## 8. 数据流

```
FileManager → "分享" → ShareDialog.vue
  → POST /share/create → ShareFileService.createShare → 生成 token + code

匿名用户 → 访问 /anonymous/download/{token}?code=xxxx
  → ShareController.anonymousDownload → verifyShare → 校验 token+code+过期 → 返回文件信息
```

## 9. 测试

- `ShareFileServiceTest` 验证创建/验证/取消/过期逻辑
