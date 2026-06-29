# Design: file-batch

## 后端

### IFileService 新增方法

**文件**：`com.qiwenshare.file.api.IFileService`

```java
void batchDelete(List<String> fileIds, String userId);
void batchMove(List<String> fileIds, String targetPath, String userId);
```

### FileService 实现

**文件**：`com.qiwenshare.file.service.FileService`

```java
@Override
@Transactional
public void batchDelete(List<String> fileIds, String userId) {
    for (String id : fileIds) {
        try { delete(id, userId); } catch (Exception e) { log.warn("批量删除失败: id={}", id); }
    }
}

@Override
@Transactional
public void batchMove(List<String> fileIds, String targetPath, String userId) {
    for (String id : fileIds) {
        FileBean file = fileBeanMapper.selectById(id);
        if (file == null || !file.getUserId().equals(userId)) continue;
        String newPath = targetPath.endsWith("/") ? targetPath + file.getFileName() : targetPath + "/" + file.getFileName();
        file.setFilePath(newPath);
        file.setParentPath(targetPath.endsWith("/") ? targetPath : targetPath + "/");
        file.setUpdateTime(LocalDateTime.now());
        fileBeanMapper.updateById(file);
    }
}
```

### FileController 新增端点

**文件**：`com.qiwenshare.file.controller.FileController`

```java
@PostMapping("/batch-delete")
public RestResult<Void> batchDelete(@RequestBody Map<String, List<String>> body) {
    fileService.batchDelete(body.get("ids"), getCurrentUserId());
    return RestResult.success();
}

@PostMapping("/batch-move")
public RestResult<Void> batchMove(@RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<String> ids = (List<String>) body.get("ids");
    String targetPath = (String) body.get("targetPath");
    fileService.batchMove(ids, targetPath, getCurrentUserId());
    return RestResult.success();
}
```

## 前端

### FileTable.vue

```html
<el-table :data="files" @selection-change="handleSelectionChange">
  <el-table-column type="selection" width="50" />
  ...
</el-table>
```

```ts
const emit = defineEmits<{
  ...
  'selection-change': [files: FileItem[]]
}>()

function handleSelectionChange(rows: FileItem[]) {
  emit('selection-change', rows)
}
```

### FileManager.vue

```html
<BatchToolbar
  v-if="selectedFiles.length"
  :count="selectedFiles.length"
  @clear="selectedFiles = []"
  @delete="handleBatchDelete"
  @move="handleBatchMove"
/>
<FileTable @selection-change="selectedFiles = $event" />
```

### BatchToolbar.vue

```html
<div class="batch-toolbar">
  已选 {{ count }} 项
  <el-button @click="$emit('delete')">批量删除</el-button>
  <el-button @click="$emit('move')">批量移动</el-button>
  <el-button @click="$emit('clear')">取消选择</el-button>
</div>
```
