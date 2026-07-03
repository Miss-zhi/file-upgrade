<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import type { ShareInfo, FileInfo } from '@/types/file'
import { getShareInfo, verifyShare, downloadShareFile } from '@/api/file'
import { formatFileSize } from '@/utils/file'
import { ElMessage } from 'element-plus'
import SaveShareDialog from '@/components/file/dialogs/SaveShareDialog.vue'

const route = useRoute()
const shareCode = route.params.shareBatchNum as string

// ---- 分享状态 ----

const shareInfo = ref<ShareInfo | null>(null)
const loading = ref(true)
const error = ref('')

// ---- 提取码 ----

const extractCode = ref('')
const isVerified = ref(false)
const needCode = ref(false)
const verifying = ref(false)

// ---- 保存弹窗 ----

const saveShareVisible = ref(false)

// ---- 获取分享信息 ----

async function fetchShareInfo(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const info = await getShareInfo(shareCode)
    shareInfo.value = info
    needCode.value = !!info.extractCode
    if (!needCode.value) {
      isVerified.value = true
    }
  } catch {
    error.value = '分享链接已失效'
  } finally {
    loading.value = false
  }
}

// ---- 验证提取码 ----

async function handleVerify(): Promise<void> {
  if (!extractCode.value) {
    ElMessage.warning('请输入提取码')
    return
  }
  verifying.value = true
  try {
    const info = await verifyShare({ shareCode, extractCode: extractCode.value })
    shareInfo.value = info
    isVerified.value = true
  } catch {
    ElMessage.error('提取码错误')
  } finally {
    verifying.value = false
  }
}

// ---- 下载 ----

function handleDownload(): void {
  downloadShareFile(shareCode)
}

onMounted(fetchShareInfo)
</script>

<template>
  <div class="share-view">
    <div class="share-container">
      <!-- 加载中 -->
      <div v-if="loading" class="share-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在加载分享信息...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" class="share-error">
        <el-icon :size="48" color="#f56c6c"><CircleCloseFilled /></el-icon>
        <p>{{ error }}</p>
      </div>

      <!-- 需要提取码 -->
      <template v-else-if="!isVerified && needCode">
        <h2 class="share-title">{{ shareInfo?.fileName || '文件分享' }}</h2>
        <p class="share-info">请输入提取码查看文件</p>
        <div class="extract-code-area">
          <el-input
            v-model="extractCode"
            placeholder="请输入提取码"
            clearable
            style="width: 240px"
            @keyup.enter="handleVerify"
          />
          <el-button type="primary" :loading="verifying" @click="handleVerify">提取文件</el-button>
        </div>
      </template>

      <!-- 分享文件信息 -->
      <template v-else-if="shareInfo">
        <h2 class="share-title">{{ shareInfo.fileName }}</h2>
        <div class="share-meta">
          <span>大小：{{ formatFileSize(shareInfo.fileSize) }}</span>
          <span v-if="shareInfo.expireTime">有效期至：{{ shareInfo.expireTime }}</span>
          <span v-else>永久有效</span>
          <span>浏览：{{ shareInfo.viewCount }} 次</span>
        </div>
        <div class="share-actions">
          <el-button type="primary" @click="handleDownload">
            <el-icon><Download /></el-icon>下载文件
          </el-button>
          <el-button @click="saveShareVisible = true">
            <el-icon><FolderAdd /></el-icon>保存到网盘
          </el-button>
        </div>
      </template>
    </div>

    <!-- 保存弹窗 -->
    <SaveShareDialog
      v-model:visible="saveShareVisible"
      :share-code="shareCode"
      :file-name="shareInfo?.fileName || ''"
    />
  </div>
</template>

<style lang="scss" scoped>
.share-view {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 20px;
  min-height: calc(100vh - $header-height);
}

.share-container {
  width: 100%;
  max-width: 800px;
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: $tab-box-shadow-min;
}

.share-title {
  font-size: 22px;
  color: $primary-text;
  margin-bottom: 8px;
}

.share-info {
  font-size: 13px;
  color: $secondary-text;
  margin-bottom: 24px;
}

.share-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: $secondary-text;
  margin-bottom: 24px;
}

.share-actions {
  display: flex;
  gap: 12px;
}

.extract-code-area {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  padding: 40px 0;
}

.share-loading,
.share-error {
  text-align: center;
  padding: 40px 0;

  p {
    margin-top: 12px;
    font-size: 14px;
    color: $secondary-text;
  }
}
</style>
