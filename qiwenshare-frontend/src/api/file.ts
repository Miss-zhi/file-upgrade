import client from './client'
import type { RestResult } from '@/types/api'
import type {
  FileInfo,
  FileDetail,
  TreeNode,
  UploadResult,
  ShareInfo,
  BatchOperationResult,
  PageResult,
  FileListParams,
  CategoryListParams,
  RenameFileDTO,
  MoveFileDTO,
  BatchMoveFileDTO,
  CopyFileDTO,
  BatchCopyFileDTO,
  CreateFoldDTO,
  CreateFileDTO,
  DeleteFileDTO,
  BatchDeleteFileDTO,
  RestoreFileDTO,
  ShareCreateDTO,
  ShareVerifyDTO,
  SaveShareFileDTO,
  SpeedUploadDTO,
  ChunkUploadInitDTO,
  OnlyOfficeConfig,
  DocumentHistory,
  QuotaInfoVO,
} from '@/types/file'

// ---- 存储信息 ----

/** 存储信息 */
interface StorageInfo {
  storageSize: number
  totalStorageSize: number
}

/** 获取用户存储容量信息（备用） */
export async function getStorage(): Promise<StorageInfo> {
  const { data } = await client.get<RestResult<StorageInfo>>('/filetransfer/getstorage')
  return data.data
}

/** 获取用户配额信息（优先使用） */
export async function getQuotaInfo(): Promise<QuotaInfoVO> {
  const { data } = await client.get<RestResult<QuotaInfoVO>>('/quota/info')
  return data.data
}

// ---- FileController (11 端点) ----

/** 获取文件列表 */
export async function getFileList(params: FileListParams): Promise<PageResult<FileInfo>> {
  const { data } = await client.get<RestResult<PageResult<FileInfo>>>('/file/getfilelist', { params })
  return data.data
}

/** 按分类浏览文件列表 */
export async function getFileListByCategory(params: CategoryListParams): Promise<PageResult<FileInfo>> {
  const { data } = await client.get<RestResult<PageResult<FileInfo>>>('/file/getfilelist/bycategory', { params })
  return data.data
}

/** 重命名文件 */
export async function renameFile(dto: RenameFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/file/renamefile', dto)
  return data.data
}

/** 移动文件 */
export async function moveFile(dto: MoveFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/file/movefile', dto)
  return data.data
}

/** 批量移动文件 */
export async function batchMoveFile(dto: BatchMoveFileDTO): Promise<BatchOperationResult> {
  const { data } = await client.post<RestResult<BatchOperationResult>>('/file/batchmovefile', dto)
  return data.data
}

/** 复制文件 */
export async function copyFile(dto: CopyFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/file/copyfile', dto)
  return data.data
}

/** 批量复制文件 */
export async function batchCopyFile(dto: BatchCopyFileDTO): Promise<BatchOperationResult> {
  const { data } = await client.post<RestResult<BatchOperationResult>>('/file/batchcopyfile', dto)
  return data.data
}

/** 创建文件夹 */
export async function createFolder(dto: CreateFoldDTO): Promise<number> {
  const { data } = await client.post<RestResult<number>>('/file/createfold', dto)
  return data.data
}

/** 创建空文件 */
export async function createFile(dto: CreateFileDTO): Promise<number> {
  const { data } = await client.post<RestResult<number>>('/file/createfile', dto)
  return data.data
}

/** 获取文件详情 */
export async function getFileDetail(userFileId: number): Promise<FileDetail> {
  const { data } = await client.get<RestResult<FileDetail>>(`/file/getfiledetail/${userFileId}`)
  return data.data
}

/** 获取文件树（仅文件夹） */
export async function getFileTree(): Promise<TreeNode[]> {
  const { data } = await client.get<RestResult<TreeNode[]>>('/file/getfiletree')
  return data.data
}

// ---- FileTransferController (7 端点) ----

