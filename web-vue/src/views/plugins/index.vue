<template>
  <div class="page-container">
    <PageHeader title="插件" subtitle="注册和管理您的 API 插件">
      <template #actions>
        <el-button type="primary" @click="showDialog = true" class="register-btn">
          <el-icon><Plus /></el-icon>
          注册插件
        </el-button>
      </template>
    </PageHeader>

    <GridContainer v-loading="loading">
      <Card is-add add-text="注册新插件" @click="showDialog = true" />

      <PluginCard
        v-for="plugin in plugins"
        :key="plugin.id"
        :plugin="plugin"
        @view="handleView"
        @edit="handleEdit"
        @toggle="handleToggle"
        @delete="handleDelete"
      />
    </GridContainer>

    <el-dialog
      v-model="showDialog"
      :title="editingPlugin ? '编辑插件' : '注册插件'"
      width="1100px"
      top="5vh"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      class="plugin-dialog"
      @close="resetForm"
    >
      <div class="dialog-content">
        <div class="dialog-header">
          <div class="dialog-icon">
            <el-icon><Connection /></el-icon>
          </div>
          <div class="dialog-title-text">
            <h3>{{ editingPlugin ? '编辑插件' : '注册插件' }}</h3>
            <p class="dialog-subtitle">{{ editingPlugin ? '修改插件的配置信息' : '通过 OpenAPI 规范快速集成外部 API' }}</p>
          </div>
          <button class="close-btn" @click="showDialog = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="form-container">
          <el-form :model="formData" label-position="top" class="plugin-form">
            <el-form-item label="插件名称" required>
              <el-input 
                v-model="formData.name" 
                placeholder="给插件起个名字，例如：天气查询" 
                size="large"
                class="modern-input"
              />
            </el-form-item>
            
            <el-form-item label="插件描述">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="2"
                placeholder="简要描述插件的功能"
                class="modern-textarea"
                resize="none"
              />
            </el-form-item>
            
            <el-form-item label="OpenAPI 规范" required>
              <div class="openapi-wrapper">
                <el-input
                  v-model="formData.openapiSpec"
                  type="textarea"
                  :rows="10"
                  placeholder='在此粘贴 OpenAPI 3.0 JSON 规范...'
                  class="openapi-input modern-textarea"
                  resize="none"
                />
                <div class="openapi-tools">
                  <el-button link type="primary" size="small" @click="formatJson">
                    <el-icon class="mr-1"><MagicStick /></el-icon>
                    格式化 JSON
                  </el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button class="cancel-btn" @click="showDialog = false">取消</el-button>
          <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleSubmit">
            {{ editingPlugin ? '保存' : '注册' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Connection, Close, MagicStick } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GridContainer from '@/components/common/GridContainer.vue'
import Card from '@/components/common/Card.vue'
import PluginCard from '@/components/plugin/PluginCard.vue'
import { 
  listPlugins, 
  createPlugin, 
  updatePlugin, 
  deletePlugin as deletePluginApi,
  enablePlugin,
  disablePlugin
} from '@/api/plugin'

const router = useRouter()

interface Plugin {
  id: number
  name: string
  description?: string
  type: 'builtin' | 'custom'
  status: 'enabled' | 'disabled'
  openapiSpec: string
  config: string
  createdAt?: string
  updatedAt?: string
}

const plugins = ref<Plugin[]>([])
const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const editingPlugin = ref<Plugin | undefined>()

const formData = ref({
  name: '',
  description: '',
  openapiSpec: '',
  config: '{}',
  status: 'disabled' as 'enabled' | 'disabled'
})

function formatJson() {
  try {
    const parsed = JSON.parse(formData.value.openapiSpec)
    formData.value.openapiSpec = JSON.stringify(parsed, null, 2)
    ElMessage.success('JSON 格式化成功')
  } catch (error) {
    ElMessage.error('JSON 格式不正确，无法格式化')
  }
}

async function loadPlugins() {
  loading.value = true
  try {
    const response = await listPlugins()
    plugins.value = response.data || response as any
  } catch (error) {
    console.error('加载插件列表失败', error)
    ElMessage.error('加载插件列表失败')
  } finally {
    loading.value = false
  }
}

function handleView(plugin: Plugin) {
  router.push(`/plugins/${plugin.id}`)
}

function handleEdit(plugin: Plugin) {
  editingPlugin.value = plugin
  formData.value = {
    name: plugin.name,
    description: plugin.description || '',
    openapiSpec: plugin.openapiSpec || '',
    config: plugin.config || '{}',
    status: plugin.status
  }
  showDialog.value = true
}

async function handleSubmit() {
  if (!formData.value.name) {
    ElMessage.warning('请填写插件名称')
    return
  }

  submitting.value = true
  try {
    if (editingPlugin.value) {
      await updatePlugin(editingPlugin.value.id, formData.value)
      ElMessage.success('保存成功')
    } else {
      await createPlugin(formData.value)
      ElMessage.success('注册成功')
    }
    showDialog.value = false
    resetForm()
    await loadPlugins()
  } catch (error) {
    console.error('操作失败', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleToggle(plugin: Plugin) {
  try {
    if (plugin.status === 'enabled') {
      await disablePlugin(plugin.id)
      ElMessage.success('插件已禁用')
    } else {
      await enablePlugin(plugin.id)
      ElMessage.success('插件已启用')
    }
    await loadPlugins()
  } catch (error) {
    console.error('切换插件状态失败', error)
    ElMessage.error('操作失败')
  }
}

function handleDelete(plugin: Plugin) {
  ElMessageBox.confirm('确定要删除该插件吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await deletePluginApi(plugin.id)
        ElMessage.success('删除成功')
        await loadPlugins()
      } catch (error) {
        console.error('删除失败', error)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

function resetForm() {
  editingPlugin.value = undefined
  formData.value = {
    name: '',
    description: '',
    openapiSpec: '',
    config: '{}',
    status: 'disabled'
  }
}

onMounted(() => {
  loadPlugins()
})
</script>

<style scoped>
.page-container {
  padding: var(--spacing-xl);
  max-width: 1600px;
  margin: 0 auto;
}

.register-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  padding: 12px 24px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

:deep(.plugin-dialog) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

:deep(.plugin-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
  display: none; /* Hide default header */
}

:deep(.plugin-dialog .el-dialog__body) {
  padding: 0;
}

:deep(.plugin-dialog .el-dialog__footer) {
  padding: 0;
  margin: 0;
}

.dialog-content {
  background: #ffffff;
}

.dialog-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 32px;
  background: #ffffff;
  border-bottom: 1px solid #f3f4f6;
}

.dialog-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  border-radius: 12px;
  color: #3b82f6;
  font-size: 24px;
  flex-shrink: 0;
}

.dialog-title-text {
  flex: 1;
}

.dialog-title-text h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  line-height: 1.4;
}

.dialog-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.close-btn {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #4b5563;
}

.form-container {
  padding: 32px;
}

.plugin-form {
  max-width: 100%;
}

.plugin-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #374151;
  padding-bottom: 8px;
}

.modern-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e5e7eb inset;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.modern-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.modern-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #3b82f6 inset;
}

.modern-textarea :deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px #e5e7eb inset;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-family: inherit;
}

.modern-textarea :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.modern-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px #3b82f6 inset;
}

.openapi-wrapper {
  position: relative;
  width: 100%;
}

.openapi-input {
  width: 100%;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

.openapi-tools {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 32px;
  background: #f9fafb;
  border-top: 1px solid #f3f4f6;
}

.cancel-btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
}

.submit-btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 500;
  background: #3b82f6;
  border-color: #3b82f6;
}

.submit-btn:hover {
  background: #2563eb;
  border-color: #2563eb;
}
</style>
