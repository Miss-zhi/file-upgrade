# UFOP Previewer 预览操作

## What Changes

1. **Previewer 接口**：`InputStream preview(PreviewFile file)`
2. **PreviewFile Domain**：文件URL + 缩略图参数
3. **ThumbImage Domain**：缩略图宽高裁剪参数
4. **QiwenMultipartFile**：MultipartFile 适配器（原有 LocalStorage）
5. **5 存储实现**：Local/AliyunOSS/Minio/FastDFS/Qiniuyun
6. **UFOPFactory 扩展**：注入 Previewer Bean 列表
