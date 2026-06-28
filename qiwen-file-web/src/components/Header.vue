<template>
	<div class="header-wrapper">
		<img class="logo" :src="logoUrl" @click="$router.push({ name: 'Home' })" />
		<img
			class="logo-xs"
			:src="logoUrlXs"
			@click="$router.push({ name: 'Home' })"
		/>
		<el-menu
			:default-active="activeIndex"
			class="top-menu-list"
			mode="horizontal"
			@select="menuItemClick"
		>
			<el-menu-item index="Home">首页</el-menu-item>
			<el-menu-item index="File">网盘</el-menu-item>
			<template v-if="isLogin">
				<el-submenu
					class="user-exit-submenu"
					index="User"
					v-if="screenWidth <= 768"
				>
					<template slot="title">
						<i class="el-icon-user-solid"></i>
						<span>{{ username }}</span>
					</template>
					<el-menu-item @click="passwordDialogVisible = true">修改密码</el-menu-item>
					<el-menu-item @click="exitButton()">退出</el-menu-item>
				</el-submenu>
				<template v-else>
					<!-- 为了和其他菜单样式保持一致，请一定要添加类名 el-menu-item -->
					<div class="el-menu-item exit" @click="exitButton()">退出</div>
					<div class="el-menu-item username" v-show="isLogin">
						<el-dropdown trigger="hover" @command="handleUserCommand">
							<span class="el-dropdown-link">
								<i class="el-icon-user-solid"></i> <span>{{ username }}</span>
								<i class="el-icon-arrow-down el-icon--right"></i>
							</span>
							<el-dropdown-menu slot="dropdown">
								<el-dropdown-item command="updatePassword">修改密码</el-dropdown-item>
							</el-dropdown-menu>
						</el-dropdown>
					</div>
				</template>
			</template>

			<!-- 修改密码对话框 -->
			<el-dialog
				title="修改密码"
				:visible.sync="passwordDialogVisible"
				width="400px"
				:append-to-body="true"
				@closed="resetPasswordForm"
			>
				<el-form
					:model="passwordForm"
					:rules="passwordRules"
					ref="passwordForm"
					label-width="100px"
				>
					<el-form-item label="旧密码" prop="oldPassword">
						<el-input
							v-model="passwordForm.oldPassword"
							type="password"
							show-password
							placeholder="请输入旧密码"
						></el-input>
					</el-form-item>
					<el-form-item label="新密码" prop="newPassword">
						<el-input
							v-model="passwordForm.newPassword"
							type="password"
							show-password
							placeholder="6-20位，不允许中文"
						></el-input>
					</el-form-item>
					<el-form-item label="确认新密码" prop="confirmPassword">
						<el-input
							v-model="passwordForm.confirmPassword"
							type="password"
							show-password
							placeholder="请再次输入新密码"
						></el-input>
					</el-form-item>
				</el-form>
				<div slot="footer" class="dialog-footer">
					<el-button @click="passwordDialogVisible = false">取 消</el-button>
					<el-button
						type="primary"
						:loading="passwordLoading"
						@click="submitPasswordUpdate"
						>确 定</el-button
					>
				</div>
			</el-dialog>

			<!-- 开发环境 -->
			<el-menu-item class="login" index="Login" v-show="!isLogin"
				>登录</el-menu-item
			>

			<!-- 开发环境 -->
			<el-menu-item class="register" v-show="!isLogin" index="Register"
				>注册</el-menu-item
			>
		</el-menu>
	</div>
</template>

<script>
import { mapGetters } from 'vuex'
import { updatePassword } from '_r/user'

