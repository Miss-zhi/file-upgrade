/** 搜索结果项（对应后端 SearchResultVO） */
export interface SearchResultVO {
  userFileId: number
  fileName: string
  extendName: string
  filePath: string
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
