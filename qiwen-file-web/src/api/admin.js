import http from './http'

/** 获取用户列表 */
export async function getUserList(params) {
  return http.get('/admin/users', { params })
}

/** 修改用户状态 */
export async function updateUserStatus(data) {
  return http.put('/admin/user/status', data)
}
