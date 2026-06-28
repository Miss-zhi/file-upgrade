# Design: file-ui

## 页面布局

```
FileManager.vue
├── AsideMenu.vue（左侧 240px 目录树）
│   ├── el-tree 组件
│   ├── 点击节点 → fileListStore.fetchFiles(path)
│   └── 高亮当前路径
│
└── 右侧操作区
    ├── BreadCrumb.vue（面包屑导航）
    ├── 工具栏（上传 / 新建文件夹 / 刷新）
    ├── FileTable.vue（文件列表表格）
    └── 对话框层
        ├── UploadDialog.vue
        ├── RenameDialog.vue
        ├── MoveDialog.vue
        ├── CopyDialog.vue
        └── DeleteDialog.vue
```

## 组件交互

```
FileTable 右键菜单 → RenameDialog
                  → MoveDialog
                  → CopyDialog
                  → DeleteDialog
                  → 直接删除

FileManager 工具栏 → UploadDialog
                   → createFolder（内联 prompt）
```

## 目录树数据

AsideMenu 使用后端已有的 `listByPath` API 递归构建树结构。树节点格式：
```ts
interface TreeNode {
  name: string
  path: string
  children?: TreeNode[]
}
```

## 对话框模式

所有对话框使用 `<el-dialog>` + `v-model:visible` 模式：

```vue
<script setup lang="ts">
const visible = ref(false)
const emit = defineEmits<{ confirm: [data: any] }>()

function open() { visible.value = true }
function handleConfirm() { emit('confirm', data); visible.value = false }
defineExpose({ open })
</script>
```

## 文件清单

```
qiwen-file-web/src/
├── views/FileManager.vue          ← 新建（替代 File.vue）
├── components/
│   ├── file/
│   │   ├── AsideMenu.vue          ← 新建
│   │   ├── FileTable.vue          ← 新建（替代 FileList.vue）
│   │   └── dialog/
│   │       ├── UploadDialog.vue   ← 新建
│   │       ├── RenameDialog.vue   ← 新建
│   │       ├── MoveDialog.vue     ← 新建
│   │       ├── CopyDialog.vue     ← 新建
│   │       └── DeleteDialog.vue   ← 新建
│   └── common/
│       └── BreadCrumb.vue         ← 新建
├── stores/fileList.js             ← 修改
├── api/file.js                    ← 修改
└── router/index.js                ← 修改（/file → FileManager）
```
