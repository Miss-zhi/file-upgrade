/** 分页响应（对应后端 PageResponse<T>） */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  first: boolean
  last: boolean
}

// ---- 用户管理 ----

/** 用户列表响应项（对应后端 UserListVO） */
export interface UserListVO {
  userId: string
  username: string
  telephone: string
  available: number // 1=启用, 0=禁用
  registerTime: string
  roles: string[]
}

/** 角色简要信息 */
export interface RoleInfo {
  roleId: number
  roleName: string
}

/** 用户详情响应（对应后端 UserDetailVO） */
export interface UserDetailVO {
  userId: string
  username: string
  telephone: string
  available: number
  registerTime: string
  roles: RoleInfo[]
  permissions: string[]
}

/** 重置密码请求 */
export interface ResetPasswordRequest {
  newPassword: string
}

// ---- 角色权限管理 ----

/** 角色响应（对应后端 RoleResponse） */
export interface RoleVO {
  roleId: number
  roleName: string
  roleDesc: string
  available: number
  permissions: number[]
}

/** 更新角色权限请求 */
export interface UpdateRolePermissionsRequest {
  permissionIds: number[]
}

/** 权限树节点 */
export interface PermissionNode {
  permissionId: number
  permKey: string
  permName: string
  parentId: number
  permType: number // 1=菜单, 2=按钮
  children?: PermissionNode[]
}

// ---- 配额管理 ----

/** 配额响应（对应后端 AdminQuotaVO） */
export interface AdminQuotaVO {
  userId: string
  totalQuota: number // 字节
  usedQuota: number // 字节
  availableQuota: number // 字节
}

/** 设置配额请求 */
export interface SetQuotaRequest {
  totalQuota: number // 字节
}

/** 批量配额项 */
export interface QuotaItem {
  userId: string
  totalQuota: number // 字节
}

/** 批量设置配额请求 */
export interface BatchSetQuotaRequest {
  items: QuotaItem[]
}

// ---- 审计日志 ----

/** 操作日志响应（对应后端 OperationLogVO） */
export interface OperationLogVO {
  id: number
  userId: string
  username: string
  module: string
  action: string
  description: string
  requestMethod: string
  requestUri: string
  requestParams: string
  responseCode: number
  errorMessage: string
  ipAddress: string
  userAgent: string
  executionTime: number
  createTime: string
}

/** 审计日志查询参数 */
export interface LogQueryParams {
  module?: string
  action?: string
  username?: string
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}

// ---- 系统配置 ----

/** 配置响应（对应后端 ConfigVO） */
export interface ConfigVO {
  id: number
  configKey: string
  configValue: string
  description: string
  createTime: string
  updateTime: string
}

/** 新增配置请求 */
export interface CreateConfigRequest {
  configKey: string
  configValue: string
  description?: string
}

/** 编辑配置请求 */
export interface UpdateConfigRequest {
  configValue?: string
  description?: string
}

// ---- 权限树静态配置 ----

/**
 * 系统预定义权限树（两级结构）。
 * 用于角色权限编辑对话框的 el-tree 展示。
 * 权限编码与数据库 permission 表保持一致。
 */
export const ADMIN_PERMISSION_TREE: PermissionNode[] = [
  {
    permissionId: 1,
    permKey: 'admin',
    permName: '后台管理',
    parentId: 0,
    permType: 1,
    children: [
      { permissionId: 101, permKey: 'admin:user-manage', permName: '用户管理', parentId: 1, permType: 2 },
      { permissionId: 102, permKey: 'admin:role-manage', permName: '角色管理', parentId: 1, permType: 2 },
      { permissionId: 103, permKey: 'admin:quota-manage', permName: '配额管理', parentId: 1, permType: 2 },
      { permissionId: 104, permKey: 'admin:log-view', permName: '审计日志', parentId: 1, permType: 2 },
      { permissionId: 105, permKey: 'admin:config-manage', permName: '系统配置', parentId: 1, permType: 2 },
      { permissionId: 106, permKey: 'admin:search-rebuild', permName: '重建索引', parentId: 1, permType: 2 },
      { permissionId: 107, permKey: 'admin:document-health', permName: '文档健康检查', parentId: 1, permType: 2 },
    ],
  },
]

/**
 * 根据权限树生成「所有叶子节点 ID」列表。
 * 用于后端提交全量权限时的 diff。
 */
export function getAllLeafPermissionIds(): number[] {
  const ids: number[] = []
  for (const parent of ADMIN_PERMISSION_TREE) {
    if (parent.children) {
      for (const child of parent.children) {
        ids.push(child.permissionId)
      }
    }
  }
  return ids
}