/** 普通上传（≤10MB） */
export async function uploadFile(file: File, filePath: string): Promise<UploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('filePath', filePath)
  const { data } = await client.post<RestResult<UploadResult>>('/filetransfer/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}

/** 秒传检测 */
export async function speedUpload(dto: SpeedUploadDTO): Promise<UploadResult | null> {
  const { data } = await client.post<RestResult<UploadResult | null>>('/filetransfer/upload/speed', dto)
  return data.data
}

/** 初始化分片上传 */
export async function initChunkUpload(dto: ChunkUploadInitDTO): Promise<string> {
  const { data } = await client.post<RestResult<string>>('/filetransfer/upload/chunk/init', dto)
  return data.data
}

/** 上传分片 */
export async function uploadChunk(taskId: string, chunkIndex: number, chunkData: File): Promise<void> {
  const formData = new FormData()
  formData.append('taskId', taskId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunkData', chunkData)
  await client.post<RestResult<void>>('/filetransfer/upload/chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 合并分片 */
export async function mergeChunks(taskId: string, filePath: string): Promise<UploadResult> {
  const { data } = await client.post<RestResult<UploadResult>>('/filetransfer/upload/chunk/merge', null, {
    params: { taskId, filePath },
  })
  return data.data
}

/** 下载文件（触发浏览器下载） */
export function downloadFile(userFileId: number): void {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  window.open(`${baseUrl}/filetransfer/download/${userFileId}`, '_blank')
}

/** 批量下载（打包为 ZIP） */
export async function batchDownload(userFileIds: number[]): Promise<void> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const response = await fetch(`${baseUrl}/filetransfer/batch-download`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(userFileIds),
  })
  if (!response.ok) {
    throw new Error('批量下载失败')
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'files.zip'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// ---- RecoveryFileController (6 端点) ----

/** 回收站列表 */
export async function getRecycleList(page: number, size: number): Promise<PageResult<FileInfo>> {
  const { data } = await client.get<RestResult<PageResult<FileInfo>>>('/recycle/list', {
    params: { page, size },
  })
  return data.data
}

/** 软删除文件 */
export async function deleteFile(dto: DeleteFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/recycle/deletefile', dto)
  return data.data
}

/** 批量软删除文件 */
export async function batchDeleteFile(dto: BatchDeleteFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/recycle/batchdeletefile', dto)
  return data.data
}

/** 恢复文件 */
export async function restoreFile(dto: RestoreFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/recycle/restorefile', dto)
  return data.data
}

/** 永久删除文件 */
export async function deletePermanent(dto: RestoreFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/recycle/deletepermanent', dto)
  return data.data
}

/** 清空回收站 */
export async function deleteAllRecycle(): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/recycle/deleteall')
  return data.data
}

// ---- FileShareController (6 端点) ----

/** 创建分享 */
export async function createShare(dto: ShareCreateDTO): Promise<ShareInfo> {
  const { data } = await client.post<RestResult<ShareInfo>>('/share/createshare', dto)
  return data.data
}

/** 获取分享信息（公开端点） */
export async function getShareInfo(shareCode: string): Promise<ShareInfo> {
  const { data } = await client.get<RestResult<ShareInfo>>(`/share/info/${shareCode}`)
  return data.data
}

/** 验证提取码 */
export async function verifyShare(dto: ShareVerifyDTO): Promise<ShareInfo> {
  const { data } = await client.post<RestResult<ShareInfo>>('/share/verifyshare', dto)
  return data.data
}

/** 下载分享文件 */
export function downloadShareFile(shareCode: string): void {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  window.open(`${baseUrl}/share/download/${shareCode}`, '_blank')
}

/** 我的分享列表 */
export async function getMyShares(): Promise<ShareInfo[]> {
  const { data } = await client.get<RestResult<ShareInfo[]>>('/share/myshares')
  return data.data
}

/** 取消分享 */
export async function cancelShare(shareId: number): Promise<void> {
  const { data } = await client.delete<RestResult<void>>(`/share/cancelshare/${shareId}`)
  return data.data
}

/** 保存分享文件到网盘 */
export async function saveShareFile(dto: SaveShareFileDTO): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/share/saveshare', dto)
  return data.data
}

// ---- 文件内容获取（预览用） ----

/** 获取文件二进制内容（用于图片/视频/音频预览） */
export async function getFileContent(userFileId: number): Promise<Blob> {
  const { data } = await client.get<Blob>(`/filetransfer/download/${userFileId}`, {
    responseType: 'blob',
  })
  return data
}

/** 获取文件文本内容（用于代码/Markdown 预览） */
export async function getFileText(userFileId: number): Promise<string> {
  const { data } = await client.get<string>(`/filetransfer/download/${userFileId}`, {
    responseType: 'text',
  })
  return data
}

/** 修改文件文本内容（代码/文本在线编辑保存） */
export async function modifyFileContent(userFileId: number, fileContent: string): Promise<void> {
  const { data } = await client.post<RestResult<void>>('/file/update', { userFileId, fileContent })
  return data.data
}

/** 获取 OnlyOffice 预览配置 */
export async function getDocumentPreviewConfig(userFileId: number): Promise<OnlyOfficeConfig> {
  const { data } = await client.post<RestResult<OnlyOfficeConfig>>('/document/preview', { userFileId })
  return data.data
}

/** 获取 OnlyOffice 编辑配置 */
export async function getDocumentEditConfig(userFileId: number): Promise<OnlyOfficeConfig> {
  const { data } = await client.post<RestResult<OnlyOfficeConfig>>('/document/edit', { userFileId })
  return data.data
}

/** 获取文档版本历史 */
export async function getDocumentHistory(userFileId: number): Promise<DocumentHistory[]> {
  const { data } = await client.get<RestResult<DocumentHistory[]>>(`/document/${userFileId}/history`)
  return data.data
}

/** 回滚文档到指定版本 */
export async function restoreDocumentVersion(userFileId: number, version: number): Promise<void> {
  const { data } = await client.post<RestResult<void>>(`/document/${userFileId}/history/${version}/restore`)
  return data.data
}
