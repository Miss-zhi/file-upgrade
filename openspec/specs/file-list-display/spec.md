## ADDED Requirements

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

### Requirement: 文件网格视图
系统 SHALL 提供 FileGrid 组件，使用 flex wrap 布局展示文件网格。

#### Scenario: 渲染文件网格
- **WHEN** fileModel 为 GRID 模式
- **THEN** 以 ul/li 结构渲染文件网格，每项宽度为 gridSize + 40px

#### Scenario: 悬停效果
- **WHEN** 鼠标悬停在文件项上
- **THEN** 背景色变为 #F5F7FA，文件名加粗

#### Scenario: 批量选择覆盖层
- **WHEN** isBatchOperation 为 true
- **THEN** 每个文件项显示 checkbox 覆盖层

#### Scenario: 文件名截断
- **WHEN** 文件名超过 2 行
- **THEN** 使用 text-overflow: ellipsis 截断

### Requirement: 文件时间线视图
系统 SHALL 提供 FileTimeLine 组件，使用 el-timeline 按日期分组展示图片文件。

#### Scenario: 按日期分组
- **WHEN** fileType=1（图片）且 fileModel=TIMELINE
- **THEN** 文件按 uploadTime 日期分组显示

#### Scenario: 正序/倒序切换
- **WHEN** 用户切换排序方式
- **THEN** 时间线重新排序

#### Scenario: 仅图片可用
- **WHEN** fileType 不是 1（非图片）
- **THEN** 时间线视图不可用或不显示

### Requirement: 分页组件
系统 SHALL 提供分页组件，支持页码切换和每页大小调整。

#### Scenario: 切换页码
- **WHEN** 用户点击页码
- **THEN** 调用 fileList store 的 fetchFileList 加载对应页数据

#### Scenario: 调整每页大小
- **WHEN** 用户选择每页 50 条
- **THEN** 重新加载文件列表，每页显示 50 条

#### Scenario: 分页大小选项
- **WHEN** 渲染分页组件
- **THEN** 提供 [10, 50, 100, 200] 四个选项

#### Scenario: 默认分页大小
- **WHEN** fileModel 为 LIST（表格模式）
- **THEN** 默认 pageSize 为 50
- **WHEN** fileModel 为 GRID（网格模式）
- **THEN** 默认 pageSize 为 100
- **WHEN** 切换 fileModel 时
- **THEN** pageSize 自动切换为对应默认值，重新加载第一页
