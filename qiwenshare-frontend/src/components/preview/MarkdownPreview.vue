<script setup lang="ts">
import { ref, inject, watch } from 'vue'
import { Download, InfoFilled, Promotion } from '@element-plus/icons-vue'
import { MARKDOWN_PREVIEW_KEY } from '@/composables/previewKeys'
import { MdEditor, type ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const {
  visible,
  file,
  markdownText,
  loading,
  editable,
  isModified,
  open,
  openEdit,
  save,
  close,
} = inject(MARKDOWN_PREVIEW_KEY)!

defineExpose({ open, openEdit })

const editorRef = ref<InstanceType<typeof MdEditor> | null>(null)

/** 工具栏配置（对齐旧项目 mavon-editor） */
const toolbars: ToolbarNames[] = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'sub', 'sup', 'quote', 'unorderedList', 'orderedList', '-',
  'codeRow', 'code', 'link', 'image', 'table', '-',
  'revoke', 'next', 'save', '-',
  'preview', 'previewOnly', 'catalog', '=',
  'pageFullscreen', 'fullscreen',
]

function onDownload(): void {
  if (file.value) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    window.open(`${baseUrl}/filetransfer/download/${file.value.userFileId}`, '_blank')
  }
}

/** md-editor-v3 的 onSave 事件 */
function onEditorSave(_text: string, _html: Promise<string>): void {
  save()
}

/** 关闭时重置编辑器内容，避免下次打开残留 */
watch(visible, (val) => {
  if (!val) {
    // 延迟重置，等动画结束后再清空
    setTimeout(() => {
      if (!visible.value) {
        markdownText.value = ''
      }
    }, 300)
  }
})
</script>

<template>
  <Teleport to="body">
    <div v-show="visible" class="markdown-preview-overlay">
      <!-- 顶部栏 -->
      <div class="tip-wrapper">
        <div class="tip-left">
          <span class="file-name" :title="file?.fileName || ''">
            {{ file?.fileName || '' }}
            <span class="un-save" v-if="editable && isModified">（未保存）</span>
          </span>
        </div>
        <div class="tip-center">
          <span class="editor-title">在线预览{{ editable ? ' & 编辑' : '' }}</span>
        </div>
        <div class="tip-right">
          <el-tooltip effect="dark" placement="bottom" popper-style="z-index: 10000">
            <template #content>
              <div style="line-height: 1.8">
                操作提示: <br />
                1. 按 Esc 键可退出查看；<br />
                2. 支持在线编辑、保存、下载
              </div>
            </template>
            <el-link :underline="false" class="tip-link tip-help-link">
              <span class="help-text">操作提示</span>
              <el-icon><InfoFilled /></el-icon>
            </el-link>
          </el-tooltip>
          <el-link :underline="false" class="tip-link" @click="onDownload">
            <el-icon :size="20"><Download /></el-icon>
          </el-link>
          <el-link
            :underline="false"
            class="tip-link close-btn"
            @click="close()"
          >
            ✕
          </el-link>
        </div>
      </div>

      <!-- 编辑器区域 -->
      <div class="markdown-container">
        <div v-if="loading" class="loading-mask">加载中...</div>
        <MdEditor
          v-else
          ref="editorRef"
          v-model="markdownText"
          :language="'zh-CN'"
          :theme="'light'"
          :preview-theme="'default'"
          :toolbars="toolbars"
          :toolbars-exclude="(editable ? [] : ['save']) as ToolbarNames[]"
          :read-only="!editable"
          :preview="true"
          :page-fullscreen="false"
          :show-code-row-number="true"
          :no-prettier="true"
          :no-upload-img="true"
          :scroll-auto="true"
          :on-save="onEditorSave"
          class="md-editor-instance"
        />
      </div>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
.markdown-preview-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.8);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.tip-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2;
  color: #fff;
}

.tip-left {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
}

.tip-center {
  flex: 1;
  text-align: center;
}

.tip-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.file-name {
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.un-save {
  color: #E6A23C;
  font-size: 14px;
}

.editor-title {
  font-size: 16px;
  color: #fff;
}

.tip-link {
  color: #fff !important;
  &:hover { opacity: 0.8; }
}

.tip-help-link {
  .help-text {
    margin-right: 4px;
    font-size: 13px;
  }
}

.close-btn { font-size: 22px; }

.markdown-container {
  position: relative;
  margin: 56px auto 0;
  width: 90vw;
  height: calc(100vh - 80px);
  z-index: 1;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;

  :deep(.md-editor) {
    height: 100%;
    border: none;
  }

  :deep(.md-editor .md-editor-content) {
    height: calc(100% - 40px);
  }
}

.loading-mask {
  color: #303133;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
