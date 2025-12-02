<template>
  <div class="editor-page">
    <!-- 页面头部 -->
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
        <h1>{{ isEdit ? '编辑智能体' : '新建智能体' }}</h1>
      </div>
      <div class="header-right">
        <el-button @click="saveDraft" :loading="isSaving">保存草稿</el-button>
        <el-button type="primary" @click="publishAgent" :loading="isSaving">发布</el-button>
      </div>
    </div>

    <!-- 三列布局 -->
    <div class="editor-content" v-loading="loading">
      <!-- 左侧：系统提示词 -->
      <div class="editor-column left-column">
        <div class="column-header">
          <h3>系统提示词</h3>
          <el-tooltip content="系统提示词定义了智能体的角色和行为方式">
            <el-icon><InfoFilled /></el-icon>
          </el-tooltip>
        </div>
        <div class="prompt-editor">
          <el-input
            v-model="formData.systemPrompt"
            type="textarea"
            :rows="20"
            placeholder="请输入系统提示词，定义智能体的角色、能力和行为规范..."
            resize="none"
          />
          <div class="prompt-actions">
            <el-button size="small" @click="insertTemplate('role')">插入角色模板</el-button>
            <el-button size="small" @click="insertTemplate('constraint')">插入约束模板</el-button>
            <el-button size="small" @click="insertTemplate('format')">插入格式模板</el-button>
          </div>
        </div>
      </div>

      <!-- 中间：配置 -->
      <div class="editor-column middle-column">
        <div class="column-header">
          <h3>配置</h3>
        </div>
        <div class="config-section">
          <el-form :model="formData" label-position="top" size="default">
            <div class="form-group">
              <el-form-item label="名称">
                <el-input v-model="formData.name" placeholder="请输入智能体名称" />
              </el-form-item>
              
              <el-form-item label="描述">
                <el-input
                  v-model="formData.description"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入智能体描述"
                />
              </el-form-item>
            </div>

            <div class="section-divider">
              <span>模型</span>
            </div>

            <div class="form-group">
              <el-form-item label="模型">
                <el-select v-model="formData.model" placeholder="选择模型">
                  <el-option label="GPT-3.5 Turbo" value="gpt-3.5-turbo" />
                  <el-option label="GPT-4" value="gpt-4" />
                  <el-option label="Claude-3" value="claude-3" />
                </el-select>
              </el-form-item>

              <el-form-item label="温度">
                <el-slider
                  v-model="formData.temperature"
                  :min="0"
                  :max="2"
                  :step="0.1"
                  show-input
                  :show-input-controls="false"
                />
              </el-form-item>

              <el-form-item label="最大长度">
                <el-input-number
                  v-model="formData.maxTokens"
                  :min="100"
                  :max="4000"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </div>

            <div class="section-divider">
              <span>对话</span>
            </div>

            <div class="form-group">
              <el-form-item label="开场白">
                <el-input
                  v-model="formData.greeting"
                  placeholder="智能体的第一句话"
                />
              </el-form-item>
            </div>
          </el-form>
        </div>
      </div>

      <!-- 右侧：试运行 -->
      <div class="editor-column right-column">
        <div class="column-header">
          <h3>试运行</h3>
          <el-button size="small" @click="clearChat">清空对话</el-button>
        </div>
        <div class="chat-container">
          <div class="chat-messages" ref="chatContainer">
            <div
              v-for="(message, index) in chatMessages"
              :key="index"
              :class="['message', message.role]"
            >
              <div class="message-content">
                <div class="message-text">{{ message.content }}</div>
                <div class="message-time">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="currentMessage"
              type="textarea"
              :rows="1"
              placeholder="输入消息进行测试..."
              @keydown.ctrl.enter="sendMessage"
              resize="none"
            />
            <el-button
              type="primary"
              @click="sendMessage"
              :loading="isGenerating"
              :disabled="!currentMessage.trim()"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  InfoFilled
} from '@element-plus/icons-vue'
import { getAgent, createAgent, updateAgent } from '@/api/agent'
import type { AgentCreateRequest, AgentUpdateRequest } from '@/api/agent'

const router = useRouter()
const route = useRoute()

const isEdit = ref(false)
const isGenerating = ref(false)
const isSaving = ref(false)
const currentMessage = ref('')
const loading = ref(false)
const agentId = ref<number | null>(null)
const chatMessages = ref<Array<{
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}>>([])
const chatContainer = ref<HTMLDivElement | null>(null)

const formData = reactive({
  name: '',
  description: '',
  systemPrompt: '',
  model: 'gpt-3.5-turbo',
  temperature: 0.7,
  maxTokens: 2000,
  greeting: '你好！我是你的AI助手，有什么可以帮助你的吗？'
})

function goBack() {
  router.push('/agents')
}

