<script setup lang="ts">
import { ref } from 'vue'
import http from '_api/http'
import { ElMessage, ElMessageBox } from 'element-plus'

const visible = ref(false)
const versions = ref<any[]>([])
const fileId = ref('')
const fileName = ref('')

async function open(fid: string, name: string) {
  fileId.value = fid; fileName.value = name; visible.value = true
  const res: any = await http.get(`/file/${fid}/versions`)
  if (res.success) versions.value = res.data || []
}

async function handleRestore(ver: any) {
  await ElMessageBox.confirm(
    `确定回滚到版本 ${ver.version} (${ver.fileName}) 吗？当前内容将被存档。`,
    '确认回滚', { type: 'warning' }
  )
  const res: any = await http.post(`/file/${fileId.value}/restore/${ver.id}`)
  if (res.success) { ElMessage.success('已回滚'); visible.value = false }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" :title="`版本历史 — ${fileName}`" width="700px" destroy-on-close>
    <el-table :data="versions" max-height="400">
      <el-table-column label="版本" width="80">
        <template #default="{ row }"><el-tag size="small">v{{ row.version }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ row.fileSize }} B</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row, $index }">
          <el-button v-if="$index > 0" link type="primary" size="small" @click="handleRestore(row)">回滚</el-button>
          <el-tag v-else size="small" type="info">当前</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>
