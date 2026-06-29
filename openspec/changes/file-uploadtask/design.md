# Design: file-uploadtask — 技术方案

## 1. 数据模型

### UploadTask Entity

**文件**：`com.qiwenshare.file.domain.task.UploadTask`

```java
@Data
@Entity
@Table(name = "upload_task")
@TableName("upload_task")
public class UploadTask {

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** MD5 唯一标识（前端生成） */
    private String identifier;

    /** 用户 ID */
    private String userId;

    /** 原始文件名 */
    private String fileName;

    /** 目标路径 */
    private String filePath;

    /** 文件总大小 */
    private Long totalSize;

    /** 已上传分片数 */
    private Integer chunkNum;

    /** 总分片数 */
    private Integer totalChunks;

    /** 上传状态：0=进行中, 1=已完成, 2=失败 */
    private Integer uploadStatus;

    /** 创建时间 */
    private LocalDateTime createTime;
}
```

## 2. Mapper

**文件**：`com.qiwenshare.file.mapper.UploadTaskMapper`

```java
@Mapper
public interface UploadTaskMapper extends BaseMapper<UploadTask> {}
```

## 3. Service 层

### IFiletransferService

**文件**：`com.qiwenshare.file.api.IFiletransferService`

```java
public interface IFiletransferService {

    /** 上传分片 */
    void uploadChunk(String identifier, int chunkNum, int totalChunks,
                     String fileName, String filePath, long totalSize,
                     String userId, InputStream chunkStream);

    /** 合并分片 → 调用 FileService.upload */
    FileBean mergeChunks(String identifier, String filePath, String userId);

    /** 查询上传进度 */
    UploadTask getProgress(String identifier);

    /** 清理临时分片文件 */
    void cleanupChunks(String identifier);
}
```

### FiletransferService 实现

**文件**：`com.qiwenshare.file.service.FiletransferService`

```java
@Service @RequiredArgsConstructor @Slf4j
public class FiletransferService implements IFiletransferService {

    private final UploadTaskMapper taskMapper;
    private final IFileService fileService;
    private final UFOPConfigProperties ufopConfig;

    private Path getChunkDir(String identifier) {
        return Paths.get(ufopConfig.getRootPath(), "chunks", identifier);
    }

    @Override
    public void uploadChunk(String identifier, int chunkNum, int totalChunks,
            String fileName, String filePath, long totalSize,
            String userId, InputStream chunkStream) {
        // 1. 写入分片文件
        Path chunkDir = getChunkDir(identifier);
        Files.createDirectories(chunkDir);
        Files.copy(chunkStream, chunkDir.resolve(String.valueOf(chunkNum)),
                   StandardCopyOption.REPLACE_EXISTING);

        // 2. 更新或创建 UploadTask
        UploadTask task = getTaskByIdentifier(identifier);
        if (task == null) {
            task = new UploadTask();
            task.setIdentifier(identifier);
            task.setFileName(fileName);
            task.setFilePath(filePath);
            task.setTotalSize(totalSize);
            task.setTotalChunks(totalChunks);
            task.setUploadStatus(0);
            taskMapper.insert(task);
        }
        task.setChunkNum(chunkNum + 1);  // 已上传数
        task.setUserId(userId);
        task.setCreateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional
    public FileBean mergeChunks(String identifier, String filePath, String userId) {
        UploadTask task = getTaskByIdentifier(identifier);
        if (task == null) throw new QiwenException(404, "上传任务不存在");

        // 1. 合并所有分片到目标路径
        Path target = Paths.get(ufopConfig.getRootPath(), filePath);
        Files.createDirectories(target.getParent());
        try (OutputStream out = new FileOutputStream(target.toFile())) {
            for (int i = 0; i < task.getTotalChunks(); i++) {
                Path chunk = getChunkDir(identifier).resolve(String.valueOf(i));
                Files.copy(chunk, out);
            }
        }

        // 2. 注册文件
        FileBean file = fileService.upload(task.getFileName(), filePath,
                Files.size(target), getContentType(task.getFileName()), userId);

        // 3. 标记完成 + 清理
        task.setUploadStatus(1);
        taskMapper.updateById(task);
        cleanupChunks(identifier);
        return file;
    }

    @Override
    public UploadTask getProgress(String identifier) {
        return getTaskByIdentifier(identifier);
    }

    @Override
    public void cleanupChunks(String identifier) {
        try { FileUtil.del(getChunkDir(identifier).toFile()); }
        catch (Exception e) { log.warn("清理分片失败: {}", e.getMessage()); }
    }

    private UploadTask getTaskByIdentifier(String identifier) {
        LambdaQueryWrapper<UploadTask> w = new LambdaQueryWrapper<>();
        w.eq(UploadTask::getIdentifier, identifier);
        return taskMapper.selectOne(w);
    }
}
```

## 4. Controller

**文件**：`com.qiwenshare.file.controller.FiletransferController`

```java
@RestController @RequestMapping("/filetransfer") @RequiredArgsConstructor
public class FiletransferController {

    private final IFiletransferService transferService;

    @PostMapping("/upload-chunk")
    public RestResult<Void> uploadChunk(
            @RequestParam MultipartFile chunk,
            @RequestParam int chunkNum,
            @RequestParam int totalChunks,
            @RequestParam String identifier,
            @RequestParam String fileName,
            @RequestParam String filePath,
            @RequestParam long totalSize) {
        String userId = getCurrentUserId();
        transferService.uploadChunk(identifier, chunkNum, totalChunks,
                fileName, filePath, totalSize, userId, chunk.getInputStream());
        return RestResult.success();
    }

    @PostMapping("/merge-chunks")
    public RestResult<FileVO> mergeChunks(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        FileBean file = transferService.mergeChunks(
                body.get("identifier"), body.get("filePath"), userId);
        return RestResult.success(FileVO.fromEntity(file));
    }

    @GetMapping("/progress/{identifier}")
    public RestResult<UploadTask> getProgress(@PathVariable String identifier) {
        return RestResult.success(transferService.getProgress(identifier));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}
```

## 5. 前端分片上传逻辑

### UploadDialog.vue 核心逻辑

```typescript
const CHUNK_SIZE = 2 * 1024 * 1024 // 2MB

async function uploadFile(file: File, targetPath: string) {
  const identifier = await computeMD5(file) // 简化：用 file.name + file.size + Date.now()
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

  for (let i = 0; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const chunk = file.slice(start, end)

    const formData = new FormData()
    formData.append('chunk', chunk)
    formData.append('chunkNum', String(i))
    formData.append('totalChunks', String(totalChunks))
    formData.append('identifier', identifier)
    formData.append('fileName', file.name)
    formData.append('filePath', targetPath + '/' + file.name)
    formData.append('totalSize', String(file.size))

    await uploadChunk(formData)
    progress.value = Math.round(((i + 1) / totalChunks) * 100)
  }

  // 所有分片上传完成 → 合并
  await mergeChunks({ identifier, filePath: targetPath + '/' + file.name })
  status.value = 'done'
}
```

## 6. 文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `domain/task/UploadTask.java` | 新增 | 上传任务实体 |
| `mapper/UploadTaskMapper.java` | 新增 | Mapper |
| `api/IFiletransferService.java` | 新增 | 接口 |
| `service/FiletransferService.java` | 新增 | 分片上传/合并逻辑 |
| `controller/FiletransferController.java` | 新增 | REST 端点 |
| `components/file/dialog/UploadDialog.vue` | 修改 | +进度条 +分片逻辑 |
| `api/filetransfer.js` | 新增 | 前端 API |
| `test/.../FiletransferTest.java` | 新增 | 测试 |
