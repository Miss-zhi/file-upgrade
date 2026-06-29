import http from './http'

export async function uploadChunk(chunk, params) {
  const formData = new FormData()
  formData.append('chunk', chunk)
  Object.entries(params).forEach(([k, v]) => formData.append(k, String(v)))
  return http.post('/filetransfer/upload-chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function mergeChunks(identifier, filePath) {
  return http.post('/filetransfer/merge-chunks', { identifier, filePath })
}

export async function getProgress(identifier) {
  return http.get(`/filetransfer/progress/${identifier}`)
}
