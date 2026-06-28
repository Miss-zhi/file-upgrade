## 1. Tasks

- [x] 1.1 新建 BreadCrumb.vue 公共面包屑组件（components/common/）
- [x] 1.2 新建 AsideMenu.vue 目录树组件（el-tree，点击切换目录）
- [x] 1.3 新建 FileTable.vue 文件列表表格（替代 FileList.vue，右键菜单）
- [x] 1.4 新建 UploadDialog.vue 上传对话框（拖拽区域 + 点击 + 路径选择 + 进度）
- [x] 1.5 新建 RenameDialog.vue 重命名对话框（el-dialog + el-input）
- [x] 1.6 新建 MoveDialog.vue 移动到对话框（选择目标目录）
- [x] 1.7 新建 CopyDialog.vue 复制到对话框（选择目标目录）
- [x] 1.8 新建 DeleteDialog.vue 删除确认对话框（文件信息 + 确认按钮）
- [x] 1.9 新建 FileManager.vue 主页面（左侧 AsideMenu + 右侧操作区，替代 File.vue）
- [x] 1.10 更新 stores/fileList.js（新增 rename/move/copy 操作）
- [x] 1.11 更新 api/file.js（新增 rename/move/copy API）
- [x] 1.12 更新 router/index.js（/file → FileManager.vue）
- [x] 1.13 删除旧 File.vue 和 FileList.vue
- [x] 1.14 验证 npx vue-tsc --noEmit + npx vite build 通过