async function saveDraft() {
  if (!formData.name.trim()) {
    ElMessage.error('请输入智能体名称')
    return
  }
  
  isSaving.value = true
  try {
    const payload: AgentCreateRequest | AgentUpdateRequest = {
      name: formData.name,
      description: formData.description,
      systemPrompt: formData.systemPrompt,
      userPromptTemplate: '',
      modelConfig: {
        provider: 'openai',
        model: formData.model,
        temperature: formData.temperature,
        maxTokens: formData.maxTokens
      }
    }
    
    if (isEdit.value && agentId.value) {
      // 更新草稿
      await updateAgent(agentId.value, payload)
      ElMessage.success('草稿已更新')
    } else {
      // 创建草稿
      const id = await createAgent(payload as AgentCreateRequest)
      agentId.value = id
      isEdit.value = true
      // 更新URL为编辑模式
      router.replace(`/agents/editor/${id}`)
      ElMessage.success('草稿已保存')
    }
  } catch (error) {
    console.error('保存草稿失败', error)
    ElMessage.error('保存失败')
  } finally {
    isSaving.value = false
  }
}

async function publishAgent() {
  if (!formData.name.trim()) {
    ElMessage.error('请输入智能体名称')
    return
  }
  if (!formData.systemPrompt.trim()) {
    ElMessage.error('请输入系统提示词')
    return
  }
  
  isSaving.value = true
  try {
    const payload: AgentCreateRequest | AgentUpdateRequest = {
      name: formData.name,
      description: formData.description,
      systemPrompt: formData.systemPrompt,
      userPromptTemplate: '',
      modelConfig: {
        provider: 'openai',
        model: formData.model,
        temperature: formData.temperature,
        maxTokens: formData.maxTokens
      }
    }
    
    if (isEdit.value && agentId.value) {
      // 更新现有智能体
      await updateAgent(agentId.value, payload)
      ElMessage.success('智能体已更新')
    } else {
      // 创建新智能体
      await createAgent(payload as AgentCreateRequest)
      ElMessage.success('智能体发布成功')
    }
    
    router.push('/agents')
  } catch (error) {
    console.error('发布失败', error)
    ElMessage.error('发布失败')
  } finally {
    isSaving.value = false
  }
}

function insertTemplate(type: string) {
  const templates = {
    role: '你是一个专业的AI助手，具有丰富的知识和经验。',
    constraint: '请确保回答准确、有用，并遵循道德准则。',
    format: '请使用清晰、结构化的方式回答问题。'
  }
  
  if (formData.systemPrompt) {
    formData.systemPrompt += '\n\n' + templates[type as keyof typeof templates]
  } else {
    formData.systemPrompt = templates[type as keyof typeof templates]
  }
}

function clearChat() {
  chatMessages.value.splice(0, chatMessages.value.length)
  // 重新添加开场白
  if (formData.greeting) {
    chatMessages.value.push({
      role: 'assistant',
      content: formData.greeting,
      timestamp: Date.now()
    })
  }
}

async function sendMessage() {
  if (!currentMessage.value.trim()) return
  
  const userMessage = {
    role: 'user' as const,
    content: currentMessage.value,
    timestamp: Date.now()
  }
  
  chatMessages.value.push(userMessage)
  const userInput = currentMessage.value
  currentMessage.value = ''
  
  await nextTick()
  scrollToBottom()
  
  isGenerating.value = true
  
  // 模拟AI回复 - 根据系统提示词生成回复
  setTimeout(() => {
    const responses = generateMockResponse(userInput, formData.systemPrompt)
    
    const assistantMessage = {
      role: 'assistant' as const,
      content: responses,
      timestamp: Date.now()
    }
    
    chatMessages.value.push(assistantMessage)
    isGenerating.value = false
    
    nextTick(() => {
      scrollToBottom()
    })
  }, 800)
}

function generateMockResponse(userInput: string, systemPrompt: string): string {
  const lowerInput = userInput.toLowerCase()
  
  // 如果有系统提示词，基于系统提示词的角色回复
  if (systemPrompt.trim()) {
    // 检测系统提示词中的角色特征
    const hasRole = systemPrompt.match(/你是(?:一个|一位)?(.{2,10}?)(?:[，,。.]|$)/)
    const roleName = hasRole ? hasRole[1] : '智能助手'
    
    // 根据用户输入匹配不同回复
    if (lowerInput.includes('你好') || lowerInput.includes('hello') || lowerInput.includes('hi')) {
      return `你好！我是${roleName}，很高兴为你服务。有什么我可以帮助你的吗？`
    }
    
    if (lowerInput.includes('你是谁') || lowerInput.includes('介绍')) {
      return `我是${roleName}。${systemPrompt.slice(0, 100)}${systemPrompt.length > 100 ? '...' : ''}`
    }
    
    if (lowerInput.includes('帮') || lowerInput.includes('能做') || lowerInput.includes('功能')) {
      return `作为${roleName}，我可以根据你的需求提供专业的帮助和建议。请告诉我具体需要什么协助，我会尽力为你解答。`
    }
    
    if (lowerInput.includes('谢谢') || lowerInput.includes('感谢')) {
      return `不客气！很高兴能够帮到你。如果还有其他问题，随时告诉我。`
    }
    
    // 默认回复
    return `作为${roleName}，我理解你说的是"${userInput}"。根据我的设定，我会尽力为你提供有价值的回答。请问还有什么具体问题吗？`
  }
  
  // 没有系统提示词时的通用回复
  if (lowerInput.includes('你好') || lowerInput.includes('hello') || lowerInput.includes('hi')) {
    return '你好！我是智能助手，请先设置系统提示词来定义我的角色和能力。'
  }
  
  return `收到你的消息："${userInput}"。请先在左侧配置系统提示词，这样我就能以特定的角色和方式为你服务了。`
}

