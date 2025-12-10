import { http } from '@/utils/http'

export interface ModelConfig {
  provider: string
  model: string
  temperature?: number
  maxTokens?: number
  topP?: number
}

export interface RagConfig {
  topK?: number
  threshold?: number
  maxContextLength?: number
  similarityMetric?: string
}

export interface AgentCreateRequest {
  name: string
  description?: string
  systemPrompt: string
  userPromptTemplate?: string
  modelConfig: ModelConfig
  pluginIds?: number[]
  ownerUserId?: number
  knowledgeBaseId?: number
  ragConfig?: RagConfig
}

export interface AgentUpdateRequest {
  name?: string
  description?: string
  systemPrompt?: string
  userPromptTemplate?: string
  modelConfig?: ModelConfig
  pluginIds?: number[]
  knowledgeBaseId?: number
  ragConfig?: RagConfig
}

export interface AgentListQuery {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface AgentVO {
  id: number
  name: string
  description?: string
  systemPrompt?: string
  userPromptTemplate?: string
  modelConfig?: ModelConfig
  pluginIds?: number[]
  ownerUserId?: number
  status: string
  createdAt: string
  updatedAt: string
  knowledgeBaseId?: number
  ragConfig?: RagConfig
}

export interface PageResponse<T> {
  total: number
  pageNo: number
  pageSize: number
  records: T[]
}

export interface AgentTestRequest {
  question: string
  ragConfig?: RagConfig
}

export interface AgentTestResponse {
  reply: string
  elapsedMs: number
  promptTokens: number
  completionTokens: number
}

export const createAgent = (data: AgentCreateRequest): Promise<number> => {
  return http.post('/api/agents', data)
}

export const updateAgent = (id: number, data: AgentUpdateRequest): Promise<void> => {
  return http.put(`/api/agents/${id}`, data)
}

export const deleteAgent = (id: number): Promise<void> => {
  return http.delete(`/api/agents/${id}`)
}

export const getAgent = (id: number): Promise<AgentVO> => {
  return http.get(`/api/agents/${id}`)
}

export const fetchAgents = (query?: AgentListQuery): Promise<PageResponse<AgentVO>> => {
  return http.get('/api/agents', { params: query })
}

export const publishAgent = (id: number): Promise<void> => {
  return http.post(`/api/agents/${id}/publish`)
}

export const unpublishAgent = (id: number): Promise<void> => {
  return http.post(`/api/agents/${id}/unpublish`)
}

export const testAgent = (id: number, data: AgentTestRequest): Promise<AgentTestResponse> => {
  return http.post(`/api/agents/${id}/test`, data)
}



