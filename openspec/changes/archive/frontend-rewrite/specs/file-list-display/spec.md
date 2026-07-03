## MODIFIED Requirements

### Requirement: 文件表格视图
系统 SHALL 提供 FileTable 组件，使用 el-table 展示文件列表，支持排序、选择、右键菜单。

#### Scenario: 渲染文件列表
- **WHEN** fileList store 中有文件数据
- **THEN** el-table 渲染文件列表，显示图标(30px)、文件名、路径、类型、大小、修改日期列

#### Scenario: 文件夹优先排序
- **WHEN** 文件列表包含文件夹和普通文件
- **THEN** 文件夹始终排在普通文件前面

#### Scenario: 动态列显隐
- **WHEN** fileList store 的 selectedColumnList 不包含 'extendName'
- **THEN** 表格隐藏"类型"列

#### Scenario: 行选择
- **WHEN** 用户点击文件行的 checkbox
- **THEN** 该文件加入 fileList store 的 selectedFiles

#### Scenario: 右键打开菜单
- **WHEN** 用户在文件行上右键（screenWidth > 768）
- **THEN** 打开 ContextMenu，传入当前文件信息

#### Scenario: 加载状态
- **WHEN** fileList store 的 loading 为 true
- **THEN** el-table 显示 v-loading 遮罩

#### Scenario: 表格高度自适应
- **WHEN** fileType=0（全部文件）
- **THEN** 表格高度为 `calc(100vh - 206px)`
- **WHEN** fileType=6（回收站）
- **THEN** 表格高度为 `calc(100vh - 211px)`
- **WHEN** fileType=8（分享）
- **THEN** 表格高度为 `calc(100vh - 109px)`

#### Scenario: 列prop名与后端字段一致
- **WHEN** 渲染表格列
- **THEN** 各列的prop MUST 与后端FileListVO字段名完全一致（如 `fileName`、`fileSize`、`uploadTime`、`extendName`），不得使用前端自定义字段名

#### Scenario: 排序方法正确
- **WHEN** 用户点击列头排序
- **THEN** 排序 MUST 使用正确的sortMethod或调用后端排序参数，不得出现排序失效
