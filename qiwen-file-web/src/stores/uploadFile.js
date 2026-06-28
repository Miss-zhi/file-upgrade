import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUploadFileStore = defineStore('uploadFile', () => {
  // ---- state ----
  const uploadList = ref([])
  const isUploading = ref(false)

  // ---- getters ----
  const uploadCount = computed(() => uploadList.value.length)
  const progress = computed(() => {
    if (!uploadList.value.length) return 0
    const total = uploadList.value.reduce((sum, f) => sum + (f.percentage || 0), 0)
    return Math.round(total / uploadList.value.length)
  })

  // ---- actions ----
  function addFile(file) {
    uploadList.value.push({ ...file, percentage: 0, status: 'pending' })
  }

  function updateProgress(fileUid, percentage) {
    const file = uploadList.value.find(f => f.uid === fileUid)
    if (file) file.percentage = percentage
  }

  function clearUploaded() {
    uploadList.value = uploadList.value.filter(f =>
      f.status === 'pending' || f.status === 'uploading'
    )
  }

  return { uploadList, isUploading, uploadCount, progress, addFile, updateProgress, clearUploaded }
})
