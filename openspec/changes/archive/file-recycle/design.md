# Design: 文件回收站

## 1. FileBean Entity 修改

**文件**: `com.qiwenshare.file.domain.file.FileBean`

新增字段：
```java
/** 逻辑删除标记 (0=正常, 1=已删除) */
private Integer deleted = 0;
```
> 不使用 `@TableLogic`，手动管理 `deleted` 字段避免 H2 测试环境 null 问题。

## 2. IFileService 接口扩展

**文件**: `com.qiwenshare.file.api.IFileService`

```java
public interface IFileService {
    // 原有方法保持不变...

    /** 回收站列表 */
    List<FileBean> listDeleted(String userId);

    /** 恢复文件（deleted 1→0） */
    void restore(String fileId, String userId);

    /** 彻底删除（删 DB + 物理文件 + ES 索引） */
    void permanentDelete(String fileId, String userId);
}
```

## 3. FileService 实现

**文件**: `com.qiwenshare.file.service.FileService`

```java
@Slf4j @Service @RequiredArgsConstructor
public class FileService extends ServiceImpl<FileBeanMapper, FileBean> implements IFileService {

    @Override
    public List<FileBean> listDeleted(String userId) {
        // LambdaQueryWrapper: eq(userId) + eq(deleted, 1) + orderByDesc(updateTime)
    }

    @Override @Transactional
    public void restore(String fileId, String userId) {
        // 校验 file.deleted == 1 → setDeleted(0) → updateById
    }

    @Override @Transactional
    public void permanentDelete(String fileId, String userId) {
        // ufopFactory.getDeleter().delete(filePath) → searchService.deleteIndex(fileId) → mapper.deleteById(fileId)
    }
}
```

## 4. FileController 扩展

**文件**: `com.qiwenshare.file.controller.FileController`

```java
// 回收站列表
@PostMapping("/recycle")
public RestResult<List<FileVO>> recycleList() { ... }

// 恢复文件
@PostMapping("/restore")
public RestResult<Void> restore(@RequestBody DeleteFileDTO dto) { ... }

// 彻底删除
@DeleteMapping("/permanent/{id}")
public RestResult<Void> permanentDelete(@PathVariable String id) { ... }
```

## 5. 前端组件

| 文件 | 说明 |
|------|------|
| `src/views/RecycleBin.vue` | 回收站页面：列表（文件名/大小/删除时间）、恢复按钮、彻底删除按钮、清空按钮 |
| `src/router/index.js` | 添加 `/recycle` 路由 |

## 6. 数据流

```
RecycleBin.vue → POST /file/recycle → FileService.listDeleted → FileBeanMapper
RecycleBin.vue → POST /file/restore → FileService.restore → updateById (deleted=0)
RecycleBin.vue → DELETE /file/permanent/{id} → FileService.permanentDelete → 删物理+ES+DB
```

## 7. 测试

- `FileRecycleTest` 验证软删除→回收站列表→恢复→彻底删除完整流程
