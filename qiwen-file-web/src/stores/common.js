import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCommonStore = defineStore('common', () => {
  // ---- state ----
  const isLoading = ref(false)
  const breadCrumbList = ref([])

  // ---- actions ----
  function setLoading(val) {
    isLoading.value = val
  }

  function setBreadCrumb(list) {
    breadCrumbList.value = list
  }

  return { isLoading, breadCrumbList, setLoading, setBreadCrumb }
})
