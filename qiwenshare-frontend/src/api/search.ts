import client from './client'
import type { RestResult } from '@/types/api'
import type { SearchResponse, SearchRequestDTO, SearchHealthVO } from '@/types/search'

/** 全文搜索文件 */
export async function searchFiles(params: SearchRequestDTO): Promise<SearchResponse> {
  const { data } = await client.get<RestResult<SearchResponse>>('/search', { params })
  return data.data
}

/** 检查搜索服务健康状态 */
export async function getSearchHealth(): Promise<SearchHealthVO> {
  const { data } = await client.get<RestResult<SearchHealthVO>>('/search/health')
  return data.data
}

/** 重建搜索索引（管理员） */
export async function rebuildSearchIndex(): Promise<void> {
  const { data } = await client.post<RestResult<null>>('/search/admin/rebuild')
  if (data.code !== 0) throw new Error(data.message)
}
