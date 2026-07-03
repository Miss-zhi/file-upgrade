## ADDED Requirements

### Requirement: 文件右键菜单
系统 SHALL 在文件行上右键时显示文件操作菜单。

#### Scenario: 显示文件右键菜单
- **WHEN** 用户在文件行上右键（screenWidth > 768）
- **THEN** 显示 ContextMenu，包含查看/删除/复制到/移动/重命名/分享/下载/文件详情等选项

#### Scenario: 回收站文件右键
- **WHEN** fileType=6 且用户右键点击文件
- **THEN** 显示还原/永久删除/文件详情选项

#### Scenario: 分享文件右键
- **WHEN** fileType=8 且用户右键点击文件
- **THEN** 显示查看/复制链接/文件详情选项

### Requirement: 空白区域右键菜单
系统 SHALL 在空白区域右键时显示创建和上传菜单（仅 fileType=0）。

#### Scenario: 显示空白区域右键菜单
- **WHEN** 用户在空白区域右键且 fileType=0
- **THEN** 显示刷新/新建文件夹/新建 Word/Excel/PPT/上传文件/上传文件夹/拖拽上传选项

#### Scenario: 非全部文件视图空白右键
- **WHEN** fileType 不是 0
- **THEN** 不显示右键菜单或仅显示刷新选项

### Requirement: 右键菜单智能定位
系统 SHALL 根据鼠标位置和屏幕空间智能定位右键菜单。

#### Scenario: 正常定位
- **WHEN** 鼠标下方和右侧空间充足
- **THEN** 菜单在鼠标右下方显示

#### Scenario: 下方空间不足
- **WHEN** 鼠标靠近屏幕底部
- **THEN** 菜单向上展开

#### Scenario: 右侧空间不足
- **WHEN** 鼠标靠近屏幕右侧（距离 < 138px）
- **THEN** 菜单向左展开

### Requirement: 右键菜单关闭
系统 SHALL 在点击菜单外部时关闭菜单。

#### Scenario: 点击外部关闭
- **WHEN** 用户点击 document.body 或其他区域
- **THEN** ContextMenu 关闭

#### Scenario: 选择菜单项后关闭
- **WHEN** 用户点击菜单项执行操作
- **THEN** ContextMenu 关闭

### Requirement: 右键菜单样式
系统 SHALL 按照旧项目样式渲染右键菜单。

#### Scenario: 菜单样式
- **WHEN** 渲染 ContextMenu
- **THEN** 白色背景，border #e2e2e2，border-radius 4px，shadow
- **THEN** 项高 36px，padding 0 16px，font-size 14px
- **THEN** 悬停背景 #ecf5ff，文字 #409EFF
