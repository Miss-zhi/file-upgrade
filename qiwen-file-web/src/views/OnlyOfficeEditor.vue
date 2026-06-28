<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getEditorConfig } from '_api/onlyoffice'

const route = useRoute()
const config = ref<any>(null)
const apiUrl = ref('')

async function loadConfig() {
  const fileId = route.params.fileId as string
  const res: any = await getEditorConfig(fileId)
  if (res.success) {
    config.value = res.data
    apiUrl.value = (config.value.editorConfig?.callbackUrl || '').replace('/callback', '') + '/web-apps/apps/api/documents/api.js'
  }
}

onMounted(loadConfig)
</script>

<template>
  <div class="onlyoffice-editor">
    <div v-if="config" class="editor-container">
      <iframe
        :src="`${apiUrl}?config=${encodeURIComponent(JSON.stringify(config))}`"
        style="width:100%;height:calc(100vh - 60px);border:none"
        allow="fullscreen"
      />
    </div>
    <div v-else class="loading">
      <el-icon :size="40" class="is-loading"><Loading /></el-icon>
      <p>加载编辑器中...</p>
    </div>
  </div>
</template>

<style lang="stylus" scoped>
.onlyoffice-editor
  .editor-container
    width: 100%

  .loading
    display: flex
    flex-direction: column
    align-items: center
    justify-content: center
    height: calc(100vh - 60px)
    color: #909399

    p
      margin-top: 12px
</style>