export default {
	name: 'Header',
	data() {
		const validateConfirmPassword = (rule, value, callback) => {
			if (value !== this.passwordForm.newPassword) {
				callback(new Error('两次输入的新密码不一致'))
			} else {
				callback()
			}
		}
		return {
			logoUrl: require('_a/images/common/logo_header.png'),
			logoUrlXs: require('_a/images/common/logo_header_xs.png'),
			passwordDialogVisible: false,
			passwordLoading: false,
			passwordForm: {
				oldPassword: '',
				newPassword: '',
				confirmPassword: ''
			},
			passwordRules: {
				oldPassword: [
					{ required: true, message: '请输入旧密码', trigger: 'blur' }
				],
				newPassword: [
					{ required: true, message: '请输入新密码', trigger: 'blur' },
					{ min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
					{
						pattern: /^[^\u4e00-\u9fa5]{6,20}$/,
						message: '不允许中文',
						trigger: 'blur'
					}
				],
				confirmPassword: [
					{ required: true, message: '请再次输入新密码', trigger: 'blur' },
					{ validator: validateConfirmPassword, trigger: 'blur' }
				]
			}
		}
	},
	computed: {
		...mapGetters(['isLogin', 'username']),
		// 当前激活菜单的 index
		activeIndex() {
			return this.$route.name || 'Home' //  获取当前路由名称
		},
		isProductEnv() {
			return (
				process.env.NODE_ENV !== 'development' &&
				location.origin === 'https://pan.qiwenshare.com'
			)
		},
		// 屏幕宽度
		screenWidth() {
			return this.$store.state.common.screenWidth
		}
	},
	methods: {
		// 奇文社区生产环境账户网址
		getAccountHref(path) {
			return `https://account.qiwenshare.com${path}?Rurl=${location.href}`
		},
		/**
		 * 退出登录
		 * @description 清除 cookie 存放的 token  并跳转到登录页面
		 */
		exitButton() {
			this.$message.success('退出登录成功！')
			this.$common.removeCookies(this.$config.tokenKeyName)
			this.$store.dispatch('getUserInfo').then(() => {
				this.$router.push({ name: 'Home' })
			})
		},

		handleUserCommand(command) {
			if (command === 'updatePassword') {
				this.passwordDialogVisible = true
			}
		},

		resetPasswordForm() {
			this.$refs.passwordForm.resetFields()
		},

		submitPasswordUpdate() {
			this.$refs.passwordForm.validate((valid) => {
				if (valid) {
					this.passwordLoading = true
					updatePassword({
						oldPassword: this.passwordForm.oldPassword,
						newPassword: this.passwordForm.newPassword
					})
						.then((res) => {
							this.passwordLoading = false
							if (res.success) {
								this.$message.success('密码修改成功')
								this.passwordDialogVisible = false
							} else {
								this.$message.error(res.message || '密码修改失败')
							}
						})
						.catch(() => {
							this.passwordLoading = false
						})
				}
			})
		},

		menuItemClick(key) {
			if (key === 'exit') {
				this.exitButton()
			} else if (key === 'Login') {
				if (this.isProductEnv) {
					location.href = this.getAccountHref('/login/account')
				} else {
					this.$router.push({ name: key })
				}
			} else if (key === 'Register') {
				if (this.isProductEnv) {
					location.href = this.getAccountHref('/register')
				} else {
					this.$router.push({ name: key })
				}
			} else if (key === 'File') {
				this.$router.push({ name: key, query: { fileType: 0, filePath: '/' } })
			} else {
				this.$router.push({ name: key })
			}
		}
	}
}
</script>

<style lang="stylus" scoped>
@import '~_a/styles/varibles.styl';

.header-wrapper {
  width: 100%;
  padding: 0 20px;
  box-shadow: $tabBoxShadow;
  display: flex;

  .logo {
    margin: 14px 24px 0 24px;
    display: inline-block;
    height: 40px;
    cursor: pointer;
  }

  .logo-xs {
    display: none;
  }

  >>> .el-menu--horizontal {
    .el-menu-item:not(.is-disabled):hover {
      border-bottom-color: $Primary !important;
      background: $tabBackColor;
    }

  }

  .top-menu-list {
    flex: 1;

    .login, .register, .username, .exit, .user-exit-submenu {
      float: right;
    }

    .username {
      .el-dropdown-link {
        cursor: pointer;
        color: inherit;
        display: block;
        height: 100%;
      }
    }
  }
}
</style>
