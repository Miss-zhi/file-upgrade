# 文件分类浏览

## What Changes

### 后端
1. **FileType Entity**：类型定义（id/name/order）— 图片/文档/视频/音乐/其他
2. **FileClassification Entity**：扩展名→类型ID 映射
3. **FileTypeController**：GET /filetypes（列表）、GET /filetypes/{id}/files（按类型查文件）
4. **FileTypeService**：初始化默认类型+分类数据

### 前端
1. **AsideMenu.vue**：侧栏增加"分类"板块，标签切换
2. **FileManager.vue**：传递 fileTypeId 筛选
3. **api/filetype.js**

### 默认分类
| ID | 名称 | 扩展名 |
|---|---|---|
| 1 | 图片 | jpg/jpeg/png/gif/bmp/webp/svg |
| 2 | 文档 | pdf/doc/docx/xls/xlsx/ppt/pptx/txt/md |
| 3 | 视频 | mp4/avi/mkv/mov/wmv/flv/webm |
| 4 | 音乐 | mp3/wav/flac/aac/ogg/wma |
| 5 | 其他 | 其余 |
