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

export async function updateUserRole(id, role) {
  return http.put(`/admin/user/${id}/role`, { role })
}

export async function getStats() {
  return http.get('/admin/stats')
}

export async function getConfig() {
  return http.get('/admin/config')
}

export async function saveConfig(data) {
  return http.put('/admin/config', data)
}
