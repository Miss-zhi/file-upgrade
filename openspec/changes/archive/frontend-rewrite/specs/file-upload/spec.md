## MODIFIED Requirements

### Requirement: 上传面板组件（前端）
系统 SHALL 提供 UploadPanel 组件，固定定位在右下角，显示上传任务列表和进度。

#### Scenario: 显示上传面板
- **WHEN** uploadFile store 中有上传任务
- **THEN** 显示上传面板，固定定位 right 16px, bottom 16px, z-index 20

#### Scenario: 面板样式
- **WHEN** 渲染上传面板
- **THEN** 宽度 560px，标题栏 40px，文件列表 240px 可滚动，白色背景，border-radius `7px 7px 0 0`，border `#e2e2e2`

#### Scenario: 折叠/展开/关闭
- **WHEN** 用户点击折叠按钮
- **THEN** 面板折叠为标题栏
- **WHEN** 用户点击展开按钮
- **THEN** 面板展开显示文件列表
- **WHEN** 用户点击关闭按钮
- **THEN** 面板隐藏

#### Scenario: 显示上传进度
- **WHEN** 有上传任务在进行
- **THEN** 每个任务显示文件名、进度条、状态（计算MD5/上传中/成功/失败）

#### Scenario: 移动端全宽
- **WHEN** screenWidth ≤ 520px
- **THEN** 上传面板 MUST 变为全宽

### Requirement: 拖拽上传遮罩（前端）
系统 SHALL 提供全屏拖拽上传遮罩，支持拖拽和截图粘贴。

#### Scenario: 显示拖拽遮罩
- **WHEN** 用户拖拽文件进入窗口
- **THEN** 显示全屏 fixed 遮罩，z-index 19，border 5px dashed #8091a5，半透明白色背景

#### Scenario: 释放文件上传
- **WHEN** 用户在遮罩上释放文件
- **THEN** 关闭遮罩，将文件加入上传队列

#### Scenario: 截图粘贴上传
- **WHEN** 用户在FileView页面按 Ctrl+V 粘贴截图
- **THEN** 从 clipboard 获取图片，加入上传队列

### Requirement: 前端 MD5 计算与秒传检测
系统 SHALL 使用 SparkMD5 在前端计算文件 MD5，用于秒传和分片校验。

#### Scenario: 计算小文件 MD5
- **WHEN** 文件 ≤10MB
- **THEN** 直接读取完整文件内容计算 MD5

#### Scenario: 计算大文件 MD5
- **WHEN** 文件 >10MB
- **THEN** 分块读取（1MB 块）计算 MD5，显示"计算MD5"进度

#### Scenario: 类型安全
- **WHEN** 导入和使用 spark-md5
- **THEN** MUST 使用 TypeScript 类型声明，禁止使用 `any` 类型

### Requirement: 上传配额预校验
系统 SHALL 在上传前校验用户存储配额，配额不足时拒绝上传。

#### Scenario: 上传前配额检查
- **WHEN** 用户发起文件上传
- **THEN** 前端 MUST 先调用配额查询接口，检查 已用空间 + 文件大小 ≤ 总配额
- **THEN** 配额不足时 MUST 在上传前给用户明确提示，不发起实际上传请求

#### Scenario: 配额数据源
- **WHEN** 查询用户配额
- **THEN** MUST 使用 `GET /api/v1/quota/info` 或 `GET /api/v1/filetransfer/getstorage` 获取配额数据
