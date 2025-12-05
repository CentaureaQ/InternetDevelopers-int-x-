<template>
  <div class="knowledge-base-card">
    <div class="card-header">
      <div class="title-section">
        <div class="icon" v-if="knowledgeBase.icon">{{ knowledgeBase.icon }}</div>
        <div v-else class="icon default-icon">📚</div>
        <h3>{{ knowledgeBase.name }}</h3>
      </div>
      <el-tag :type="getAccessLevelType(knowledgeBase.accessLevel)">
        {{ getAccessLevelLabel(knowledgeBase.accessLevel) }}
      </el-tag>
    </div>

    <div class="card-body">
      <p class="description" v-if="knowledgeBase.description">
        {{ truncateText(knowledgeBase.description, 100) }}
      </p>
      <p v-else class="description empty">暂无描述</p>

      <div class="stats">
        <div class="stat-item">
          <span class="stat-label">文档:</span>
          <span class="stat-value">{{ knowledgeBase.documentCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">分块:</span>
          <span class="stat-value">{{ knowledgeBase.chunkCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">大小:</span>
          <span class="stat-value">{{ formatFileSize(knowledgeBase.totalSize) }}</span>
        </div>
      </div>

      <div class="meta">
        <span class="meta-item" v-if="knowledgeBase.createdAt">
          创建: {{ formatDate(knowledgeBase.createdAt) }}
        </span>
      </div>
    </div>

    <div class="card-footer">
      <el-button text @click="handleView">查看</el-button>
      <el-button text @click="handleEdit">编辑</el-button>
      <el-button text type="danger" @click="handleDelete">删除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type KnowledgeBaseVO } from '@/api/knowledge'

interface Props {
  knowledgeBase: KnowledgeBaseVO
}

interface Emits {
  (e: 'view', kb: KnowledgeBaseVO): void
  (e: 'edit', kb: KnowledgeBaseVO): void
  (e: 'delete', kb: KnowledgeBaseVO): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const handleView = () => {
  emit('view', props.knowledgeBase)
}

const handleEdit = () => {
  emit('edit', props.knowledgeBase)
}

const handleDelete = () => {
  emit('delete', props.knowledgeBase)
}

const truncateText = (text: string, maxLength: number) => {
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const getAccessLevelType = (level?: string) => {
  switch (level) {
    case 'public':
      return 'success'
    case 'protected':
      return 'warning'
    case 'private':
      return 'danger'
    default:
      return 'info'
  }
}

const getAccessLevelLabel = (level?: string) => {
  switch (level) {
    case 'public':
      return '公开'
    case 'protected':
      return '受保护'
    case 'private':
      return '私有'
    default:
      return '未知'
  }
}
</script>

<style scoped lang="css">
.knowledge-base-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.knowledge-base-card:hover {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.card-header {
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-section {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
}

.icon {
  font-size: 32px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
}

.icon.default-icon {
  background: rgba(255, 255, 255, 0.3);
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-body {
  padding: 16px;
  flex: 1;
}

.description {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  min-height: 40px;
}

.description.empty {
  color: #999;
  font-style: italic;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 12px 0;
}

.stat-item {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.meta {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.meta-item {
  display: inline-block;
}

.card-footer {
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.card-footer button {
  color: #606266;
  font-size: 14px;
}

.card-footer button:hover {
  color: #409eff;
}

.card-footer button.is-danger:hover {
  color: #f56c6c;
}
</style>
