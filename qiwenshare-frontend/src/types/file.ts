/** 文件信息（对应后端 FileListVO） */
export interface FileInfo {
  userFileId: number
  fileName: string
  filePath: string
  fileType: number // 1=file, 2=folder
  fileSize: number
  extendName: string
  uploadTime: string
  modifyTime: string
  deleteStatus: number
}

/** 文件详情（对应后端 FileDetailVO） */
export interface FileDetail {
  userFileId: number
  fileName: string
  filePath: string
  fileType: number
  fileSize: number
  extendName: string
  fileHash: string
  storageType: string
  uploadTime: string
  modifyTime: string
}

/** 文件树节点（对应后端 TreeNodeVO） */
export interface TreeNode {
  userFileId: number
  fileName: string
  filePath: string
  children: TreeNode[]
}

/** 上传结果（对应后端 UploadFileVO） */
export interface UploadResult {
  userFileId: number
  fileName: string
  fileSize: number
  fileHash: string
  isSpeed: boolean
}

/** 分享信息（对应后端 ShareInfoVO） */
export interface ShareInfo {
  shareId: number
  userFileId: number
  shareCode: string
  extractCode: string | null
  expireTime: string | null
  isExpired: boolean // 是否已过期
  fileName: string
  fileSize: number
  viewCount: number
  createTime: string
}

/** 批量操作结果（对应后端 BatchOperationResultVO） */
export interface BatchOperationResult {
  successCount: number
  failedItems: Array<{ userFileId: number; reason: string }>
}

/** 分页结果（对应 Spring Data Page） */
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/** 用户配额信息（对应后端 QuotaInfoVO） */
export interface QuotaInfoVO {
  totalQuota: number
  usedSize: number
  availableQuota: number
}

/** 上传任务状态 */
export type UploadTaskStatus = 'pending' | 'hashing' | 'uploading' | 'success' | 'error'

/** 上传任务 */
export interface UploadTask {
  id: string
  fileName: string
  fileSize: number
  progress: number
  status: UploadTaskStatus
  errorMsg: string
  file?: File
}

// ---- DTO 请求类型 ----

/** 文件列表查询参数 */
export interface FileListParams {
  filePath?: string
  fileType?: number
  page?: number
  size?: number
  order?: string
  sort?: string
}

/** 按分类浏览参数 */
export interface CategoryListParams {
  category: string
  page?: number
  size?: number
}

/** 重命名请求 */
export interface RenameFileDTO {
  userFileId: number
  newName: string
}

/** 移动文件请求 */
export interface MoveFileDTO {
  userFileId: number
  targetFolderId?: number | null
}

/** 批量移动文件请求 */
export interface BatchMoveFileDTO {
  userFileIds: number[]
  targetFolderId?: number | null
}

/** 复制文件请求 */
export interface CopyFileDTO {
  userFileId: number
  targetFolderId?: number | null
}

/** 批量复制文件请求 */
export interface BatchCopyFileDTO {
  userFileIds: number[]
  targetFolderId?: number | null
}

/** 创建文件夹请求 */
export interface CreateFoldDTO {
  folderName: string
  filePath: string
}

/** 创建文件请求 */
export interface CreateFileDTO {
  fileName: string
  filePath: string
}

/** 删除文件请求 */
export interface DeleteFileDTO {
  userFileId: number
}

/** 批量删除文件请求 */
export interface BatchDeleteFileDTO {
  userFileIds: number[]
}

/** 恢复文件请求 */
export interface RestoreFileDTO {
  userFileIds: number[]
}

/** 创建分享请求 */
export interface ShareCreateDTO {
  userFileId: number
  expireType?: number // 1=1天, 7=7天, 30=30天, 0=永久（expireTime 为空时生效）
  expireTime?: string // 自定义过期时间 ISO-8601 格式，优先于 expireType
  shareType?: number // 1=需要提取码, 0=不需要，默认 1
  extractCode?: string // 自定义提取码（4-6位字母数字），为空时服务端随机生成
}

/** 验证提取码请求 */
export interface ShareVerifyDTO {
  shareCode: string
  extractCode: string
}

