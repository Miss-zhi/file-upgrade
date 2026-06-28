import http from './http'

export async function searchFiles(keyword) {
  return http.post('/search', null, { params: { keyword } })
}
