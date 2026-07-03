<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { SuccessFilled } from '@element-plus/icons-vue'
import type { FileInfo, ShareInfo, ShareCreateDTO } from '@/types/file'
import { createShare } from '@/api/file'

const props = defineProps<{
  visible: boolean
  file: FileInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// ---- 表单状态 ----

const expireTime = ref<Date>()
const shareType = ref(1) // 1=需要提取码, 0=不需要
const extractCode = ref('')
const loading = ref(false)

// ---- 分享结果 ----

const shareResult = ref<ShareInfo | null>(null)
const stage = ref<'config' | 'result'>('config')

// ---- 日期选择器快捷选项 ----

const pickerShortcuts = [
  {
    text: '今天',
    value: () => {
      const d = new Date()
      d.setHours(23, 59, 59, 0)
      return d
    },
  },
  {
    text: '1天',
    value: () => {
      const d = new Date()
      d.setTime(d.getTime() + 86400 * 1000)
      return d
    },
  },
  {
    text: '7天',
    value: () => {
      const d = new Date()
      d.setTime(d.getTime() + 86400 * 1000 * 7)
      return d
    },
  },
  {
    text: '30天',
    value: () => {
      const d = new Date()
      d.setTime(d.getTime() + 86400 * 1000 * 30)
      return d
    },
  },
]

// ---- 计算属性 ----

const shareUrl = computed(() => {
  if (!shareResult.value) return ''
  return `${window.location.origin}/share/${shareResult.value.shareCode}`
})

const isExtractCodeRequired = computed(() => shareType.value === 1)

// ---- 方法 ----

/** 生成 4 位随机提取码（数字+字母） */
function generateRandomCode(): void {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  extractCode.value = code
}

/** 验证提取码格式（4-6 位字母数字） */
function isValidExtractCode(code: string): boolean {
  return /^[A-Za-z0-9]{4,6}$/.test(code)
}

/** 创建分享 */
async function handleCreateShare(): Promise<void> {
  if (!props.file) return

  // 校验过期时间
  if (!expireTime.value) {
    ElMessage.warning('请选择链接有效期')
    return
  }

  // 校验提取码
  if (isExtractCodeRequired.value) {
    if (!extractCode.value.trim()) {
      ElMessage.warning('请输入提取码或点击随机生成')
      return
    }
    if (!isValidExtractCode(extractCode.value.trim())) {
      ElMessage.warning('提取码须为 4-6 位字母或数字')
      return
    }
  }

  loading.value = true
  try {
    // 构造本地时间的 ISO-8601 格式，避免 toISOString() 转 UTC 导致时区偏移
    let isoExpireTime: string
    if (expireTime.value instanceof Date) {
      const d = expireTime.value
      const pad = (n: number): string => String(n).padStart(2, '0')
      isoExpireTime = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    } else {
      // 兼容字符串类型
      isoExpireTime = String(expireTime.value)
    }

    const dto: ShareCreateDTO = {
      userFileId: props.file.userFileId,
      expireTime: isoExpireTime,
      shareType: shareType.value,
      extractCode: isExtractCodeRequired.value ? extractCode.value.trim().toUpperCase() : undefined,
    }

    const result = await createShare(dto)
    if (result) {
      shareResult.value = result
      stage.value = 'result'
      emit('success')
    }
  } catch {
    ElMessage.error('创建分享失败')
  } finally {
    loading.value = false
  }
}

/** 复制链接及提取码 */
function handleCopyLink(): void {
  if (!shareResult.value) return
  const text = shareResult.value.extractCode
    ? `${shareUrl.value}\n提取码: ${shareResult.value.extractCode}`
    : shareUrl.value
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

/** 关闭弹窗 */
function handleClose(): void {
  emit('update:visible', false)
}

// ---- 生命周期 ----

watch(() => props.visible, (val) => {
  if (val) {
    // 默认 7 天后过期
    const defaultExpire = new Date()
    defaultExpire.setTime(defaultExpire.getTime() + 86400 * 1000 * 7)
    expireTime.value = defaultExpire
    shareType.value = 1
    extractCode.value = ''
    shareResult.value = null
    stage.value = 'config'
  }
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="分享文件"
    width="550px"
    @close="handleClose"
  >
    <!-- 配置阶段 -->
    <div v-if="stage === 'config'" class="share-config">
      <el-form label-width="120px" label-suffix="：">
        <el-form-item label="链接有效期至" required>
          <el-date-picker
            v-model="expireTime"
            type="datetime"
            placeholder="选择日期时间"
            align="right"
            format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="pickerShortcuts"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="是否需要提取码">
          <el-radio-group v-model="shareType">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="isExtractCodeRequired" label="提取码">
          <div class="extract-code-row">
            <el-input
              v-model="extractCode"
              placeholder="请输入提取码（4-6位字母或数字）"
              maxlength="6"
              style="flex: 1"
            />
            <el-button @click="generateRandomCode" style="margin-left: 8px">
              随机生成
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 结果阶段 -->
    <div v-else class="share-result">
      <div class="success-tip">
        <el-icon color="#67c23a" :size="20"><SuccessFilled /></el-icon>
        <span>成功创建分享链接</span>
      </div>
      <el-form label-width="90px" label-suffix="：">
        <el-form-item label="分享链接">
          <el-input :model-value="shareUrl" readonly type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </el-form-item>
        <el-form-item v-if="shareResult?.extractCode" label="提取码">
          <el-input :model-value="shareResult.extractCode" readonly />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <template v-if="stage === 'config'">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleCreateShare">
          确定
        </el-button>
      </template>
      <template v-else>
        <el-button type="primary" @click="handleCopyLink">复制链接及提取码</el-button>
        <el-button @click="handleClose">关闭</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<style scoped>
.share-config :deep(.el-form-item) {
  margin-bottom: 20px;
}

.extract-code-row {
  display: flex;
  align-items: center;
  width: 100%;
}

.share-result {
  .success-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 20px;
    font-size: 15px;
    color: #303133;
  }
}
</style>
