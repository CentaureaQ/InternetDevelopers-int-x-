import { http } from '@/utils/http'

export type WorkflowStatus = 'draft' | 'published'

export interface WorkflowVO {
  id: number
  name: string
  description?: string
  status: WorkflowStatus
  version: string
  graph: string
  createdAt: string
  updatedAt: string
}

export interface WorkflowCreateRequest {
  name: string
  description?: string
  graph: string
}

export interface WorkflowUpdateRequest {
  name?: string
  description?: string
  graph?: string
}

export interface WorkflowListQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: WorkflowStatus
}

export interface PageResponse<T> {
  total: number
  pageNo: number
  pageSize: number
  records: T[]
}

export interface WorkflowDebugRequest {
  inputs?: Record<string, any>
}

export interface WorkflowTraceEvent {
  nodeId: string
  nodeType: string
  status: 'running' | 'success' | 'error'
  startedAt?: number
  finishedAt?: number
  input?: any
  output?: any
  error?: string
}

export interface WorkflowDebugResponse {
  output: any
  trace: WorkflowTraceEvent[]
}

export const fetchWorkflows = (query?: WorkflowListQuery): Promise<PageResponse<WorkflowVO>> => {
  return http.get('/api/workflows', { params: query })
}

export const getWorkflow = (id: number): Promise<WorkflowVO> => {
  return http.get(`/api/workflows/${id}`)
}

export const createWorkflow = (data: WorkflowCreateRequest): Promise<number> => {
  return http.post('/api/workflows', data)
}

export const updateWorkflow = (id: number, data: WorkflowUpdateRequest): Promise<void> => {
  return http.put(`/api/workflows/${id}`, data)
}

export const deleteWorkflow = (id: number): Promise<void> => {
  return http.delete(`/api/workflows/${id}`)
}

export const publishWorkflow = (id: number): Promise<void> => {
  return http.post(`/api/workflows/${id}/publish`)
}

export const unpublishWorkflow = (id: number): Promise<void> => {
  return http.post(`/api/workflows/${id}/unpublish`)
}

export const debugWorkflow = (id: number, data: WorkflowDebugRequest): Promise<WorkflowDebugResponse> => {
  return http.post(`/api/workflows/${id}/debug`, data)
}
