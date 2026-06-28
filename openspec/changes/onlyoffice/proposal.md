# OnlyOffice 集成：文档在线编辑

## What Changes

### 后端
1. **OnlyOfficeProperties**（config/onlyoffice/）：服务器地址、API URL、回调密钥
2. **OnlyOfficeService**：生成 editorConfig（token/fileType/url/mode）、处理回调（保存/关闭）
3. **OnlyOfficeController**：GET /onlyoffice/edit/{fileId}（返回 editorConfig）、POST /onlyoffice/callback（保存回调）
4. **SecurityConfig**：白名单 /onlyoffice/callback
5. **FileService**：新增 readContent/saveContent 方法

### 前端
1. **OnlyOfficeEditor.vue**：iframe 嵌入 OnlyOffice，监听消息
2. **FileTable.vue**：对可编辑文件类型增加"编辑"按钮
3. **FileManager.vue**：集成编辑器打开逻辑
4. **router**：添加 /onlyoffice/:fileId 路由
5. **api/onlyoffice.js**：获取 editorConfig