/** 保存分享文件请求 */
export interface SaveShareFileDTO {
  shareCode: string
  targetNodeId?: number | null
}

/** 秒传请求 */
export interface SpeedUploadDTO {
  fileName: string
  filePath: string
  fileSize: number
  fileHash: string
}

/** 分片上传初始化请求 */
export interface ChunkUploadInitDTO {
  fileName: string
  filePath: string
  fileSize: number
  fileHash: string
  totalChunks: number
}

// ---- 预览相关类型 ----

/** 预览类型枚举 */
export enum PreviewType {
  IMAGE = 'image',
  VIDEO = 'video',
  AUDIO = 'audio',
  CODE = 'code',
  MARKDOWN = 'markdown',
  OFFICE = 'office',
  UNKNOWN = 'unknown',
}

/** 预览文件项 */
export interface PreviewFileItem {
  userFileId: number
  fileName: string
  filePath: string
  extendName: string
  fileSize: number
  fileType: number
  previewUrl?: string
  content?: string
  /** 同目录同类型文件列表（批量预览） */
  fileList?: PreviewFileItem[]
}

/** 音乐元数据（后端 FileDetailVO.music 字段） */
export interface MusicMetadata {
  trackLength?: string
  albumImage?: string
  lyrics?: string
  artist?: string
  album?: string
}

/** 文档版本历史 */
export interface DocumentHistory {
  version: number
  fileSize: number
  createTime: string
}

/** OnlyOffice 文档配置 */
export interface OnlyOfficeConfig {
  document: {
    fileType: string
    key: string
    title: string
    url: string
  }
  editorConfig: {
    callbackUrl: string
    lang: string
    mode: string
    user: {
      id: string
      name: string
    }
  }
  token: string
  /** OnlyOffice API JS 加载地址（由后端返回） */
  docserviceApiUrl?: string
}

/** 文件分类枚举 */
export enum FileType {
  ALL = 0,
  IMAGE = 1,
  DOCUMENT = 2,
  VIDEO = 3,
  MUSIC = 4,
  OTHER = 5,
  RECYCLE = 6,
  SHARE = 8,
}

/** 文件视图模式枚举 */
export enum FileViewMode {
  LIST = 0,
  GRID = 1,
  TIMELINE = 2,
}

/**
 * 文件扩展名 → 图标路径映射。
 * 图标资源位于 src/assets/icons/file/ 目录。
 */
export const fileImgMap: Record<string, string> = {
  avi: '/img/file/file_avi.png',
  bat: '/img/file/file_unknown.png',
  c: '/img/file/file_c.png',
  cpp: '/img/file/file_c++.png',
  cs: '/img/file/file_c#.png',
  chm: '/img/file/file_chm.png',
  css: '/img/file/file_css.png',
  gif: '/img/file/file_gif.png',
  go: '/img/file/file_go.png',
  py: '/img/file/file_python.png',
  styl: '/img/file/file_stylus.png',
  less: '/img/file/file_less.png',
  conf: '/img/file/file_nginx.png',
  m: '/img/file/file_objective_c.png',
  scss: '/img/file/file_scss.png',
  sass: '/img/file/file_sass.png',
  csv: '/img/file/file_csv.png',
  dmg: '/img/file/file_dmg.png',
  dir: '/img/file/dir.png',
  doc: '/img/file/file_word.svg',
  docx: '/img/file/file_word.svg',
  exe: '/img/file/file_exe.png',
  html: '/img/file/file_html.png',
  jar: '/img/file/file_jar.png',
  java: '/img/file/file_java.png',
  js: '/img/file/file_js.png',
  json: '/img/file/file_json.png',
  jsp: '/img/file/file_jsp.png',
  kt: '/img/file/file_kotlin.png',
  mp3: '/img/file/file_music.png',
  flac: '/img/file/file_flac.svg',
  oa: '/img/file/file_oa.png',
  open: '/img/file/file_open.png',
  pdf: '/img/file/file_pdf.png',
  php: '/img/file/file_php.png',
  png: '/img/file/file_unknown.png',
  ppt: '/img/file/file_ppt.svg',
  pptx: '/img/file/file_ppt.svg',
  properties: '/img/file/file_properties.png',
  ps1: '/img/file/file_powershell.png',
  r: '/img/file/file_r.png',
  rar: '/img/file/file_rar.png',
  rs: '/img/file/file_rust.png',
  rtf: '/img/file/file_rtf.png',
  sh: '/img/file/file_shell.png',
  sql: '/img/file/file_sql.png',
  svg: '/img/file/file_svg.png',
  swift: '/img/file/file_swift.png',
  ts: '/img/file/file_typescript.png',
  txt: '/img/file/file_txt.png',
  vue: '/img/file/file_vue.png',
  xls: '/img/file/file_excel.svg',
  xlsx: '/img/file/file_excel.svg',
  xml: '/img/file/file_xml.png',
  zip: '/img/file/file_zip.png',
  '7z': '/img/file/file_7z.svg',
  tar: '/img/file/file_tar.svg',
  md: '/img/file/file_markdown.png',
  markdown: '/img/file/file_markdown.png',
  yaml: '/img/file/file_yaml.png',
  yml: '/img/file/file_yaml.png',
}

