# Design: file-classify

## 1. 数据模型

### FileType

**文件**：`com.qiwenshare.file.domain.file.FileType`

```java
@Data
@Entity @Table(name = "file_type") @TableName("file_type")
public class FileType {
    @Id @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer orderNum;
}
```

### FileClassification

**文件**：`com.qiwenshare.file.domain.file.FileClassification`

```java
@Data
@Entity @Table(name = "file_classification") @TableName("file_classification")
public class FileClassification {
    @Id @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer fileTypeId;
    private String fileExtendName;
}
```

## 2. Mapper

```java
@Mapper public interface FileTypeMapper extends BaseMapper<FileType> {}
@Mapper public interface FileClassificationMapper extends BaseMapper<FileClassification> {}
```

## 3. Service

**文件**：`com.qiwenshare.file.service.FileTypeService`

```java
@Service @RequiredArgsConstructor
public class FileTypeService {
    private final FileTypeMapper typeMapper;
    private final FileClassificationMapper classMapper;
    private final FileBeanMapper fileBeanMapper;

    public List<FileType> listTypes() { ... }

    @PostConstruct
    public void initDefaults() {
        // 插入 5 种默认类型 + 分类映射
    }

    public IPage<FileBean> listByType(Integer typeId, int page, int size) {
        // 根据 typeId 的扩展名列表查询文件
        List<String> exts = getExtsByTypeId(typeId);
        ...
    }
}
```

## 4. Controller

**文件**：`com.qiwenshare.file.controller.FileTypeController`

```java
@GetMapping("/filetypes") → list all types
@GetMapping("/filetypes/{id}/files?page=1&size=20") → files by type
```

## 5. 前端

### AsideMenu.vue 扩展

```html
<el-menu-item-group title="分类">
  <el-menu-item v-for="t in types" :key="t.id"
    :index="`/type/${t.id}`" @click="emit('type-change', t.id)">📁 {{ t.name }}</el-menu-item>
</el-menu-item-group>
```

### FileManager.vue

```ts
const fileTypeId = ref<number | null>(null)

// 当 fileTypeId 变化时，请求 /filetypes/{id}/files
```

## 6. 文件清单

| 文件 | 说明 |
|---|---|
| `domain/file/FileType.java` | 新增 |
| `domain/file/FileClassification.java` | 新增 |
| `mapper/FileTypeMapper.java` | 新增 |
| `mapper/FileClassificationMapper.java` | 新增 |
| `service/FileTypeService.java` | 新增 |
| `controller/FileTypeController.java` | 新增 |
| `components/file/AsideMenu.vue` | 修改 |
| `views/FileManager.vue` | 修改 |
| `test/.../FileTypeTest.java` | 新增 |
