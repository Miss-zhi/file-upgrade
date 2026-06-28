import http from './http'

export async function getEditorConfig(fileId) {
  return http.get(`/onlyoffice/edit/${fileId}`)
}
