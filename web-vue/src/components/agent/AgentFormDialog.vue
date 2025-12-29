<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑智能体' : '创建智能体'"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="formData" label-width="100px">
      <el-form-item label="名称" required>
        <el-input v-model="formData.name" placeholder="请输入智能体名称" />
      </el-form-item>
      
      <el-form-item label="描述">
        <el-input 
          v-model="formData.description" 
          placeholder="一句话介绍智能体"
        />
      </el-form-item>
      
      <el-form-item label="系统提示词" required>
        <el-input
          v-model="formData.systemPrompt"
          type="textarea"
          :rows="4"
          placeholder="描述AI的身份与能力"
        />
      </el-form-item>
      
      <el-form-item label="模型服务商" required>
        <el-input 
          v-model="formData.model.provider" 
          placeholder="如 deepseek"
        />
      </el-form-item>
      
      <el-form-item label="模型名称" required>
        <el-input 
          v-model="formData.model.model" 
          placeholder="如 deepseek-chat"
        />
      </el-form-item>
      
      <el-form-item label="Temperature">
        <el-input-number
          v-model="formData.model.temperature"
          :min="0"
          :max="1"
          :step="0.1"
          style="width: 100%"
        />
      </el-form-item>
      
      <el-form-item label="关联工作流">
        <el-select 
          v-model="formData.workflowId" 
          placeholder="选择已发布的工作流（可选）" 
          style="width: 100%" 
          clearable
          filterable
        >
          <el-option
            v-for="workflow in publishedWorkflows"
            :key="workflow.id"
            :label="`${workflow.name} (ID: ${workflow.id})`"
            :value="workflow.id"
          >
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>{{ workflow.name }}</span>
              <span style="color: var(--text-secondary); font-size: 12px;">ID: {{ workflow.id }}</span>
            </div>
          </el-option>
        </el-select>
        <div style="font-size: 12px; color: var(--text-secondary); margin-top: 4px;">
          选择工作流后，智能体将使用工作流执行逻辑，而不是传统的 LLM 调用
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { AgentVO } from '@/api/agent'
import { fetchWorkflows, type WorkflowVO } from '@/api/workflow'

interface FormData {
  name: string
  description: string
  systemPrompt: string
  model: {
    provider: string
    model: string
    temperature: number
  }
  workflowId?: number
}

const props = defineProps<{
  modelValue: boolean
  agent?: AgentVO
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [data: FormData]
}>()

const visible = ref(props.modelValue)
const isEdit = ref(false)
const publishedWorkflows = ref<WorkflowVO[]>([])

const formData = ref<FormData>({
  name: '',
  description: '',
  systemPrompt: '',
  model: {
    provider: 'deepseek',
    model: 'deepseek-chat',
    temperature: 0.7
  },
  workflowId: undefined
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.agent) {
    isEdit.value = true
    const modelConfig = props.agent.modelConfig || formData.value.model
    formData.value = {
      name: props.agent.name,
      description: props.agent.description || '',
      systemPrompt: props.agent.systemPrompt || '',
      model: {
        provider: modelConfig.provider,
        model: modelConfig.model,
        temperature: modelConfig.temperature ?? 0.7
      },
      workflowId: props.agent.workflowId
    }
  } else {
    isEdit.value = false
    resetForm()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

async function loadPublishedWorkflows() {
  try {
    const data = await fetchWorkflows({ status: 'published', pageNo: 1, pageSize: 100 })
    publishedWorkflows.value = data?.records || []
  } catch (error) {
    console.error('加载已发布工作流失败', error)
  }
}

function resetForm() {
  formData.value = {
    name: '',
    description: '',
    systemPrompt: '',
    model: {
      provider: 'deepseek',
      model: 'deepseek-chat',
      temperature: 0.7
    },
    workflowId: undefined
  }
}

onMounted(() => {
  loadPublishedWorkflows()
})

function handleClose() {
  visible.value = false
}

function handleSubmit() {
  if (!formData.value.name || !formData.value.systemPrompt) {
    return
  }
  emit('submit', formData.value)
}
</script>
