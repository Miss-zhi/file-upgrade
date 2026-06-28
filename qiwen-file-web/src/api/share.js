import http from './http'

export async function createShare(data) {
  return http.post('/share/create', data)
}

export async function listShares() {
  return http.post('/share/list')
}

export async function cancelShare(id) {
  return http.post('/share/cancel', { fileId: id })
}

export async function verifyShare(token, code) {
  return http.get('/share/verify', { params: { token, code } })
}
