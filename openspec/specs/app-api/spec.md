# app-api — API 模块与类型定义

## Description

补充基础骨架所需的 API 模块（admin / notice / home / file-storage）和公共类型/常量定义。完整文件 API 由后续 file-module-frontend change 实现。

## Requirements

### REQ-1: RestResult 统一响应类型

将 `RestResult<T>` 从 `api/client.ts` 提取到独立类型文件。

**Scenarios:**

- `src/types/api.ts` 定义 `RestResult<T>` 接口：`code: number`、`message: string`、`data: T`、`timestamp?: number`
- `api/client.ts` 改为从 `types/api.ts` 导入 `RestResult`
- 所有 API 模块从 `types/api.ts` 导入 `RestResult`

### REQ-2: 文件类型常量与枚举

`src/types/file.ts` 定义文件管理相关的类型、枚举和常量映射。

**Scenarios:**

- **FileType enum**：`ALL = 0`、`IMAGE = 1`、`DOCUMENT = 2`、`VIDEO = 3`、`MUSIC = 4`、`OTHER = 5`、`RECYCLE = 6`、`SHARE = 8`（注意 7 未使用）
- **FileViewMode enum**：`LIST = 0`、`GRID = 1`、`TIMELINE = 2`
- **fileImgMap**：`Record<string, string>`，50+ 文件扩展名到图标路径的映射。包括：avi, bat, c, cpp, cs, css, go, py, styl, less, conf, m, scss, sass, csv, dmg, dir, doc, docx, exe, html, jar, java, js, json, jsp, kt, mp3, flac, oa, open, pdf, php, ppt, pptx, properties, r, rar, rs, rtf, sh, sql, svg, swift, ts, txt, vue, xls, xlsx, xml, zip, 7z, tar, md, markdown, yaml, yml
- **officeFileType**：`string[]` = `['ppt', 'pptx', 'doc', 'docx', 'xls', 'xlsx']`
- **markdownFileType**：`string[]` = `['markdown', 'md']`
- **allColumnList**：`string[]` = `['extendName', 'fileSize', 'uploadTime', 'deleteTime']`
- **fileSuffixCodeModeMap**：`Record<string, { language: string; mime: string }>`，约 25 种代码文件扩展名到 CodeMirror language/mime 的映射。此常量预定义供后续 `frontend-preview` change 的 codePreview 功能使用，本 change 内无消费者

### REQ-3: admin API 模块

管理员相关 API 调用。

**Scenarios:**

- `getUserList(params)` → GET `/api/v1/admin/user/list`
- `updateUserAvailable(params)` → POST `/api/v1/admin/user/updateAvailable`
- `updateUserStorage(params)` → POST `/api/v1/admin/storage/updateTotalStorage`
- `resetPassword(params)` → POST `/api/v1/admin/user/resetPassword`
- 所有函数返回 `Promise<T>`（解包 RestResult）

### REQ-4: notice API 模块

公告相关 API 调用。

**Scenarios:**

- `getNoticeList(params)` → GET `/api/v1/notice/list`
- `getNoticeDetail(params)` → GET `/api/v1/notice/detail`
- 所有函数返回 `Promise<T>`

### REQ-5: home API 模块

首页辅助 API 调用。

**Scenarios:**

- `getSystemParams(params)` → GET `/api/v1/param/grouplist`，用于获取版权信息等系统参数
- 返回 `Promise<T>`

### REQ-6: file API 模块（部分）

仅存储容量端点，完整文件 API 由 file-module-frontend 实现。

**Scenarios:**

- `getStorage()` → GET `/api/v1/filetransfer/getstorage`
- 返回 `Promise<{ storageSize: number; totalStorageSize: number }>`

## API Type Definitions

```typescript
// api/admin.ts
interface UserListParams { currentPage: number; pageCount: number }
interface UpdateAvailableParams { userId: string; available: boolean }
interface UpdateStorageParams { userId: string; totalStorage: number }
interface ResetPasswordParams { userId: string }

// api/notice.ts
interface NoticeListParams { currentPage: number; pageCount: number }
interface NoticeDetailParams { noticeId: string }
interface NoticeItem { noticeId: string; title: string; publishTime: string; content: string }

// api/file.ts
interface StorageInfo { storageSize: number; totalStorageSize: number }

// api/home.ts
interface SystemParam { groupName: string; params: Record<string, string> }
```

## Dependencies

- `api/client.ts`：已有的 axios 实例
- `types/api.ts`：RestResult<T> 类型
