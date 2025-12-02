<template>
  <div class="page-container">
    <PageHeader title="插件" subtitle="注册和管理您的 API 插件">
      <template #actions>
        <el-button type="primary" @click="showDialog = true">
          <el-icon><Plus /></el-icon>
          注册插件
        </el-button>
      </template>
    </PageHeader>

    <GridContainer v-loading="loading">
      <PluginCard
        v-for="plugin in plugins"
        :key="plugin.id"
        :plugin="plugin"
        @view="handleView"
        @edit="handleEdit"
        @toggle="handleToggle"
        @delete="handleDelete"
      />

      <Card is-add add-text="注册新插件" @click="showDialog = true" />
    </GridContainer>

    <el-dialog
      v-model="showDialog"
      :title="editingPlugin ? '编辑插件' : '注册插件'"
      width="700px"
      @close="resetForm"
    >
      <el-form :model="formData" label-width="120px">
        <el-form-item label="插件名称" required>
          <el-input v-model="formData.name" placeholder="请输入插件名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="插件描述"
          />
        </el-form-item>
        <el-form-item label="插件类型">
          <el-select v-model="formData.type" placeholder="选择类型" style="width: 100%">
            <el-option label="内置" value="builtin" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="OpenAPI规范">
          <el-input
            v-model="formData.openapiSpec"
            type="textarea"
            :rows="10"
            placeholder="粘贴OpenAPI 3.0规范JSON内容"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingPlugin ? '保存' : '注册' }}
        </el-button>
      </template>
    </el-dialog>
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
import PluginCard from '@/components/plugin/PluginCard.vue'

const router = useRouter()

interface Plugin {
  id: string
  name: string
  description?: string
  type: 'builtin' | 'custom'
  status: 'enabled' | 'disabled'
  openapiSpec?: string
  createdAt?: string
}

const STORAGE_KEY = 'console_plugins'

const plugins = ref<Plugin[]>([])
const loading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const editingPlugin = ref<Plugin | undefined>()

const formData = ref({
  name: '',
  description: '',
  type: 'custom' as 'builtin' | 'custom',
  openapiSpec: ''
})

function loadPlugins() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw) {
    try {
      plugins.value = JSON.parse(raw)
    } catch (e) {
      console.error('解析插件数据失败', e)
    }
  }
}

function savePlugins() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(plugins.value))
}

function handleView(plugin: Plugin) {
  router.push(`/plugins/${plugin.id}`)
}

function handleEdit(plugin: Plugin) {
  editingPlugin.value = plugin
  formData.value = {
    name: plugin.name,
    description: plugin.description || '',
    type: plugin.type,
    openapiSpec: plugin.openapiSpec || ''
  }
  showDialog.value = true
}

function handleSubmit() {
  if (!formData.value.name) {
    ElMessage.warning('请填写插件名称')
    return
  }

  submitting.value = true
  try {
    if (editingPlugin.value) {
      const index = plugins.value.findIndex(p => p.id === editingPlugin.value!.id)
      if (index !== -1) {
        plugins.value[index] = {
          ...plugins.value[index],
          ...formData.value
        }
      }
    } else {
      plugins.value.push({
        id: `plg_${Date.now()}`,
        ...formData.value,
        status: 'disabled',
        createdAt: new Date().toISOString()
      })
    }
    savePlugins()
    ElMessage.success(editingPlugin.value ? '保存成功' : '注册成功')
    showDialog.value = false
    resetForm()
  } catch (error) {
    console.error('操作失败', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

function handleToggle(plugin: Plugin) {
  plugin.status = plugin.status === 'enabled' ? 'disabled' : 'enabled'
  savePlugins()
  ElMessage.success(`插件已${plugin.status === 'enabled' ? '启用' : '禁用'}`)
}

function handleDelete(plugin: Plugin) {
  ElMessageBox.confirm('确定要删除该插件吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      const index = plugins.value.findIndex(p => p.id === plugin.id)
      if (index !== -1) {
        plugins.value.splice(index, 1)
        savePlugins()
        ElMessage.success('删除成功')
      }
    })
    .catch(() => {})
}

function resetForm() {
  editingPlugin.value = undefined
  formData.value = {
    name: '',
    description: '',
    type: 'custom',
    openapiSpec: ''
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
</style>