function scrollToBottom() {
  const container = chatContainer.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleTimeString()
}

onMounted(async () => {
  const routeAgentId = route.params.id
  // 只有当ID存在且是有效数字时才进入编辑模式
  if (routeAgentId && !isNaN(Number(routeAgentId))) {
    // 编辑模式：从后端加载数据
    isEdit.value = true
    agentId.value = Number(routeAgentId)
    loading.value = true
    try {
      const agent = await getAgent(agentId.value)
      if (agent) {
        // 填充表单数据
        formData.name = agent.name || ''
        formData.description = agent.description || ''
        formData.systemPrompt = agent.systemPrompt || ''
        formData.model = agent.modelConfig?.model || 'gpt-3.5-turbo'
        formData.temperature = agent.modelConfig?.temperature ?? 0.7
        formData.maxTokens = agent.modelConfig?.maxTokens ?? 2000
        
        // 添加开场白到聊天
        if (formData.greeting) {
          chatMessages.value.push({
            role: 'assistant',
            content: formData.greeting,
            timestamp: Date.now()
          })
        }
      }
    } catch (error) {
      console.error('加载智能体数据失败', error)
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  } else {
    // 新建模式：使用默认值，添加默认开场白
    isEdit.value = false
    agentId.value = null
    if (formData.greeting) {
      chatMessages.value.push({
        role: 'assistant',
        content: formData.greeting,
        timestamp: Date.now()
      })
    }
  }
})
</script>

<style scoped>
.editor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-left h1 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.header-right {
  display: flex;
  gap: var(--spacing-sm);
}

.editor-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  overflow: hidden;
}

.editor-column {
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  background: white;
}

.column-header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

/* 左侧：系统提示词 */
.prompt-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--spacing-lg);
}

.prompt-editor :deep(.el-textarea) {
  flex: 1;
}

.prompt-editor :deep(.el-textarea__inner) {
  height: 100% !important;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.prompt-actions {
  display: flex;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

/* 中间：配置 */
.config-section {
  flex: 1;
  padding: var(--spacing-lg);
  overflow-y: auto;
}

.config-section :deep(.el-form-item) {
  margin-bottom: var(--spacing-md);
}

.config-section :deep(.el-form-item__label) {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-xs);
}

.config-section :deep(.el-input__inner),
.config-section :deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
}

.config-section :deep(.el-select) {
  width: 100%;
}

.form-group {
  margin-bottom: var(--spacing-lg);
}

.section-divider {
  margin: var(--spacing-lg) 0;
  padding: var(--spacing-xs) 0;
  border-bottom: 2px solid var(--border-color);
}

.section-divider span {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 右侧：试运行 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}

.chat-messages {
  flex: 1;
  padding: var(--spacing-lg);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  background: #f8f9fa;
}

.message {
  display: flex;
  gap: var(--spacing-sm);
  max-width: 70%;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  align-self: flex-end;
}

.message.assistant {
  align-self: flex-start;
}

.message-content {
  flex: 1;
}

.message-text {
  background: white;
  padding: 8px 12px;
  border-radius: 12px;
  margin-bottom: 4px;
  word-wrap: break-word;
  line-height: 1.5;
  font-size: var(--font-size-sm);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  color: var(--text-primary);
}

.message.user .message-text {
  background: var(--color-primary);
  color: white;
  border-radius: 12px 12px 2px 12px;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
}

.message.assistant .message-text {
  border-radius: 12px 12px 12px 2px;
}

.message-time {
  font-size: 11px;
  color: var(--text-tertiary);
  padding-left: 4px;
}

.message.user .message-time {
  text-align: right;
  padding-left: 0;
  padding-right: 4px;
}

.chat-input {
  padding: var(--spacing-md);
  border-top: 1px solid var(--border-color);
  display: flex;
  gap: var(--spacing-sm);
  align-items: flex-end;
  background: white;
}

.chat-input :deep(.el-textarea) {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  resize: none;
  padding: 8px 12px;
  font-size: var(--font-size-sm);
  line-height: 1.5;
  min-height: 36px;
}

.chat-input :deep(.el-textarea__inner):focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.chat-input :deep(.el-button) {
  border-radius: var(--radius-lg);
  padding: 8px 16px;
  font-weight: var(--font-weight-medium);
  min-width: 60px;
  height: 36px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .editor-content {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr 1fr;
  }
  
  .editor-column {
    min-height: 400px;
  }
}
</style>
