<script setup lang="ts">
import { computed, ref, watch, inject, nextTick } from 'vue'
import { Download, InfoFilled, Promotion } from '@element-plus/icons-vue'
import { CODE_PREVIEW_KEY } from '@/composables/previewKeys'
import { EditorView } from '@codemirror/view'
import { basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import type { Extension } from '@codemirror/state'
import { oneDark } from '@codemirror/theme-one-dark'
import { dracula } from '@uiw/codemirror-theme-dracula'
import { vscodeDark } from '@uiw/codemirror-theme-vscode'
import { okaidia } from '@uiw/codemirror-theme-okaidia'
import { monokai } from '@uiw/codemirror-theme-monokai'
import { nord } from '@uiw/codemirror-theme-nord'
import { copilot } from '@uiw/codemirror-theme-copilot'
import { kimbie } from '@uiw/codemirror-theme-kimbie'
import { andromeda } from '@uiw/codemirror-theme-andromeda'
import { bbedit } from '@uiw/codemirror-theme-bbedit'
import { red } from '@uiw/codemirror-theme-red'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { java } from '@codemirror/lang-java'
import { html } from '@codemirror/lang-html'
import { css } from '@codemirror/lang-css'
import { json } from '@codemirror/lang-json'
import { xml } from '@codemirror/lang-xml'
import { markdown } from '@codemirror/lang-markdown'
import { sql } from '@codemirror/lang-sql'
import { rust } from '@codemirror/lang-rust'
import { php } from '@codemirror/lang-php'
import { cpp } from '@codemirror/lang-cpp'

const {
  visible,
  file,
  codeContent,
  loading,
  lineWrapping,
  languageMode,
  theme,
  fontSize,
  editable,
  isModified,
  setTheme,
  setLanguageMode,
  setFontSize,
  toggleLineWrapping,
  open,
  openEdit,
  save,
  close,
} = inject(CODE_PREVIEW_KEY)!

defineExpose({ open, openEdit })

const editorContainer = ref<HTMLElement | null>(null)
const editorView = ref<EditorView | null>(null)

/** 记录上次构建编辑器时的结构化配置指纹，用于判断是否需要完全重建 */
let lastBuildKey = ''

/** 防抖标记：避免同一帧内多次重建 */
let pendingRebuild = false

/**
 * 编辑器自身输入标记。
 * 当 updateListener 把文档内容同步到 codeContent 时置 true，
 * 让 watch(codeContent) 跳过重建，避免光标位置被 dispatch 重置。
 */
let isEditorUpdate = false

/**
 * 防抖重建编辑器。
 * 同一帧内多次调用只执行最后一次。
 * 仅在 overlay 可见时才重建。
 */
function scheduleRebuild(): void {
  if (pendingRebuild || !visible.value) return
  pendingRebuild = true
  nextTick(() => {
    pendingRebuild = false
    buildEditor()
  })
}

/** 计算结构化配置指纹（不含内容） */
function getBuildKey(): string {
  return [
    languageMode.value,
    theme.value,
    fontSize.value,
    lineWrapping.value ? 'wrap' : 'nowrap',
    editable.value ? 'edit' : 'readonly',
  ].join('|')
}

/** 创建或重建编辑器 */
async function buildEditor(): Promise<void> {
  if (!editorContainer.value) return

  const newKey = getBuildKey()

  // 如果结构化配置没变，只更新文档内容（不销毁重建）
  if (editorView.value && newKey === lastBuildKey) {
    const currentDoc = editorView.value.state.doc.toString()
    if (currentDoc !== codeContent.value) {
      const view = editorView.value
      view.dispatch({
        changes: {
          from: 0,
          to: view.state.doc.length,
          insert: codeContent.value,
        },
      })
    }
    return
  }

  // 结构化配置变了，需要完全重建
  if (editorView.value) {
    editorView.value.destroy()
    editorView.value = null
    editorContainer.value.innerHTML = ''
  }

  // 等待 DOM 更新和容器尺寸就绪
  await nextTick()
  await new Promise<void>(resolve => requestAnimationFrame(() => resolve()))

  if (!editorContainer.value) return

  const langExt = await getLanguageExtension(languageMode.value)
  const extensions: Extension[] = [
    basicSetup,
    langExt,
    theme.value !== 'default' ? themeMap[theme.value] || [] : [],
    fontSizeTheme.value,
  ]

  if (editable.value) {
    extensions.push(EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        isEditorUpdate = true
        codeContent.value = update.state.doc.toString()
      }
    }))
  } else {
    extensions.push(EditorState.readOnly.of(true))
    extensions.push(EditorView.editable.of(false))
  }

  if (lineWrapping.value) {
    extensions.push(EditorView.lineWrapping)
  }

  const state = EditorState.create({
    doc: codeContent.value,
    extensions,
  })

  editorView.value = new EditorView({
    state,
    parent: editorContainer.value,
  })

  lastBuildKey = newKey
}

/** 语言扩展映射 */
function getLanguageExtension(lang: string): Extension | Extension[] {
  if (!lang) return []
  const map: Record<string, () => any> = {
    javascript, js: javascript,
    typescript: () => import('@codemirror/lang-javascript').then(m => m.javascript({ typescript: true })),
    python, py: python,
    java, html, css, json, xml, markdown: () => markdown(),
    sql, rust, php, cpp,
    csharp: () => import('@codemirror/lang-cpp').then(m => m.cpp()),
    go: () => import('@codemirror/lang-go').then(m => m.go()),
    kotlin: () => import('@codemirror/lang-java').then(m => m.java()),
    swift: () => import('@codemirror/lang-javascript').then(m => m.javascript()),
    vue: html,
    yaml: () => import('@codemirror/lang-yaml').then(m => m.yaml()),
    shell: () => import('@codemirror/lang-javascript').then(m => m.javascript()),
    ruby: () => import('@codemirror/lang-python').then(m => m.python()),
  }
  return map[lang]?.() || javascript()
}

