# 文件在线预览

## What Changes

### 后端
1. **FilePreviewController**：GET /file/preview/{id} 返回文件流（用于图片/PDF）、GET /file/preview/text/{id} 返回文本内容

### 前端
1. **PreviewDialog.vue**：预览对话框（根据文件类型渲染不同预览器）
2. **FileTable.vue**：对可预览文件增加"预览"按钮

支持预览类型：jpg/png/gif/webp/svg（图片）、mp4/webm（视频）、txt/md/json/xml/js/css/html（文本）、pdf
