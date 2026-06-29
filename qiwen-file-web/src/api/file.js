import http from './http'

export async function getFileListByPath(params) {
  return http.post('/file/list', params)
}

export async function uploadFile(formData) {
  return http.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function deleteFile(data) {
  return http.post('/file/delete', data)
}

export async function createFolder(path, folderName) {
  return http.post('/file/create-folder', null, { params: { path, folderName } })
}

export async function batchDeleteFiles(ids) {
  return http.post('/file/batch-delete', { ids })
}

export async function batchMoveFiles(ids, targetPath) {
  return http.post('/file/batch-move', { ids, targetPath })
}

export async function moveFile(id, targetPath) {
  return http.post('/file/move', { id, targetPath })
}

export async function copyFile(id, targetPath) {
  return http.post('/file/copy', { id, targetPath })
}

export async function downloadFile(id) {
  return http.get(`/file/download/${id}`)
}
