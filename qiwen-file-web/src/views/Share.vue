<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listShares, cancelShare } from '_api/share'
import { ElMessage, ElMessageBox } from 'element-plus'

interface ShareItem {
  id: string
  shareCode: string
  filePath: string
  expireTime: string
  createTime: string
  link: string
}

const shares = ref<ShareItem[]>([])
const loading = ref(false)

async function fetchShares() {
  loading.value = true
  const res: any = await listShares()
  if (res.success) {
    shares.value = res.data || []
  }
  loading.value = false
}

function copyLink(link: string) {
  navigator.clipboard.writeText(link)
  ElMessage.success('链接已复制')
}

async function handleCancel(share: ShareItem) {
  await ElMessageBox.confirm('确定取消此分享吗？', '确认取消', { type: 'warning' })
  const res: any = await cancelShare(share.id)
  if (res.success) {
    ElMessage.success('已取消分享')
    fetchShares()
  }
}

function isExpired(time: string): boolean {
  return new Date(time) < new Date()
}

onMounted(fetchShares)
</script>

<template>
  <div class="share-page">
    <h2>我的分享</h2>
    <el-table :data="shares" v-loading="loading" style="width:100%">
      <el-table-column prop="filePath" label="文件路径" min-width="200" />
      <el-table-column prop="shareCode" label="提取码" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }: any">
          <el-tag :type="isExpired(row.expireTime) ? 'danger' : 'success'" size="small">
            {{ isExpired(row.expireTime) ? '已过期' : '有效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" width="180" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }: any">
          <el-button link type="primary" size="small" @click="copyLink(row.link)">复制链接</el-button>
          <el-button link type="danger" size="small" @click="handleCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style lang="stylus" scoped>
.share-page
  padding: 20px
  h2
    margin-bottom: 16px
</style>
