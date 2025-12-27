<template>
  <div class="page-container">
    <PageHeader title="工作流" subtitle="创建和管理可复用的自动化流程">
      <template #actions>
        <el-button type="primary" @click="goToEditor()">
          <el-icon><Plus /></el-icon>
          创建工作流
        </el-button>
      </template>
    </PageHeader>

    <div class="cards-section">
      <GridContainer v-loading="loading">
        <Card is-add add-text="创建新工作流" @click="goToEditor()" />

        <WorkflowCard
          v-for="workflow in workflows"
          :key="workflow.id"
          :workflow="workflow"
          @view="handleView"
          @edit="handleEdit"
          @publish="handlePublish"
          @delete="handleDelete"
        />
      </GridContainer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GridContainer from '@/components/common/GridContainer.vue'
import Card from '@/components/common/Card.vue'
import WorkflowCard from '@/components/workflow/WorkflowCard.vue'
import {
  fetchWorkflows,
  deleteWorkflow,
  publishWorkflow as publishWorkflowAPI,
  unpublishWorkflow as unpublishWorkflowAPI,
  type WorkflowVO
} from '@/api/workflow'

const router = useRouter()
const workflows = ref<WorkflowVO[]>([])
const loading = ref(false)

async function loadWorkflows() {
  loading.value = true
  try {
    const data = await fetchWorkflows({ pageNo: 1, pageSize: 100 })
    workflows.value = data?.records || []
  } catch (error) {
    console.error('获取工作流列表失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function goToEditor(id?: string) {
  if (id) {
    router.push(`/workflows/editor/${id}`)
  } else {
    router.push('/workflows/editor')
  }
}

function handleView(workflow: WorkflowVO) {
  goToEditor(workflow.id.toString())
}

function handleEdit(workflow: WorkflowVO) {
  goToEditor(workflow.id.toString())
}

async function handlePublish(workflow: WorkflowVO) {
  const actionLabel = workflow.status === 'published' ? '取消发布' : '发布'
  try {
    if (workflow.status === 'published') {
      await unpublishWorkflowAPI(workflow.id)
    } else {
      await publishWorkflowAPI(workflow.id)
    }
    ElMessage.success(`${actionLabel}成功`)
    await loadWorkflows()
  } catch (error) {
    console.error(`${actionLabel}失败`, error)
    ElMessage.error(`${actionLabel}失败`)
  }
}

function handleDelete(workflow: WorkflowVO) {
  ElMessageBox.confirm('确定要删除该工作流吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteWorkflow(workflow.id)
        ElMessage.success('删除成功')
        await loadWorkflows()
      } catch (error) {
        console.error('删除失败', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.page-container {
  padding: var(--spacing-xl);
  max-width: 1600px;
  margin: 0 auto;
  background: var(--bg-primary);
  min-height: 100vh;
  position: relative;
}

.page-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 300px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.05) 0%, rgba(5, 150, 105, 0.03) 50%, rgba(99, 102, 241, 0.02) 100%);
  z-index: -1;
  pointer-events: none;
}

.page-container::after {
  content: '';
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(0deg, rgba(16, 185, 129, 0.03) 0%, transparent 100%);
  z-index: -1;
  pointer-events: none;
}

.cards-section {
  margin-top: var(--spacing-xl);
  padding: var(--spacing-lg);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(229, 231, 235, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  position: relative;
  overflow: hidden;
}
</style>
