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
      try {
        docEditor = new (window as any).DocsAPI.DocEditor('editor-container', stripNulls({
          ...config,
          width: '100%',
          height: '100%',
        }))
      } catch (err) {
        console.error('[OfficePreview] DocEditor 创建失败:', err)
        error.value = '文档编辑器初始化失败'
      }
    } else {
      if (!(window as any).DocsAPI) {
        console.error('[OfficePreview] DocsAPI 未定义')
        error.value = 'OnlyOffice API 加载异常'
      }
    }
  } catch (e: any) {
    error.value = e?.message || '加载文档预览失败'
    loading.value = false
  }
})

/**
 * 递归移除对象中的 null 值。
 *
 * OnlyOffice DocsAPI 内部的 extend() 做递归深合并时，
 * 遇到 null 会因 typeof null === 'object' 而崩溃，
 * 因此传给 DocEditor 之前必须剔除所有 null 字段。
 */
function stripNulls(obj: Record<string, any>): Record<string, any> {
  const result: Record<string, any> = {}
  for (const [key, value] of Object.entries(obj)) {
    if (value === null || value === undefined) continue
    if (typeof value === 'object' && !Array.isArray(value)) {
      result[key] = stripNulls(value)
    } else {
      result[key] = value
    }
  }
  return result
}

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
    <div v-else id="editor-container" ref="editorContainer" class="editor-container" />
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
