<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const filePath = ref('')
const expireDays = ref(7)
const code = ref('')
const shareLink = ref('')
const showResult = ref(false)

const emit = defineEmits<{
  confirm: [data: { filePath: string; expireDays: number; code: string }]
}>()

function open(path: string) {
  filePath.value = path
  expireDays.value = 7
  code.value = ''
  showResult.value = false
  visible.value = true
}

function handleCreate() {
  emit('confirm', {
    filePath: filePath.value,
    expireDays: expireDays.value,
    code: code.value || ''
  })
}

function setResult(link: string) {
  shareLink.value = link
  showResult.value = true
}

function copyLink() {
  navigator.clipboard.writeText(shareLink.value)
  ElMessage.success('链接已复制')
}

defineExpose({ open, setResult })
</script>

<template>
  <el-dialog v-model="visible" title="创建分享" width="480px">
    <div v-if="!showResult">
      <el-form label-position="top">
        <el-form-item label="文件">
          <el-input :model-value="filePath" disabled />
        </el-form-item>
        <el-form-item label="过期天数">
          <el-select v-model="expireDays" style="width:100%">
            <el-option :value="1" label="1 天" />
            <el-option :value="7" label="7 天" />
            <el-option :value="30" label="30 天" />
            <el-option :value="90" label="90 天" />
          </el-select>
        </el-form-item>
        <el-form-item label="提取码（留空自动生成）">
          <el-input v-model="code" placeholder="4位数字" maxlength="4" />
        </el-form-item>
      </el-form>
    </div>
    <div v-else class="share-result">
      <p>分享链接已生成：</p>
      <el-input v-model="shareLink" readonly>
        <template #append>
          <el-button @click="copyLink">复制</el-button>
        </template>
      </el-input>
    </div>
    <template #footer>
      <el-button v-if="!showResult" @click="visible = false">取消</el-button>
      <el-button v-if="!showResult" type="primary" @click="handleCreate">创建分享</el-button>
      <el-button v-else type="primary" @click="visible = false">完成</el-button>
    </template>
  </el-dialog>
</template>

<style lang="stylus" scoped>
.share-result
  text-align: center
  p
    margin-bottom: 12px
    color: #303133
</style>
