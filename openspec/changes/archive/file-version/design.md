# Design: file-version — 技术方案

## 1. 数据模型

### FileVersion Entity

**文件**：`com.qiwenshare.file.domain.file.FileVersion`

```java
package com.qiwenshare.file.domain.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "file_version")
@TableName("file_version")
public class FileVersion {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 关联的文件ID */
    private String fileId;

    /** 版本序号 */
    private Integer version;

    /** 版本快照：文件名 */
    private String fileName;

    /** 版本快照：文件路径 */
    private String filePath;

    /** 版本快照：文件大小 */
    private Long fileSize;

    /** UFOP 存储路径 */
    private String storagePath;

    /** 操作用户 */
    private String userId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
```

## 2. Mapper

**文件**：`com.qiwenshare.file.mapper.FileVersionMapper`

```java
@Mapper
public interface FileVersionMapper extends BaseMapper<FileVersion> {
}
```

## 3. Service

### IFileVersionService

**文件**：`com.qiwenshare.file.api.IFileVersionService`

```java
public interface IFileVersionService {

    /** 保存版本快照 */
    FileVersion saveVersion(String fileId, String fileName, String filePath,
                            Long fileSize, String storagePath, String userId);

    /** 列出文件的所有版本 */
    List<FileVersion> listVersions(String fileId);

    /** 回滚到指定版本 */
    FileBean restoreVersion(String fileId, String versionId, String userId);

    /** 清理多余版本（保留最近 maxVersions 个） */
    void cleanupOldVersions(String fileId, int maxVersions);
}
```

### FileVersionService 实现

