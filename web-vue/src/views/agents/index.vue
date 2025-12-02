<template>
  <div class="page-container">
    <PageHeader title="智能体" subtitle="创建和管理您的 AI 智能体">
      <template #actions>
        <el-button type="primary" @click="goToEditor">
          <el-icon><Plus /></el-icon>
          创建智能体
        </el-button>
      </template>
    </PageHeader>

    <div class="cards-section">
      <GridContainer v-loading="loading">
        <Card is-add add-text="创建新智能体" @click="goToEditor" />

        <AgentCard
          v-for="agent in agents"
          :key="agent.id"
          :agent="agent"
          @view="handleView"
          @edit="handleEdit"
          @publish="handlePublish"
          @delete="handleDelete"
        />
      </GridContainer>
    </div>

    <AgentFormDialog
      v-model="showDialog"
      :agent="editingAgent"
      :loading="submitting"
      @submit="handleSubmit"
    />
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
import AgentCard from '@/components/agent/AgentCard.vue'
import AgentFormDialog from '@/components/agent/AgentFormDialog.vue'
import { fetchAgents, createAgent, updateAgent, publishAgent as publishAgentAPI, unpublishAgent as unpublishAgentAPI, type AgentVO } from '@/api/agent'

const router = useRouter()
const agents = ref<AgentVO[]>([])
const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const editingAgent = ref<AgentVO | undefined>()

async function loadAgents() {
  loading.value = true
  try {
    const data = await fetchAgents({ pageNo: 1, pageSize: 100 })
    agents.value = data?.records || []
  } catch (error) {
    console.error('获取智能体列表失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleView(agent: AgentVO) {
  goToEditor(agent.id.toString())
}

function goToEditor(agentId?: string) {
  if (agentId) {
    router.push(`/agents/editor/${agentId}`)
  } else {
    router.push('/agents/editor')
  }
}

function handleEdit(agent: AgentVO) {
  goToEditor(agent.id.toString())
}

async function handleSubmit(data: any) {
  submitting.value = true
  const isEdit = Boolean(editingAgent.value?.id)
  const payload = {
    ...data,
    userPromptTemplate: '',
    modelConfig: { ...data.model, maxTokens: 2048 }
  }
  try {
    if (isEdit && editingAgent.value) {
      await updateAgent(editingAgent.value.id, payload)
    } else {
      await createAgent(payload)
    }
    ElMessage.success(isEdit ? '保存成功' : '创建成功')
    showDialog.value = false
    editingAgent.value = undefined
    await loadAgents()
  } catch (error) {
    console.error('操作失败', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function handlePublish(agent: AgentVO) {
  const actionLabel = agent.status === 'published' ? '取消发布' : '发布'
  try {
    if (agent.status === 'published') {
      await unpublishAgentAPI(agent.id)
    } else {
      await publishAgentAPI(agent.id)
    }
    ElMessage.success(`${actionLabel}成功`)
    loadAgents()
  } catch (error) {
    console.error(`${actionLabel}失败`, error)
    ElMessage.error(`${actionLabel}失败`)
  }
}

function handleDelete(_agent: AgentVO) {
  ElMessageBox.confirm('确定要删除该智能体吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      ElMessage.success('删除成功')
      loadAgents()
    })
    .catch(() => {})
}

onMounted(() => {
  loadAgents()
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
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.05) 0%, rgba(139, 92, 246, 0.03) 50%, rgba(236, 72, 153, 0.02) 100%);
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
  background: linear-gradient(0deg, rgba(99, 102, 241, 0.03) 0%, transparent 100%);
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

.cards-section::before {
  content: '';
  position: absolute;
  top: -50px;
  right: -50px;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.cards-section::after {
  content: '';
  position: absolute;
  bottom: -30px;
  left: -30px;
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}
</style>
