/** 公告列表查询参数 */
interface NoticeListParams {
  currentPage: number
  pageCount: number
}

/** 公告详情查询参数 */
interface NoticeDetailParams {
  noticeId: string
}

/** 公告条目 */
interface NoticeItem {
  noticeId: string
  title: string
  publishTime: string
  content: string
}

/**
 * 获取公告列表。
 * 降级处理：后端无 /notice/list 端点，返回空数组。
 */
export async function getNoticeList(_params: NoticeListParams): Promise<NoticeItem[]> {
  return []
}

/**
 * 获取公告详情。
 * 降级处理：后端无 /notice/detail 端点，返回空对象。
 */
export async function getNoticeDetail(_params: NoticeDetailParams): Promise<NoticeItem> {
  return {
    noticeId: '',
    title: '暂无公告',
    publishTime: '',
    content: '',
  }
}

export type { NoticeListParams, NoticeDetailParams, NoticeItem }