/** Office 文件类型 */
export const officeFileType: string[] = ['ppt', 'pptx', 'doc', 'docx', 'xls', 'xlsx']

/** Markdown 文件类型 */
export const markdownFileType: string[] = ['markdown', 'md']

/** 文件表格默认列 */
export const allColumnList: string[] = ['extendName', 'fileSize', 'uploadTime', 'deleteTime']

/**
 * 代码文件后缀 → CodeMirror language/mime 映射。
 * 供后续 frontend-preview change 的 codePreview 功能使用。
 */
export const fileSuffixCodeModeMap: Record<string, { language: string; mime: string }> = {
  c: { language: 'c', mime: 'text/x-csrc' },
  cpp: { language: 'cpp', mime: 'text/x-c++src' },
  cs: { language: 'csharp', mime: 'text/x-csharp' },
  css: { language: 'css', mime: 'text/css' },
  go: { language: 'go', mime: 'text/x-go' },
  html: { language: 'html', mime: 'text/html' },
  java: { language: 'java', mime: 'text/x-java' },
  js: { language: 'javascript', mime: 'text/javascript' },
  json: { language: 'json', mime: 'application/json' },
  jsp: { language: 'jsp', mime: 'application/x-jsp' },
  kt: { language: 'kotlin', mime: 'text/x-kotlin' },
  less: { language: 'less', mime: 'text/x-less' },
  m: { language: 'objectivec', mime: 'text/x-objectivec' },
  php: { language: 'php', mime: 'application/x-httpd-php' },
  py: { language: 'python', mime: 'text/x-python' },
  r: { language: 'r', mime: 'text/x-rsrc' },
  rs: { language: 'rust', mime: 'text/x-rustsrc' },
  rb: { language: 'ruby', mime: 'text/x-ruby' },
  sass: { language: 'sass', mime: 'text/x-sass' },
  scss: { language: 'scss', mime: 'text/x-scss' },
  sh: { language: 'shell', mime: 'application/x-sh' },
  sql: { language: 'sql', mime: 'text/x-sql' },
  swift: { language: 'swift', mime: 'text/x-swift' },
  ts: { language: 'typescript', mime: 'text/typescript' },
  vue: { language: 'vue', mime: 'text/x-vue' },
  xml: { language: 'xml', mime: 'application/xml' },
  yaml: { language: 'yaml', mime: 'text/x-yaml' },
  yml: { language: 'yaml', mime: 'text/x-yaml' },
  md: { language: 'markdown', mime: 'text/x-markdown' },
  markdown: { language: 'markdown', mime: 'text/x-markdown' },
  txt: { language: '', mime: 'text/plain' },
  bat: { language: '', mime: 'application/x-bat' },
  properties: { language: '', mime: 'text/x-properties' },
  conf: { language: '', mime: 'text/x-nginx-conf' },
  http: { language: '', mime: 'message/http' },
}
