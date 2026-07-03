## ADDED Requirements

### Requirement: 工具栏上传按钮组
系统 SHALL 在 OperationMenu 中提供上传按钮组，包含上传文件、上传文件夹、拖拽上传三个选项。

#### Scenario: 显示上传按钮组
- **WHEN** fileType=0（全部文件）且非批量操作模式
- **THEN** 显示 el-dropdown 上传按钮组

#### Scenario: 点击上传文件
- **WHEN** 用户点击"上传文件"
- **THEN** 打开文件选择对话框，支持多文件选择

#### Scenario: 点击上传文件夹
- **WHEN** 用户点击"上传文件夹"
- **THEN** 打开文件夹选择对话框

#### Scenario: 点击拖拽上传
- **WHEN** 用户点击"拖拽上传"
- **THEN** 显示全屏拖拽上传遮罩

### Requirement: 工具栏新建按钮组
系统 SHALL 在 OperationMenu 中提供新建按钮组，包含新建文件夹、新建 Word/Excel/PPT。

#### Scenario: 显示新建按钮组
- **WHEN** fileType=0 且非批量操作模式
- **THEN** 显示 el-dropdown 新建按钮组

#### Scenario: 点击新建文件夹
- **WHEN** 用户点击"新建文件夹"
- **THEN** 打开 AddFolderDialog

#### Scenario: 点击新建 Word
- **WHEN** 用户点击"新建 Word"
- **THEN** 调用 createFile API 创建 .docx 文件

### Requirement: 工具栏批量操作按钮
系统 SHALL 在批量操作模式下显示批量操作按钮组。

#### Scenario: 显示批量操作按钮
- **WHEN** isBatchOperation 为 true
- **THEN** 显示批量删除、批量移动、批量下载、批量分享按钮

#### Scenario: 批量删除
- **WHEN** 用户点击批量删除
- **THEN** 对 selectedFiles 执行批量删除操作

### Requirement: 工具栏搜索框
系统 SHALL 在 OperationMenu 中提供搜索框（fileType=0 时显示）。

#### Scenario: 显示搜索框
- **WHEN** fileType=0
- **THEN** 显示 250px 宽的 el-input，带搜索图标

#### Scenario: 输入搜索关键词
- **WHEN** 用户输入关键词并按回车
- **THEN** 过滤当前文件列表（前端过滤或调用搜索 API）

### Requirement: 工具栏视图切换
系统 SHALL 提供视图模式切换图标（列表/网格/时间线）。

#### Scenario: 切换视图模式
- **WHEN** 用户点击网格图标
- **THEN** fileModel 切换为 GRID，显示 FileGrid 组件

#### Scenario: 仅大屏显示时间线
- **WHEN** screenWidth <= 768
- **THEN** 隐藏时间线视图图标

### Requirement: 工具栏设置弹窗
系统 SHALL 提供设置 el-popover，包含列显隐设置和图标大小滑块。

#### Scenario: 打开设置弹窗
- **WHEN** 用户点击设置图标
- **THEN** 显示 el-popover，包含 SelectColumn 和 gridSize 滑块

#### Scenario: 调整图标大小
- **WHEN** 用户拖动滑块调整 gridSize
- **THEN** 实时更新 fileList store 的 gridSize 并持久化

### Requirement: 回收站工具栏
系统 SHALL 在回收站视图下显示简化的工具栏。

#### Scenario: 回收站工具栏布局
- **WHEN** fileType=6（回收站）
- **THEN** 隐藏上传/新建按钮，仅显示"清空回收站"按钮，右对齐

### Requirement: 面包屑导航
系统 SHALL 提供 BreadCrumb 组件，显示当前路径并支持导航。

#### Scenario: 显示路径面包屑
- **WHEN** 当前路径为 '/文档/工作'
- **THEN** 显示 "全部 > 文档 > 工作" 面包屑

#### Scenario: 点击面包屑导航
- **WHEN** 用户点击"文档"
- **THEN** 导航到 '/文档' 路径，刷新文件列表

#### Scenario: 路径可编辑（fileType=0）
- **WHEN** fileType=0 且用户点击路径空白区域
- **THEN** 切换为 el-input，用户可编辑路径并按回车导航

#### Scenario: 静态路径显示（fileType=1~6）
- **WHEN** fileType 为 1~6
- **THEN** 显示静态文本标签，不可编辑
