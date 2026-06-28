import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getFileListByPath } from '_api/file'

export const useFileListStore = defineStore('fileList', () => {
  // ---- state ----
  const files = ref([])
  const currentPath = ref('/')
  const total = ref(0)
  const loading = ref(false)

  // ---- getters ----
  const folderList = computed(() =>
    files.value.filter(f => f.isFolder)
  )

  const fileList = computed(() =>
    files.value.filter(f => !f.isFolder)
  )

  // ---- actions ----
  async function fetchFiles(path) {
    loading.value = true
    currentPath.value = path || '/'
    const res = await getFileListByPath({ path: currentPath.value })
    if (res.success) {
      files.value = res.dataList || res.data || []
      total.value = Number(res.total) || 0
    }
    loading.value = false
  }

  function setFiles(data) {
    files.value = data
  }

  return { files, currentPath, total, loading, folderList, fileList, fetchFiles, setFiles }
})
