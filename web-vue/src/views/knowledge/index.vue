<template>
  <div class="page-container">
    <PageHeader title="知识库" subtitle="创建和管理您的知识库">
      <template #actions>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建知识库
        </el-button>
      </template>
    </PageHeader>

    <div class="cards-section">
      <GridContainer v-loading="loading">
        <Card is-add add-text="创建新知识库" @click="showCreateDialog = true" />

        <KnowledgeBaseCard
          v-for="kb in knowledgeBases"
          :key="kb.id"
          :knowledge-base="kb"
          @view="handleView"
          @edit="handleEdit"
          @delete="handleDelete"
        />
      </GridContainer>
    </div>

    <!-- 创建/编辑知识库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingKb ? '编辑知识库' : '创建知识库'"
      width="500px"
    >
      <el-form ref="formRef" :model="formData" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入知识库名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            rows="3"
            placeholder="请输入知识库描述"
          />
        </el-form-item>

        <el-form-item label="访问级别" prop="accessLevel">
          <el-select v-model="formData.accessLevel" placeholder="选择访问级别">
            <el-option label="私有" value="private" />
            <el-option label="受保护" value="protected" />
            <el-option label="公开" value="public" />
          </el-select>
        </el-form-item>

        <el-form-item label="分块大小" prop="chunkSize">
          <el-input-number v-model="formData.chunkSize" :min="100" :max="4000" />
        </el-form-item>

        <el-form-item label="分块重叠" prop="chunkOverlap">
          <el-input-number v-model="formData.chunkOverlap" :min="0" :max="200" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="submitting">
            {{ editingKb ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GridContainer from '@/components/common/GridContainer.vue'
import Card from '@/components/common/Card.vue'
import KnowledgeBaseCard from '@/components/knowledge/KnowledgeBaseCard.vue'
import {
  fetchKnowledgeBases,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase,
  type KnowledgeBaseVO,
  type KnowledgeBaseCreateRequest
} from '@/api/knowledge'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const knowledgeBases = ref<KnowledgeBaseVO[]>([])
const showCreateDialog = ref(false)
const editingKb = ref<KnowledgeBaseVO | null>(null)
const formRef = ref()

const formData = reactive<KnowledgeBaseCreateRequest>({
  name: '',
  description: '',
  accessLevel: 'private',
  chunkSize: 800,
  chunkOverlap: 50
})

const loadKnowledgeBases = async () => {
  try {
    loading.value = true
    knowledgeBases.value = await fetchKnowledgeBases()
  } catch (error) {
    ElMessage.error('加载知识库失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleView = (kb: KnowledgeBaseVO) => {
  router.push({
    name: 'KnowledgeBaseDetail',
    params: { id: kb.id }
  })
}

const handleEdit = (kb: KnowledgeBaseVO) => {
  editingKb.value = kb
  formData.name = kb.name
  formData.description = kb.description || ''
  formData.accessLevel = kb.accessLevel || 'private'
  formData.chunkSize = kb.chunkSize || 800
  formData.chunkOverlap = kb.chunkOverlap || 50
  showCreateDialog.value = true
}

const handleDelete = (kb: KnowledgeBaseVO) => {
  ElMessageBox.confirm(
    `确定要删除知识库 "${kb.name}" 吗？删除后无法恢复。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        if (kb.id) {
          await deleteKnowledgeBase(kb.id)
          ElMessage.success('知识库已删除')
          await loadKnowledgeBases()
        }
      } catch (error) {
        ElMessage.error('删除知识库失败')
        console.error(error)
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const handleSave = async () => {
  if (!formData.name) {
    ElMessage.error('请输入知识库名称')
    return
  }

  try {
    submitting.value = true
    if (editingKb.value && editingKb.value.id) {
      await updateKnowledgeBase(editingKb.value.id, formData)
      ElMessage.success('知识库已更新')
    } else {
      await createKnowledgeBase(formData)
      ElMessage.success('知识库已创建')
    }
    showCreateDialog.value = false
    editingKb.value = null
    resetForm()
    await loadKnowledgeBases()
  } catch (error) {
    ElMessage.error(editingKb.value ? '更新知识库失败' : '创建知识库失败')
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formData.name = ''
  formData.description = ''
  formData.accessLevel = 'private'
  formData.chunkSize = 800
  formData.chunkOverlap = 50
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped lang="css">
.page-container {
  padding: 20px;
}

.cards-section {
  margin-top: 30px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