/** 主题映射 */
const themeMap: Record<string, Extension> = {
  oneDark,
  dracula,
  vscodeDark,
  okaidia,
  monokai,
  nord,
  copilot,
  kimbie: kimbie,
  andromeda,
  bbedit,
  red,
}

const availableThemes = [
  { label: 'Default (Light)', value: 'default' },
  { label: 'One Dark', value: 'oneDark' },
  { label: 'Dracula', value: 'dracula' },
  { label: 'VS Code Dark', value: 'vscodeDark' },
  { label: 'Okaidia', value: 'okaidia' },
  { label: 'Monokai', value: 'monokai' },
  { label: 'Nord', value: 'nord' },
  { label: 'Copilot', value: 'copilot' },
  { label: 'Kimbie Dark', value: 'kimbie' },
  { label: 'Andromeda', value: 'andromeda' },
  { label: 'BBEdit', value: 'bbedit' },
  { label: 'Red', value: 'red' },
]
const availableLanguages = [
  'javascript', 'typescript', 'python', 'java', 'go', 'rust', 'cpp', 'csharp',
  'php', 'swift', 'kotlin', 'html', 'css', 'json', 'xml', 'yaml', 'sql',
  'markdown', 'shell', 'ruby', 'vue', 'scss', 'less',
]

/** 字号选项 */
const fontSizeOptions = [12, 14, 16, 18, 20, 22, 24, 26, 28, 30]

/** 动态字号主题 */
const fontSizeTheme = computed(() =>
  EditorView.theme({
    '&.cm-editor .cm-content': {
      fontSize: `${fontSize.value}px`,
    },
  }),
)

function onDownload(): void {
  if (file.value) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    window.open(`${baseUrl}/filetransfer/download/${file.value.userFileId}`, '_blank')
  }
}

// 用 scheduleRebuild 防抖，避免同一帧内多次重建
watch(codeContent, () => {
  if (isEditorUpdate) {
    isEditorUpdate = false
    return
  }
  scheduleRebuild()
})
watch(languageMode, () => scheduleRebuild())
watch(lineWrapping, () => scheduleRebuild())
watch(theme, () => scheduleRebuild())
watch(fontSize, () => scheduleRebuild())
watch(editable, () => scheduleRebuild())
</script>

<template>
  <Teleport to="body">
    <div v-show="visible" class="code-preview-overlay">
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
          <el-tooltip effect="dark" placement="bottom">
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

      <!-- 工具栏 -->
      <div class="operate-wrapper" @click.stop>
        <i
          v-if="editable && isModified"
          class="save-icon"
          title="保存（ctrl+s）"
          @click="save()"
        >
          <el-icon :size="20"><Promotion /></el-icon>
        </i>

        <el-checkbox :model-value="lineWrapping" @change="toggleLineWrapping" class="tool-item">
          自动换行
        </el-checkbox>

        <div class="tool-item">
          <span class="tool-label">字号</span>
          <el-select :model-value="fontSize" size="small" style="width: 96px" :teleported="false" @update:model-value="(v: number) => setFontSize(v)">
            <el-option v-for="s in fontSizeOptions" :key="s" :label="`${s}px`" :value="s" />
          </el-select>
        </div>

        <div class="tool-item">
          <span class="tool-label">语言</span>
          <el-select :model-value="languageMode" size="small" style="width: 120px" :teleported="false" @update:model-value="(v: string) => setLanguageMode(v)">
            <el-option
              v-for="lang in availableLanguages"
              :key="lang"
              :label="lang"
              :value="lang"
            />
          </el-select>
        </div>

        <div class="tool-item">
          <span class="tool-label">主题</span>
          <el-select :model-value="theme" size="small" style="width: 190px" :teleported="false" @update:model-value="(v: string) => setTheme(v)">
            <el-option
              v-for="t in availableThemes"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </div>
      </div>

      <!-- 编辑器区域 -->
      <div class="code-editor-wrapper" @click.stop>
        <div v-if="loading" class="loading-mask">加载中...</div>
        <div ref="editorContainer" class="codemirror-container" />
      </div>
    </div>
  </Teleport>
</template>

<style lang="scss" scoped>
.code-preview-overlay {
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

.operate-wrapper {
  position: absolute;
  top: 48px;
  left: 0;
  right: 0;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  margin: 0 auto;
  width: 90vw;
  background: #fff;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #DCDFE6;
  z-index: 5;
}

.save-icon {
  font-size: 20px;
  cursor: pointer;
  color: #409EFF;
  font-weight: 550;
  display: flex;
  align-items: center;
  &:hover { opacity: 0.5; }
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #303133;
}

.tool-label { white-space: nowrap; }

.code-editor-wrapper {
  position: relative;
  margin: 56px auto 0;
  width: 90vw;
  height: calc(100vh - 80px);
  z-index: 1;
  background: #fff;
  border-radius: 0 0 8px 8px;
  display: flex;
  flex-direction: column;
}

.loading-mask {
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.codemirror-container {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #fff;
  border-radius: 0 0 8px 8px;

  &::-webkit-scrollbar { width: 12px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: #C0C4CC; border-radius: 2em; }

  :deep(.cm-editor) {
    height: 100%;
    font-family: SFMono-Regular, Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  }
}
</style>
