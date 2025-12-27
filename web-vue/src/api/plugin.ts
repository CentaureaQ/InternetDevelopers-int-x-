import { http } from '@/utils/http'

// 插件类型定义
export interface Plugin {
  id: number
  name: string
  type: 'builtin' | 'custom'
  status: 'enabled' | 'disabled'
  description?: string
  openapiSpec: string
  config: string
  createdAt?: string
  updatedAt?: string
}

// 插件执行参数类型
export interface PluginExecuteParams {
  functionName: string
  arguments: Record<string, any>
}

// 插件执行结果类型
export interface PluginExecuteResult {
  pluginId: number
  functionName: string
  result: string
}

// 获取插件列表
export function listPlugins(params: Record<string, any> = {}) {
  return http.get<Plugin[]>('/api/plugin', { params })
}

// 获取插件详情
export function getPlugin(id: number) {
  return http.get<Plugin>(`/api/plugin/${id}`)
}

// 创建插件
export function createPlugin(plugin: Omit<Plugin, 'id' | 'type' | 'status' | 'createdAt' | 'updatedAt'>) {
  return http.post<Plugin>('/api/plugin', plugin)
}

// 更新插件
export function updatePlugin(id: number, plugin: Omit<Plugin, 'id' | 'type' | 'createdAt' | 'updatedAt'>) {
  return http.put<Plugin>(`/api/plugin/${id}`, plugin)
}

// 删除插件
export function deletePlugin(id: number) {
  return http.delete(`/api/plugin/${id}`)
}

// 启用插件
export function enablePlugin(id: number) {
  return http.post(`/api/plugin/${id}/enable`)
}

// 禁用插件
export function disablePlugin(id: number) {
  return http.post(`/api/plugin/${id}/disable`)
}

// 执行插件函数
export function executePlugin(id: number, data: PluginExecuteParams) {
  return http.post<PluginExecuteResult>(`/api/plugin/${id}/execute`, data)
}
