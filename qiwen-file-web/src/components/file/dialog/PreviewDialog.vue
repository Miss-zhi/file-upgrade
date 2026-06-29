<script setup lang="ts">
import { ref, computed } from 'vue'

interface FileItem {
  id: string
  fileName: string
  fileType: string
}

const visible = ref(false)
const file = ref<FileItem | null>(null)
const textContent = ref('')

const previewMode = computed<'image'|'video'|'text'|'pdf'|'unsupported'>(() => {
  if (!file.value) return 'unsupported'
  const ext = file.value.fileName.split('.').pop()?.toLowerCase() || ''
  const type = file.value.fileType || ''
  if (/jpg|jpeg|png|gif|webp|svg/i.test(ext)) return 'image'
  if (/mp4|webm|ogg/i.test(ext)) return 'video'
  if (/txt|md|json|xml|js|ts|css|html|java|py|yml|yaml/i.test(ext)) return 'text'
  if (/pdf/i.test(ext)) return 'pdf'
  return 'unsupported'
})

const previewUrl = computed(() => file.value ? `/api/file/preview/${file.value.id}` : '')

async function open(item: FileItem) {
  file.value = item
  visible.value = true
  if (previewMode.value === 'text') {
    try {
      const res: any = await (await fetch(`/api/file/preview/text/${item.id}`)).json()
      textContent.value = res.data || ''
    } catch { textContent.value = '' }
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="file?.fileName"
    :width="previewMode === 'text' ? '800px' : '900px'"
    top="5vh"
    destroy-on-close
  >
    <!-- 图片 -->
    <div v-if="previewMode === 'image'" class="preview-image">
      <el-image :src="previewUrl" fit="contain" style="max-height:70vh" />
    </div>

    <!-- 视频 -->
    <div v-else-if="previewMode === 'video'" class="preview-video">
      <video :src="previewUrl" controls style="max-width:100%;max-height:70vh" />
    </div>

    <!-- 文本 -->
    <div v-else-if="previewMode === 'text'" class="preview-text">
      <pre><code>{{ textContent }}</code></pre>
    </div>

    <!-- PDF -->
    <div v-else-if="previewMode === 'pdf'" class="preview-pdf">
      <iframe :src="previewUrl" style="width:100%;height:70vh;border:none" />
    </div>

    <!-- 不支持 -->
    <div v-else class="preview-unsupported">
      <el-empty description="不支持预览此文件类型" />
    </div>
  </el-dialog>
</template>

<style lang="stylus" scoped>
.preview-image, .preview-video
  text-align: center

.preview-text
  max-height: 70vh
  overflow: auto
  background: #f5f7fa
  border-radius: 4px
  padding: 16px

  pre
    margin: 0
    white-space: pre-wrap
    word-break: break-all
    font-size: 13px
    line-height: 1.6

.preview-unsupported
  padding: 40px 0
</style>
