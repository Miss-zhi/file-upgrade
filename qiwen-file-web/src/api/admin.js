import http from './http'

export async function getUserList(params) {
  return http.post('/admin/user/list', params)
}

export async function updateUser(data) {
  return http.put('/admin/user', data)
}

export async function toggleUserStatus(id, enabled) {
  return http.put(`/admin/user/${id}/status`, null, { params: { enabled } })
}

export async function deleteUser(id) {
  return http.delete(`/admin/user/${id}`)
}
