import { ref } from 'vue'
import { useUploadFileStore } from '@/stores/uploadFile'
import { useSideMenuStore } from '@/stores/sideMenu'
import type { UploadTask } from '@/types/file'
import {
  uploadFile as uploadFileApi,
  speedUpload as speedUploadApi,
  initChunkUpload,
  uploadChunk,
  mergeChunks,
} from '@/api/file'

/** 分片大小：1MB */
const CHUNK_SIZE = 1024 * 1024
/** 最大并发上传数 */
const MAX_CONCURRENT = 3
/** 最大重试次数 */
const MAX_RETRIES = 3
/** 小文件阈值：10MB */
const SMALL_FILE_THRESHOLD = 10 * 1024 * 1024

let taskIdCounter = 0

/**
 * 生成唯一任务 ID。
 */
function generateTaskId(): string {
  return `upload_${Date.now()}_${++taskIdCounter}`
}

/**
 * 使用 SparkMD5 计算文件 MD5。
 * 动态导入避免打包时强制包含。
 */
async function calculateMD5(file: File): Promise<string> {
  const SparkMD5 = (await import('spark-md5')).default
  const spark = new SparkMD5.ArrayBuffer()
  const buffer = await file.slice(0, file.size).arrayBuffer()
  spark.append(buffer)
  return spark.end()
}

/**
 * 上传管理器 composable。
 * 封装 MD5 计算、秒传检测、分片上传、并发控制。
 */
export function useUploadManager() {
  const uploadFileStore = useUploadFileStore()
  const sideMenuStore = useSideMenuStore()
  const uploading = ref(false)

  /**
   * 检查存储配额是否充足。
   * 剩余空间 = totalStorageSize - storageSize，需 >= 文件大小。
   */
  function checkQuota(fileSize: number): boolean {
    const remaining = sideMenuStore.totalStorageValue - sideMenuStore.storageValue
    if (remaining < fileSize) {
      throw new Error('存储空间不足，请清理文件或扩容后再试')
    }
    return true
  }

  /**
   * 上传单个文件。
   * 根据文件大小自动选择普通上传或分片上传。
   */
  async function uploadFile(file: File, filePath: string): Promise<void> {
    const taskId = generateTaskId()
    const task: UploadTask = {
      id: taskId,
      fileName: file.name,
      fileSize: file.size,
      progress: 0,
      status: 'pending',
      errorMsg: '',
      file,
    }

    uploadFileStore.addTask(task)

    try {
      // 0. 配额校验
      checkQuota(file.size)

      // 1. 计算 MD5
      uploadFileStore.updateStatus(taskId, 'hashing')
      const fileHash = await calculateMD5(file)

      // 2. 秒传检测
      uploadFileStore.updateProgress(taskId, 10)
      const speedResult = await speedUploadApi({
        fileName: file.name,
        filePath,
        fileSize: file.size,
        fileHash,
      })

      if (speedResult) {
        // 秒传成功
        uploadFileStore.updateProgress(taskId, 100)
        uploadFileStore.updateStatus(taskId, 'success')
        return
      }

      // 3. 根据文件大小选择上传方式
      if (file.size <= SMALL_FILE_THRESHOLD) {
        // 普通上传
        uploadFileStore.updateStatus(taskId, 'uploading')
        uploadFileStore.updateProgress(taskId, 30)
        await uploadFileApi(file, filePath)
        uploadFileStore.updateProgress(taskId, 100)
        uploadFileStore.updateStatus(taskId, 'success')
      } else {
        // 分片上传
        uploadFileStore.updateStatus(taskId, 'uploading')
        await chunkedUpload(file, filePath, fileHash, taskId)
      }
    } catch (error) {
      const msg = error instanceof Error ? error.message : '上传失败'
      uploadFileStore.updateStatus(taskId, 'error', msg)
    }
  }

  /**
   * 分片上传流程。
   */
  async function chunkedUpload(
    file: File,
    filePath: string,
    fileHash: string,
    taskId: string,
  ): Promise<void> {
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

    // 初始化分片上传
    const chunkTaskId = await initChunkUpload({
      fileName: file.name,
      filePath,
      fileSize: file.size,
      fileHash,
      totalChunks,
    })

    // 上传所有分片，并发控制
    const chunks = Array.from({ length: totalChunks }, (_, i) => i)
    await runWithConcurrency(chunks, MAX_CONCURRENT, async (chunkIndex: number) => {
      const start = chunkIndex * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      const chunkBlob = file.slice(start, end)
      const chunkFile = new File([chunkBlob], `chunk_${chunkIndex}`, { type: file.type })

      await retryWithBackoff(async () => {
        await uploadChunk(chunkTaskId, chunkIndex, chunkFile)
      }, MAX_RETRIES)

      // 更新进度
      const progress = 30 + Math.round(((chunkIndex + 1) / totalChunks) * 65)
      uploadFileStore.updateProgress(taskId, progress)
    })

    // 合并分片
    uploadFileStore.updateProgress(taskId, 95)
    await mergeChunks(chunkTaskId, filePath)
    uploadFileStore.updateProgress(taskId, 100)
    uploadFileStore.updateStatus(taskId, 'success')
  }

  /**
   * 并发控制执行器。
   */
  async function runWithConcurrency<T>(
    items: T[],
    limit: number,
    fn: (item: T) => Promise<void>,
  ): Promise<void> {
    const executing: Promise<void>[] = []

    for (const item of items) {
      const p = fn(item).then(() => {
        executing.splice(executing.indexOf(p), 1)
      })
      executing.push(p)

      if (executing.length >= limit) {
        await Promise.race(executing)
      }
    }

    await Promise.all(executing)
  }

  /**
   * 指数退避重试。
   */
  async function retryWithBackoff(
    fn: () => Promise<void>,
    maxRetries: number,
  ): Promise<void> {
    let lastError: Error | null = null
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        await fn()
        return
      } catch (error) {
        lastError = error instanceof Error ? error : new Error(String(error))
        if (attempt < maxRetries) {
          await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000))
        }
      }
    }
    throw lastError
  }

  /**
   * 批量上传多个文件。
   */
  async function uploadFiles(files: File[], filePath: string): Promise<void> {
    uploading.value = true
    try {
      // 并发控制上传
      const fileArr = Array.from(files)
      await runWithConcurrency(fileArr, MAX_CONCURRENT, async (file) => {
        await uploadFile(file, filePath)
      })
    } finally {
      uploading.value = false
    }
  }

  return {
    uploading,
    uploadFile,
    uploadFiles,
  }
}
