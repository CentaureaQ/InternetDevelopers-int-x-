import { http } from '@/utils/http'

export interface RagConfig {
  topK?: number
  threshold?: number
  maxContextLength?: number
  similarityMetric?: string
}

export interface KnowledgeBaseCreateRequest {
  name: string
  description?: string
  chunkSize?: number
  chunkOverlap?: number
  accessLevel?: string
  icon?: string
}

export interface KnowledgeBaseVO {
  id?: number
  uuid?: string
  name: string
  description?: string
  icon?: string
  level?: string
  parentKbId?: number
  vectorDbType?: string
  embeddingModelId?: string
  chunkSize?: number
  chunkOverlap?: number
  retrievalConfig?: string
  accessLevel?: string
  ownerId?: number
  documentCount?: number
  chunkCount?: number
  totalSize?: number
  createdAt?: string
  updatedAt?: string
}

export interface DocumentVO {
  id?: number
  uuid?: string
  name: string
  filePath?: string
  fileSize?: number
  fileType?: string
  knowledgeBaseId?: number
  status?: string
  chunkCount?: number
  errorMessage?: string
  processedAt?: string
  uploadedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface KnowledgeChunkVO {
  id?: number
  chunkId?: number
  documentId?: number
  knowledgeBaseId?: number
  content: string
  vector?: number[]
  similarity?: number
}

export const createKnowledgeBase = (data: KnowledgeBaseCreateRequest): Promise<KnowledgeBaseVO> => {
  return http.post('/api/v1/knowledge-bases', data)
}

export const fetchKnowledgeBases = (): Promise<KnowledgeBaseVO[]> => {
  return http.get('/api/v1/knowledge-bases')
}

export const getMyKnowledgeBases = (): Promise<KnowledgeBaseVO[]> => {
  return http.get('/api/v1/knowledge-bases/my')
}

export const getKnowledgeBase = (id: number): Promise<KnowledgeBaseVO> => {
  return http.get(`/api/v1/knowledge-bases/${id}`)
}

export const updateKnowledgeBase = (id: number, data: Partial<KnowledgeBaseVO>): Promise<KnowledgeBaseVO> => {
  return http.put(`/api/v1/knowledge-bases/${id}`, data)
}

export const deleteKnowledgeBase = (id: number): Promise<void> => {
  return http.delete(`/api/v1/knowledge-bases/${id}`)
}

export const uploadKnowledgeDocument = (kbUuid: string, file: File): Promise<DocumentVO> => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post(`/api/v1/knowledge-bases/${kbUuid}/documents`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const fetchKnowledgeDocuments = (kbUuid: string, status?: string): Promise<DocumentVO[]> => {
  return http.get(`/api/v1/knowledge-bases/${kbUuid}/documents`, { params: { status } })
}

export const getDocument = (uuid: string): Promise<DocumentVO> => {
  return http.get(`/api/v1/documents/${uuid}`)
}

export const deleteDocument = (uuid: string): Promise<void> => {
  return http.delete(`/api/v1/documents/${uuid}`)
}



