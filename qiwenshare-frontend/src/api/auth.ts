import client from './client'
import type { RestResult } from '@/types/api'

/** 登录请求参数 */
interface LoginParams {
  telephone: string
  password: string
}

/** 注册请求参数 */
interface RegisterParams {
  username: string
  telephone: string
  password: string
}

/** 登录响应 */
interface LoginResult {
  userId: string
  username: string
  roles: string[]
  permissions: string[]
}

/** 用户信息 */
interface UserInfo {
  userId: string
  username: string
  telephone: string
  avatar: string | null
  roles: string[]
  permissions: string[]
  registerTime: string
}

/** 修改密码参数 */
interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

/**
 * 用户登录。
 */
export async function login(params: LoginParams): Promise<LoginResult> {
  const { data } = await client.post<RestResult<LoginResult>>('/auth/login', params)
  return data.data
}

/**
 * 用户注册。
 */
export async function register(params: RegisterParams): Promise<string> {
  const { data } = await client.post<RestResult<{ userId: string }>>('/auth/register', params)
  return data.data.userId
}

/**
 * 用户登出。
 */
export async function logout(): Promise<void> {
  await client.post('/auth/logout')
}

/**
 * 获取当前用户信息。
 */
export async function fetchMe(): Promise<UserInfo> {
  const { data } = await client.get<RestResult<UserInfo>>('/auth/me')
  return data.data
}

/**
 * 修改密码。
 */
export async function changePassword(params: ChangePasswordParams): Promise<void> {
  await client.put('/auth/password', params)
}

export type { LoginParams, RegisterParams, LoginResult, UserInfo, ChangePasswordParams }
