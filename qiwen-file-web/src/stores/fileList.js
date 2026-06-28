import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFileListByPath, uploadFile as uploadApi, deleteFile as deleteApi, createFolder as createFolderApi } from '_api/file'
import { ElMessage } from 'element-plus'

export const useFileListStore = defineStore('fileList', () => {
  const files = ref([])
  const currentPath = ref('/')
  const loading = ref(false)

  async function fetchFiles(path) {
    loading.value = true
    currentPath.value = path || '/'
    try {
      const res = await getFileListByPath({ path: currentPath.value })
      if (res.success) {
        files.value = res.data || []
      }
    } catch {
      // 错误在拦截器中处理
    }
    loading.value = false
  }

  async function uploadFile(file, path) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('path', path || '/')
    try {
      const res = await uploadApi(formData)
      if (res.success) {
        ElMessage.success('上传成功')
        await fetchFiles(currentPath.value)
      }
    } catch {
      // handled
    }
  }

  async function deleteFile(id) {
    try {
      const res = await deleteApi({ id })
      if (res.success) {
        await fetchFiles(currentPath.value)
      }
    } catch {
      // handled
    }
  }

  async function createFolder(path, folderName) {
    try {
      const res = await createFolderApi(path, folderName)
      if (res.success) {
        ElMessage.success('文件夹创建成功')
        await fetchFiles(currentPath.value)
      }
    } catch {
      // handled
    }
  }

  return { files, currentPath, loading, fetchFiles, uploadFile, deleteFile, createFolder }
})
