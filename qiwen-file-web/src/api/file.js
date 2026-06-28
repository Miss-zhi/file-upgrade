import http from './http'

/** 根据路径获取文件列表 */
export async function getFileListByPath(params) {
  return http.post('/file/list', params)
}

/** 上传文件 */
export async function uploadFile(formData, onProgress) {
  return http.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

/** 删除文件 */
export async function deleteFile(data) {
  return http.post('/file/delete', data)
}

/** 创建文件夹 */
export async function createFolder(data) {
  return http.post('/file/create-folder', data)
}
