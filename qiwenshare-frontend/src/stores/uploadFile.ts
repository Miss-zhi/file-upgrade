import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { UploadTask, UploadTaskStatus } from '@/types/file'

/**
 * 上传文件状态管理。
 * 管理上传遮罩显隐和上传任务队列。
 */
export const useUploadFileStore = defineStore('uploadFile', () => {
  const showUploadMask = ref(false)
  const uploadQueue = ref<UploadTask[]>([])
  const isPanelCollapsed = ref(false)
  const isPanelVisible = ref(false)

  /** 切换上传遮罩显隐 */
  function toggleUploadMask(): void {
    showUploadMask.value = !showUploadMask.value
  }

  /** 添加上传任务 */
  function addTask(task: UploadTask): void {
    uploadQueue.value.push(task)
    isPanelVisible.value = true
    isPanelCollapsed.value = false
  }

  /** 移除上传任务 */
  function removeTask(id: string): void {
    uploadQueue.value = uploadQueue.value.filter((t) => t.id !== id)
  }

  /** 更新上传进度 */
  function updateProgress(id: string, progress: number): void {
    const task = uploadQueue.value.find((t) => t.id === id)
    if (task) {
      task.progress = progress
    }
  }

  /** 更新上传状态 */
  function updateStatus(id: string, status: UploadTaskStatus, errorMsg?: string): void {
    const task = uploadQueue.value.find((t) => t.id === id)
    if (task) {
      task.status = status
      if (errorMsg !== undefined) {
        task.errorMsg = errorMsg
      }
    }
  }

  /** 清空已完成的任务 */
  function clearCompleted(): void {
    uploadQueue.value = uploadQueue.value.filter(
      (t) => t.status !== 'success' && t.status !== 'error',
    )
  }

  /** 进行中的上传任务数 */
  const activeTaskCount = computed(() =>
    uploadQueue.value.filter((t) => t.status === 'pending' || t.status === 'hashing' || t.status === 'uploading').length,
  )

  return {
    showUploadMask,
    toggleUploadMask,
    uploadQueue,
    isPanelCollapsed,
    isPanelVisible,
    addTask,
    removeTask,
    updateProgress,
    updateStatus,
    clearCompleted,
    activeTaskCount,
  }
})
