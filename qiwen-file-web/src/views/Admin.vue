<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, updateUser, toggleUserStatus, deleteUser } from '_api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserEditDialog from '_c/admin/UserEditDialog.vue'

interface UserItem {
  id: string
  username: string
  email: string
  phone: string
  nickname: string
  status: number
  createTime: string
}

const loading = ref(false)
const users = ref<UserItem[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const editRef = ref<InstanceType<typeof UserEditDialog>>()
const editingUser = ref<UserItem | null>(null)

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getUserList({ page: page.value, size: size.value, keyword: keyword.value })
    if (res.success && res.data) {
      users.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch { /* handled */ }
  loading.value = false
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function handlePageChange(p: number) {
  page.value = p
  fetchData()
}

function handleEdit(user: UserItem) {
  editingUser.value = user
  editRef.value?.open()
}

async function handleEditConfirm(data: { id: string; email: string; phone: string; nickname: string }) {
  const res: any = await updateUser(data)
  if (res.success) {
    ElMessage.success('更新成功')
    fetchData()
  }
}

async function handleToggleStatus(user: UserItem) {
  const enabled = user.status === 0
  const action = enabled ? '启用' : '禁用'
  await ElMessageBox.confirm(`确定${action}用户 "${user.username}" 吗？`, '确认操作')
  const res: any = await toggleUserStatus(user.id, enabled)
  if (res.success) {
    ElMessage.success(`${action}成功`)
    fetchData()
  }
}

async function handleDelete(user: UserItem) {
  await ElMessageBox.confirm(`确定删除用户 "${user.username}" 吗？`, '确认删除', { type: 'warning' })
  const res: any = await deleteUser(user.id)
  if (res.success) {
    ElMessage.success('删除成功')
    fetchData()
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="admin-page">
    <h2>用户管理</h2>

    <!-- 搜索 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名或邮箱"
        style="width: 300px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="users" v-loading="loading" style="width: 100%">
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }: any">
          <el-switch
            :model-value="row.status === 1"
            @change="handleToggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }: any">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <UserEditDialog ref="editRef" :user="editingUser as any" @confirm="handleEditConfirm" />
  </div>
</template>

<style lang="stylus" scoped>
.admin-page
  padding: 20px

  h2
    margin-bottom: 16px

  .search-bar
    display: flex
    gap: 8px
    margin-bottom: 16px

  .pagination-wrap
    margin-top: 16px
    display: flex
    justify-content: flex-end
</style>
