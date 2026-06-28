<template>
	<div class="admin-user-list-wrapper">
		<!-- 1. 搜索区域 -->
		<div class="top-search">
			<div class="search-input-group">
				<el-input
					v-model="searchForm.username"
					placeholder="用户名"
					class="search-item"
					clearable
					@keyup.enter.native="getUserList"
				></el-input>
				<el-input
					v-model="searchForm.telephone"
					placeholder="手机号"
					class="search-item"
					clearable
					@keyup.enter.native="getUserList"
				></el-input>
			</div>
			<el-button
				type="primary"
				class="search-btn"
				icon="el-icon-search"
				@click="getUserList"
				>搜索</el-button
			>
		</div>

		<!-- 2. 表格区域 -->
		<div class="table-container">
			<el-table :data="userList" border class="user-table">
				<el-table-column prop="username" label="用户名" min-width="120"></el-table-column>
				<el-table-column prop="telephone" label="手机号" min-width="120"></el-table-column>
				<el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip></el-table-column>
				<el-table-column label="存储情况" min-width="200">
					<template slot-scope="scope">
						<el-progress
							:percentage="calculatePercentage(scope.row)"
							:color="storageColor"
						></el-progress>
						<div class="storage-text">
							{{ $file.calculateFileSize(scope.row.storageSize || 0) }} /
							{{ $file.calculateFileSize((scope.row.totalStorageSize || 0) * 1024 * 1024) }}
						</div>
					</template>
				</el-table-column>
				<el-table-column prop="registerTime" label="注册时间" width="180"></el-table-column>
				<el-table-column label="状态" width="100" align="center">
					<template slot-scope="scope">
						<el-tag :type="scope.row.available === 1 ? 'success' : 'danger'">
							{{ scope.row.available === 1 ? '启用' : '禁用' }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="操作" width="220" fixed="right" align="center">
					<template slot-scope="scope">
						<el-button type="text" size="small" @click="handleViewDetail(scope.row)"
							>详情</el-button
						>
						<el-button type="text" size="small" @click="handleEditStorage(scope.row)"
							>修改配额</el-button
						>
						<el-button type="text" size="small" @click="handleResetPassword(scope.row)"
							>重置密码</el-button
						>
						<el-button
							type="text"
							size="small"
							:style="{ color: scope.row.available === 1 ? '#F56C6C' : '#67C23A' }"
							@click="handleToggleAvailable(scope.row)"
						>
							{{ scope.row.available === 1 ? '禁用' : '启用' }}
						</el-button>
					</template>
				</el-table-column>
			</el-table>
		</div>

		<!-- 3. 分页区域 -->
		<div class="pagination-wrapper">
			<el-pagination
				@size-change="handleSizeChange"
				@current-change="handleCurrentChange"
				:current-page="searchForm.currentPage"
				:page-sizes="[10, 20, 50, 100]"
				:page-size="searchForm.pageCount"
				layout="total, sizes, prev, pager, next, jumper"
				:total="total"
			>
			</el-pagination>
		</div>

		<!-- 修改存储配额对话框 -->
		<el-dialog title="修改存储配额" :visible.sync="storageDialogVisible" width="30%">
			<el-form :model="storageForm" label-width="100px">
				<el-form-item label="总配额(M)">
					<el-input-number
						v-model="storageForm.totalStorageSize"
						:min="1"
						label="存储大小"
					></el-input-number>
				</el-form-item>
			</el-form>
			<span slot="footer" class="dialog-footer">
				<el-button @click="storageDialogVisible = false">取 消</el-button>
				<el-button type="primary" @click="submitStorageUpdate">确 定</el-button>
			</span>
		</el-dialog>

		<!-- 用户详情对话框 -->
		<el-dialog title="用户详情" :visible.sync="detailDialogVisible" width="40%">
			<el-form label-width="100px" label-position="left">
				<el-form-item label="用户ID">{{ detailInfo.userId }}</el-form-item>
				<el-form-item label="用户名">{{ detailInfo.username }}</el-form-item>
				<el-form-item label="手机号">{{ detailInfo.telephone }}</el-form-item>
				<el-form-item label="邮箱">{{ detailInfo.email }}</el-form-item>
				<el-form-item label="注册时间">{{ detailInfo.registerTime }}</el-form-item>
				<el-form-item label="当前存储">
					{{ $file.calculateFileSize(detailInfo.storageSize || 0) }}
				</el-form-item>
				<el-form-item label="总配额">
					{{ $file.calculateFileSize((detailInfo.totalStorageSize || 0) * 1024 * 1024) }}
				</el-form-item>
				<el-form-item label="使用比例">
					<el-progress :percentage="calculatePercentage(detailInfo)"></el-progress>
				</el-form-item>
			</el-form>
			<span slot="footer" class="dialog-footer">
				<el-button @click="detailDialogVisible = false">关 闭</el-button>
			</span>
		</el-dialog>

		<!-- 重置密码对话框 -->
		<el-dialog title="重置密码" :visible.sync="resetPasswordDialogVisible" width="400px">
			<el-form :model="resetPasswordForm" :rules="resetPasswordRules" ref="resetPasswordForm" label-width="100px">
				<el-form-item label="用户名">{{ resetPasswordInfo.username }}</el-form-item>
				<el-form-item label="新密码" prop="password">
					<el-input
						v-model="resetPasswordForm.password"
						type="password"
						show-password
						placeholder="6-20位，不允许中文"
					></el-input>
				</el-form-item>
			</el-form>
			<span slot="footer" class="dialog-footer">
				<el-button @click="resetPasswordDialogVisible = false">取 消</el-button>
				<el-button type="primary" :loading="resetPasswordLoading" @click="submitResetPassword">确 定</el-button>
			</span>
		</el-dialog>
	</div>
</template>

<script>
import { getUserList, updateUserAvailable, updateUserStorage, resetPassword } from '_r/admin'

export default {
	name: 'AdminUserList',
	data() {
		return {
			searchForm: {
				currentPage: 1,
				pageCount: 10,
				username: '',
				telephone: ''
			},
			userList: [],
			total: 0,
			storageDialogVisible: false,
			detailDialogVisible: false,
			resetPasswordDialogVisible: false,
			resetPasswordLoading: false,
			detailInfo: {},
			resetPasswordInfo: {},
			storageForm: {
				userId: '',
				totalStorageSize: 0
			},
			resetPasswordForm: {
				userId: '',
				password: ''
			},
			resetPasswordRules: {
				password: [
					{ required: true, message: '请输入新密码', trigger: 'blur' },
					{ min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
					{
						pattern: /^[^\u4e00-\u9fa5]{6,20}$/,
						message: '不允许中文',
						trigger: 'blur'
					}
				]
			},
			storageColor: [
				{ color: '#67C23A', percentage: 50 },
				{ color: '#E6A23C', percentage: 80 },
				{ color: '#F56C6C', percentage: 100 }
			]
		}
	},
	created() {
		this.getUserList()
	},
	methods: {
		getUserList() {
			getUserList(this.searchForm).then((res) => {
				if (res.success) {
					this.userList = res.data.records
					this.total = res.data.total
				}
			})
		},
		handleViewDetail(row) {
			this.detailInfo = row
			this.detailDialogVisible = true
		},
		calculatePercentage(row) {
			const total = (row.totalStorageSize || 0) * 1024 * 1024
			const used = row.storageSize || 0
			if (total === 0) return 0
			const res = Math.round((used / total) * 100)
			return res > 100 ? 100 : res
		},
		handleSizeChange(val) {
			this.searchForm.pageCount = val
			this.getUserList()
		},
		handleCurrentChange(val) {
			this.searchForm.currentPage = val
			this.getUserList()
		},
		handleToggleAvailable(row) {
			const newAvailable = row.available === 1 ? 0 : 1
			const actionText = newAvailable === 1 ? '启用' : '禁用'
			this.$confirm(`确定要${actionText}该用户吗？`, '提示', {
				type: 'warning'
			}).then(() => {
				updateUserAvailable({ userId: row.userId, available: newAvailable }).then(
					(res) => {
						if (res.success) {
							this.$message.success('操作成功')
							this.getUserList()
						}
					}
				)
			})
		},
		handleEditStorage(row) {
			this.storageForm.userId = row.userId
			this.storageForm.totalStorageSize = row.totalStorageSize || 0
			this.storageDialogVisible = true
		},
		submitStorageUpdate() {
			updateUserStorage(this.storageForm).then((res) => {
				if (res.success) {
					this.$message.success('修改成功')
					this.storageDialogVisible = false
					this.getUserList()
				}
			})
		},
		handleResetPassword(row) {
			this.resetPasswordInfo = row
			this.resetPasswordForm.userId = row.userId
			this.resetPasswordForm.password = '123456'
			this.resetPasswordDialogVisible = true
			this.$nextTick(() => {
				this.$refs.resetPasswordForm.clearValidate()
			})
		},
		submitResetPassword() {
			this.$refs.resetPasswordForm.validate((valid) => {
				if (valid) {
					this.resetPasswordLoading = true
					resetPassword(this.resetPasswordForm)
						.then((res) => {
							this.resetPasswordLoading = false
							if (res.success) {
								this.$message.success('重置密码成功')
								this.resetPasswordDialogVisible = false
							} else {
								this.$message.error(res.message || '重置密码失败')
							}
						})
						.catch(() => {
							this.resetPasswordLoading = false
						})
				}
			})
		}
	}
}
</script>

<style lang="stylus" scoped>
.admin-user-list-wrapper {
	padding: 24px;
	background: #fff;
	min-height: calc(100vh - 189px);
	display: flex;
	flex-direction: column;

	// 顶部搜索区域
	.top-search {
		display: flex;
		flex-wrap: wrap;
		gap: 16px;
		margin-bottom: 24px;
		padding-bottom: 20px;
		border-bottom: 1px solid #EBEEF5;

		.search-input-group {
			display: flex;
			gap: 12px;
			flex-wrap: wrap;

			.search-item {
				width: 220px;
			}
		}

		.search-btn {
			padding: 10px 24px;
		}
	}

	// 中间表格区域
	.table-container {
		flex: 1;
		width: 100%;
		overflow: hidden; // 重要：防止外部容器被撑开
		
		.user-table {
			width: 100% !important;
			
			.storage-text {
				font-size: 12px;
				margin-top: 5px;
				color: #909399;
			}
		}
	}

	// 底部分页区域
	.pagination-wrapper {
		margin-top: 24px;
		padding-top: 16px;
		border-top: 1px solid #EBEEF5;
		display: flex;
		justify-content: center;
	}
}

// 响应式处理
@media screen and (max-width: 768px) {
	.admin-user-list-wrapper {
		padding: 12px;

		.top-search {
			flex-direction: column;
			align-items: stretch;
			gap: 12px;

			.search-input-group {
				flex-direction: column;
				.search-item {
					width: 100%;
				}
			}

			.search-btn {
				width: 100%;
			}
		}

		.table-container {
			.user-table {
				// 移动端保持表格最小宽度，支持横向滚动
				min-width: 900px;
			}
		}
	}
}
</style>
