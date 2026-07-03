import type { RestResult } from '@/types/api'

/** 系统参数 */
interface SystemParam {
  groupName: string
  params: Record<string, string>
}

/**
 * 获取系统参数（版权信息等）。
 * 降级处理：后端无 /param/grouplist 端点，返回硬编码默认值。
 */
export async function getSystemParams(): Promise<SystemParam[]> {
  return [
    {
      groupName: 'copyright',
      params: { copyright: '© 2024 奇文网盘' },
    },
  ]
}

export type { SystemParam }
