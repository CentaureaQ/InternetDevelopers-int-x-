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
import { ref, watch } from 'vue'
import type { AgentVO } from '@/api/agent'

interface FormData {
  name: string
  description: string
  systemPrompt: string
  model: {
    provider: string
    model: string
    temperature: number
  }
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

const formData = ref<FormData>({
  name: '',
  description: '',
  systemPrompt: '',
  model: {
    provider: 'deepseek',
    model: 'deepseek-chat',
    temperature: 0.7
  }
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
      }
    }
  } else {
    isEdit.value = false
    resetForm()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function resetForm() {
  formData.value = {
    name: '',
    description: '',
    systemPrompt: '',
    model: {
      provider: 'deepseek',
      model: 'deepseek-chat',
      temperature: 0.7
    }
  }
}

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
