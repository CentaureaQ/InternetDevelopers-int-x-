<template>
  <div class="page-container">
    <PageHeader :title="knowledgeBase?.name || '知识库'" subtitle="管理知识库中的文档">
      <template #actions>
        <el-button @click="router.back">返回</el-button>
        <el-button type="primary" @click="showUploadDialog = true">
          <el-icon><Upload /></el-icon>
          上传文档
        </el-button>
      </template>
    </PageHeader>

    <!-- 知识库信息卡片 -->
    <el-card class="info-card" v-loading="loading">
      <div class="info-grid">
        <div class="info-item">
          <span class="label">知识库ID:</span>
          <span class="value">{{ knowledgeBase?.id }}</span>
        </div>
        <div class="info-item">
          <span class="label">文档数:</span>
          <span class="value">{{ knowledgeBase?.documentCount || 0 }}</span>
        </div>
        <div class="info-item">
          <span class="label">分块数:</span>
          <span class="value">{{ knowledgeBase?.chunkCount || 0 }}</span>
        </div>
        <div class="info-item">
          <span class="label">分块大小:</span>
          <span class="value">{{ knowledgeBase?.chunkSize || 800 }}</span>
        </div>
        <div class="info-item">
          <span class="label">分块重叠:</span>
          <span class="value">{{ knowledgeBase?.chunkOverlap || 50 }}</span>
        </div>
        <div class="info-item">
          <span class="label">访问级别:</span>
          <span class="value">
            <el-tag :type="getAccessLevelType(knowledgeBase?.accessLevel)">
              {{ getAccessLevelLabel(knowledgeBase?.accessLevel) }}
            </el-tag>
          </span>
        </div>
      </div>

      <div v-if="knowledgeBase?.description" class="description">
        <strong>描述:</strong>
        <p>{{ knowledgeBase.description }}</p>
      </div>
    </el-card>

    <!-- 文档列表 -->
    <el-card class="documents-card" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>文档列表</span>
          <el-button text type="primary" @click="refreshDocuments">刷新</el-button>
        </div>
      </template>

      <el-table :data="documents" v-loading="documentsLoading" stripe>
        <el-table-column prop="name" label="文件名" width="250" />
        <el-table-column prop="fileType" label="文件类型" width="100">
          <template #default="{ row }">
            {{ row.fileType || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="100">
          <template #default="{ row }">
            {{ row.chunkCount || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleViewDocument(row)">
              查看
            </el-button>
            <el-button text type="danger" size="small" @click="handleDeleteDocument(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="documents.length === 0" class="empty-state">
        <p>暂无文档，请上传文档</p>
      </div>
    </el-card>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
      <el-upload
        ref="uploadRef"
        :action="`/api/v1/knowledge-bases/${knowledgeBase?.uuid}/documents`"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :file-list="fileList"
        multiple
      >
        <el-button type="primary">选择文件</el-button>
        <template #tip>
          <div class="el-upload__tip">
            支持上传 TXT, MD 等文本文档格式，单个文件不超过 10MB
          </div>
        </template>
      </el-upload>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmUpload" :loading="uploading">
            上传
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useUserStore } from '@/stores/user'
import {
  getKnowledgeBase,
  fetchKnowledgeDocuments,
  uploadKnowledgeDocument,
  deleteDocument,
  type KnowledgeBaseVO,
  type DocumentVO
} from '@/api/knowledge'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const documentsLoading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const knowledgeBase = ref<KnowledgeBaseVO | null>(null)
const documents = ref<DocumentVO[]>([])
const fileList = ref([])
const uploadRef = ref()

const kbId = computed(() => route.params.id as string)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const loadKnowledgeBase = async () => {
  try {
    loading.value = true
    knowledgeBase.value = await getKnowledgeBase(parseInt(kbId.value))
  } catch (error) {
    ElMessage.error('加载知识库失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const refreshDocuments = async () => {
  if (!knowledgeBase.value?.uuid) return

  try {
    documentsLoading.value = true
    documents.value = await fetchKnowledgeDocuments(knowledgeBase.value.uuid)
  } catch (error) {
    ElMessage.error('加载文档列表失败')
    console.error(error)
  } finally {
    documentsLoading.value = false
  }
}

const handleViewDocument = (doc: DocumentVO) => {
  ElMessage.info(`查看文档: ${doc.name}`)
  // 可以添加查看文档详情的逻辑
}

const handleDeleteDocument = (doc: DocumentVO) => {
  ElMessageBox.confirm(
    `确定要删除文档 "${doc.name}" 吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        if (doc.uuid) {
          await deleteDocument(doc.uuid)
          ElMessage.success('文档已删除')
          await refreshDocuments()
        }
      } catch (error) {
        ElMessage.error('删除文档失败')
        console.error(error)
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const handleUploadSuccess = (response: any) => {
  ElMessage.success('文档上传成功，正在处理...')
  fileList.value = []
  showUploadDialog.value = false
  // Refresh both documents and knowledge base info to update counts
  refreshDocuments()
  loadKnowledgeBase()
}

const handleUploadError = (error: any) => {
  // 优先显示后端返回的错误信息，其次是HTTP错误信息，最后是通用提示
  const errorMessage = 
    error?.response?.data?.message || 
    error?.message || 
    '文档上传失败'
  ElMessage.error(errorMessage)
  console.error('Upload error:', error)
}

const handleConfirmUpload = () => {
  if (uploadRef.value) {
    uploadRef.value.submit()
  }
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const getStatusType = (status?: string) => {
  switch (status) {
    case 'processed':
      return 'success'
    case 'processing':
      return 'warning'
    case 'failed':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'uploading':
      return '上传中'
    case 'processing':
      return '处理中'
    case 'processed':
      return '已处理'
    case 'failed':
      return '失败'
    default:
      return '未知'
  }
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

onMounted(() => {
  loadKnowledgeBase()
  refreshDocuments()
})
</script>

<style scoped lang="css">
.page-container {
  padding: 20px;
}

.info-card {
  margin-top: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info-item .label {
  font-weight: bold;
  color: #666;
  font-size: 12px;
  text-transform: uppercase;
}

.info-item .value {
  font-size: 16px;
  color: #333;
}

.description {
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.description p {
  margin: 10px 0 0 0;
  color: #666;
  line-height: 1.6;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.documents-card {
  margin-top: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