**文件**：`com.qiwenshare.file.service.FileVersionService`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FileVersionService implements IFileVersionService {

    private final FileVersionMapper versionMapper;
    private final FileBeanMapper fileBeanMapper;
    private final UFOPFactory ufopFactory;

    @Override
    public FileVersion saveVersion(String fileId, String fileName, String filePath,
                                    Long fileSize, String storagePath, String userId) {
        int nextVersion = getNextVersion(fileId);
        FileVersion v = new FileVersion();
        v.setId(IdUtil.getSnowflakeNextIdStr());
        v.setFileId(fileId);
        v.setVersion(nextVersion);
        v.setFileName(fileName);
        v.setFilePath(filePath);
        v.setFileSize(fileSize);
        v.setStoragePath(storagePath);
        v.setUserId(userId);
        v.setCreateTime(LocalDateTime.now());
        versionMapper.insert(v);
        cleanupOldVersions(fileId, 10);
        return v;
    }

    @Override
    public List<FileVersion> listVersions(String fileId) {
        LambdaQueryWrapper<FileVersion> w = new LambdaQueryWrapper<>();
        w.eq(FileVersion::getFileId, fileId).orderByDesc(FileVersion::getVersion);
        return versionMapper.selectList(w);
    }

    @Override
    @Transactional
    public FileBean restoreVersion(String fileId, String versionId, String userId) {
        FileVersion target = versionMapper.selectById(versionId);
        if (target == null) throw new QiwenException(404, "版本不存在");
        FileBean file = fileBeanMapper.selectById(fileId);
        if (file == null) throw new QiwenException(404, "文件不存在");
        // 保存当前状态为版本快照
        saveVersion(fileId, file.getFileName(), file.getFilePath(),
                    file.getFileSize(), file.getFilePath(), userId);
        // 恢复到目标版本
        file.setFileName(target.getFileName());
        file.setFilePath(target.getFilePath());
        file.setFileSize(target.getFileSize());
        file.setUpdateTime(LocalDateTime.now());
        fileBeanMapper.updateById(file);
        return file;
    }

    @Override
    public void cleanupOldVersions(String fileId, int maxVersions) {
        List<FileVersion> versions = listVersions(fileId);
        if (versions.size() <= maxVersions) return;
        // 删除第 maxVersions+1 之后的最旧版本
        for (int i = maxVersions; i < versions.size(); i++) {
            versionMapper.deleteById(versions.get(i).getId());
        }
    }

    private int getNextVersion(String fileId) {
        List<FileVersion> versions = listVersions(fileId);
        return versions.isEmpty() ? 1 : versions.get(0).getVersion() + 1;
    }
}
```

## 4. Controller

**文件**：`com.qiwenshare.file.controller.FileVersionController`

```java
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileVersionController {

    private final IFileVersionService versionService;
    private final IFileService fileService;

    @GetMapping("/{fileId}/versions")
    public RestResult<List<FileVersion>> listVersions(@PathVariable String fileId) {
        return RestResult.success(versionService.listVersions(fileId));
    }

    @PostMapping("/{fileId}/restore/{versionId}")
    public RestResult<FileVO> restoreVersion(@PathVariable String fileId,
                                              @PathVariable String versionId) {
        String userId = getCurrentUserId();
        FileBean restored = versionService.restoreVersion(fileId, versionId, userId);
        return RestResult.success(FileVO.fromEntity(restored));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
```

## 5. FileService 集成

**文件**：`com.qiwenshare.file.service.FileService`（修改 upload 方法）

```java
@Autowired
private IFileVersionService versionService;

@Override
public FileBean upload(String fileName, String filePath, Long fileSize,
                        String fileType, String userId) {
    // ... 现有逻辑 ...

    // 如果同路径已存在文件 → 先保存旧版本
    FileBean existing = getByPath(filePath);
    if (existing != null) {
        versionService.saveVersion(existing.getId(),
            existing.getFileName(), existing.getFilePath(),
            existing.getFileSize(), existing.getFilePath(), userId);
        // 更新现有文件
        existing.setFileName(fileName);
        existing.setFileSize(fileSize);
        existing.setFileType(fileType);
        existing.setUpdateTime(LocalDateTime.now());
        fileBeanMapper.updateById(existing);
        return existing;
    }

    // 新文件
    FileBean file = new FileBean();
    file.setId(IdUtil.getSnowflakeNextIdStr());
    // ... 设置属性 ...
    fileBeanMapper.insert(file);
    // 创建初始版本
    versionService.saveVersion(file.getId(), fileName, filePath,
                                fileSize, filePath, userId);
    return file;
}
```

## 6. 前端

### VersionHistory.vue

**文件**：`src/components/file/dialog/VersionHistory.vue`

```vue
<script setup lang="ts">
import { ref } from 'vue'
import http from '_api/http'

const visible = ref(false)
const versions = ref<any[]>([])
const fileId = ref('')

async function open(fid: string) {
  fileId.value = fid; visible.value = true
  const res: any = await http.get(`/file/${fid}/versions`)
  if (res.success) versions.value = res.data
}

async function handleRestore(ver: any) {
  const res: any = await http.post(`/file/${fileId.value}/restore/${ver.id}`)
  if (res.success) { ElMessage.success('已回滚'); visible.value = false }
}

defineExpose({ open })
</script>
```

### FileTable 菜单增加

```html
<el-dropdown-item @click="emit('versions', row)">版本历史</el-dropdown-item>
```

## 7. 文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `domain/file/FileVersion.java` | 新增 | 版本实体 |
| `mapper/FileVersionMapper.java` | 新增 | Mapper |
| `api/IFileVersionService.java` | 新增 | 接口 |
| `service/FileVersionService.java` | 新增 | 实现 |
| `controller/FileVersionController.java` | 新增 | 端点 |
| `service/FileService.java` | 修改 | upload 集成 saveVersion |
| `components/file/dialog/VersionHistory.vue` | 新增 | 版本对话框 |
| `components/file/FileTable.vue` | 修改 | +版本历史菜单项 |
| `views/FileManager.vue` | 修改 | 集成对话框 |
| `test/.../FileVersionTest.java` | 新增 | 测试 |
