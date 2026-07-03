<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { getDocumentPreviewConfig, getDocumentEditConfig } from '@/api/file'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const editorContainer = ref<HTMLElement | null>(null)
let docEditor: any = null

onUnmounted(() => {
  if (docEditor) {
    docEditor.destroyEditor?.()
    docEditor = null
  }
})

onMounted(async () => {
  const userFileId = Number(route.query.userFileId)
  const mode = (route.query.mode as string) || 'preview'

  if (!userFileId) {
    error.value = '缺少文件 ID 参数'
    loading.value = false
    return
  }

  try {
    // 获取配置
    const config = mode === 'edit'
      ? await getDocumentEditConfig(userFileId)
      : await getDocumentPreviewConfig(userFileId)

    // 动态加载 OnlyOffice API JS（使用后端返回的 docserviceApiUrl）
    const apiUrl = config.docserviceApiUrl || import.meta.env.VITE_ONLYOFFICE_API_URL || '/onlyoffice/web-apps/apps/api/documents/api.js'
    await loadScript(apiUrl)

    // 先隐藏 loading，让编辑器容器渲染到 DOM
    loading.value = false

    // 等待 DOM 更新后再初始化编辑器
    await nextTick()

    if ((window as any).DocsAPI && editorContainer.value) {
      docEditor = new (window as any).DocsAPI.DocEditor('editor-container', {
        ...config,
        width: '100%',
        height: '100%',
      })
    }
  } catch (e: any) {
    error.value = e?.message || '加载文档预览失败'
    loading.value = false
  }
})

function loadScript(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = url
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('OnlyOffice API 加载失败'))
    document.head.appendChild(script)
  })
}
</script>

<template>
  <div class="office-preview-page">
    <div v-if="loading" class="loading-state">
      <el-icon class="loading-icon" :size="32"><Loading /></el-icon>
      <p>正在加载文档预览...</p>
    </div>
    <div v-else-if="error" class="error-state">
      <el-result icon="error" :title="error" />
    </div>
    <div v-else id="editor-container" class="editor-container" />
  </div>
</template>

<style lang="scss" scoped>
.office-preview-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #fff;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
  gap: 16px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.editor-container {
  width: 100%;
  height: 100%;
}
</style>
