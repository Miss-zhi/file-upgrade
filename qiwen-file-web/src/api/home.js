import http from './http'

/** 获取首页统计信息 */
export async function getHomeStats() {
  return http.get('/home/stats')
}

/** 获取公告列表 */
export async function getNoticeList() {
  return http.get('/home/notices')
}
