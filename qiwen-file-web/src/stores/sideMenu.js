import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSideMenuStore = defineStore('sideMenu', () => {
  // ---- state ----
  const isCollapse = ref(false)
  const activeMenu = ref('/home')

  // ---- actions ----
  function toggleCollapse() {
    isCollapse.value = !isCollapse.value
  }

  function setActiveMenu(path) {
    activeMenu.value = path
  }

  return { isCollapse, activeMenu, toggleCollapse, setActiveMenu }
})
