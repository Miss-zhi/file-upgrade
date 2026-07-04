/** 搜索结果项（对应后端 SearchResultVO） */
export interface SearchResultVO {
  userFileId: number
  fileName: string
  extendName: string
  filePath: string
  fileType: number // 0=文件夹，1=文件
  fileSize: number
  uploadTime: string
  modifyTime: string
  /** 高亮文件名（含 <em> 标签） */
  highlightFileName: string
}

/** 搜索请求参数（对应后端 SearchRequestDTO） */
export interface SearchRequestDTO {
  keyword: string
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: string
}

/** 搜索响应（对应后端 SearchResponse） */
export interface SearchResponse {
  total: number
  items: SearchResultVO[]
}

/** 搜索健康检查（对应后端 SearchHealthVO） */
export interface SearchHealthVO {
  available: boolean
  status: string
}

/** OnlyOffice 文档服务健康检查（对应后端 DocumentHealthVO） */
export interface DocumentHealthVO {
  status: string // 'UP' | 'DOWN'
  serverUrl: string
  error: string | null
}
