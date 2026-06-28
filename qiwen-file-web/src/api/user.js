import http from './http'

/** 用户登录 */
export async function login(data) {
  return http.post('/user/login', data)
}

/** 用户注册 */
export async function register(data) {
  return http.post('/user/register', data)
}

/** 获取当前用户信息 */
export async function getUserInfo() {
  return http.get('/user/info')
}
