import client from './client'
import type { RestResult } from '@/types/api'
import type {
  PageResponse,
  UserListVO,
  UserDetailVO,
  ResetPasswordRequest,
  RoleVO,
  UpdateRolePermissionsRequest,
  AdminQuotaVO,
  SetQuotaRequest,
  BatchSetQuotaRequest,
  OperationLogVO,
  LogQueryParams,
  ConfigVO,
  CreateConfigRequest,
  UpdateConfigRequest,
} from '@/types/admin'

// ==================== 用户管理 ====================

/**
 * 分页查询用户列表。
 */
export async function listUsers(params: {
  keyword?: string
  available?: number
  page?: number
  pageSize?: number
}): Promise<PageResponse<UserListVO>> {
  const { data } = await client.get<RestResult<PageResponse<UserListVO>>>('/admin/users', { params })
  return data.data
}

/**
 * 查询用户详情（含角色和权限列表）。
 */
export async function getUserDetail(userId: string): Promise<UserDetailVO> {
  const { data } = await client.get<RestResult<UserDetailVO>>(`/admin/users/${userId}`)
  return data.data
}

/**
 * 启用用户。
 */
export async function enableUser(userId: string): Promise<void> {
  await client.put(`/admin/users/${userId}/enable`)
}

/**
 * 禁用用户。
 */
export async function disableUser(userId: string): Promise<void> {
  await client.put(`/admin/users/${userId}/disable`)
}

/**
 * 重置用户密码。
 */
export async function resetUserPassword(
  userId: string,
  body: ResetPasswordRequest,
): Promise<void> {
  await client.put(`/admin/users/${userId}/password`, body)
}

// ==================== 角色权限管理 ====================

/**
 * 获取所有角色列表（含权限信息）。
 */
export async function listRoles(): Promise<RoleVO[]> {
  const { data } = await client.get<RestResult<RoleVO[]>>('/admin/roles')
  return data.data
}

/**
 * 更新角色权限（全量替换）。
 */
export async function updateRolePermissions(
  roleId: number,
  body: UpdateRolePermissionsRequest,
): Promise<void> {
  await client.put(`/admin/roles/${roleId}/permissions`, body)
}

// ==================== 配额管理 ====================

/**
 * 查询用户配额信息。
 */
export async function getUserQuota(userId: string): Promise<AdminQuotaVO> {
  const { data } = await client.get<RestResult<AdminQuotaVO>>(`/admin/quota/${userId}`)
  return data.data
}

/**
 * 设置用户配额。
 */
export async function setUserQuota(userId: string, body: SetQuotaRequest): Promise<void> {
  await client.put(`/admin/quota/${userId}`, body)
}

/**
 * 批量设置用户配额。
 * @returns 被跳过的 userId 列表
 */
export async function batchSetQuota(body: BatchSetQuotaRequest): Promise<string[]> {
  const { data } = await client.put<RestResult<string[]>>('/admin/quota/batch', body)
  return data.data
}

// ==================== 审计日志 ====================

/**
 * 分页查询操作日志。
 */
export async function listLogs(params: LogQueryParams): Promise<PageResponse<OperationLogVO>> {
  const { data } = await client.get<RestResult<PageResponse<OperationLogVO>>>('/admin/logs', { params })
  return data.data
}

// ==================== 系统配置 ====================

/**
 * 分页查询系统参数。
 */
export async function listConfigs(params: {
  keyword?: string
  page?: number
  pageSize?: number
}): Promise<PageResponse<ConfigVO>> {
  const { data } = await client.get<RestResult<PageResponse<ConfigVO>>>('/admin/config', { params })
  return data.data
}

/**
 * 新增系统参数。
 */
export async function createConfig(body: CreateConfigRequest): Promise<ConfigVO> {
  const { data } = await client.post<RestResult<ConfigVO>>('/admin/config', body)
  return data.data
}

/**
 * 修改系统参数。
 */
export async function updateConfig(id: number, body: UpdateConfigRequest): Promise<ConfigVO> {
  const { data } = await client.put<RestResult<ConfigVO>>(`/admin/config/${id}`, body)
  return data.data
}

/**
 * 删除系统参数。
 */
export async function deleteConfig(id: number): Promise<void> {
  await client.delete(`/admin/config/${id}`)
}

