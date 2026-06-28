import { get, post } from './http'

// 获取用户列表
export const getUserList = (p) => get('/admin/user/list', p)
// 修改用户状态
export const updateUserAvailable = (p) => post('/admin/user/updateAvailable', p)
// 修改用户存储空间
export const updateUserStorage = (p) => post('/admin/storage/updateTotalStorage', p)
// 重置用户密码
export const resetPassword = (p) => post('/admin/user/resetPassword', p)
