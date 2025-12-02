<template>
  <div class="plugin-detail-page">
    <!-- 顶部导航 -->
    <div class="detail-nav flex items-center justify-between p-lg">
      <el-button @click="goBack" text size="large">
        <el-icon class="mr-xs"><ArrowLeft /></el-icon>
        返回插件列表
      </el-button>
      <div class="flex gap-md">
        <el-button @click="handleValidate" size="large">
          <el-icon class="mr-xs"><CircleCheck /></el-icon>
          验证规范
        </el-button>
        <el-button type="primary" @click="handleSave" :loading="saving" size="large">
          <el-icon class="mr-xs"><Check /></el-icon>
          保存更改
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="detail-content p-lg" v-loading="loading">
      <!-- 标题栏 -->
      <div class="detail-header mb-xl">
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-3xl font-bold mb-sm">{{ plugin?.name || '插件详情' }}</h1>
            <p class="text-muted">{{ plugin?.description || '暂无描述' }}</p>
          </div>
          <div class="flex gap-sm">
            <el-tag 
              :type="plugin?.status === 'enabled' ? 'success' : 'info'"
              size="large"
            >
              {{ plugin?.status === 'enabled' ? '✓ 已启用' : '○ 已禁用' }}
            </el-tag>
            <el-tag 
              :type="plugin?.type === 'builtin' ? '' : 'warning'"
              size="large"
            >
              {{ plugin?.type === 'builtin' ? '内置插件' : '自定义插件' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 表单区域 -->
      <el-form :model="form" label-width="140px" label-position="left" size="large">
        <!-- 基本信息 -->
        <section class="form-section card shadow-card rounded-lg p-xl mb-lg">
          <h2 class="section-title">📋 基本信息</h2>
          
          <el-form-item label="插件名称" required>
            <el-input 
              v-model="form.name" 
              placeholder="为您的插件起个名字" 
              maxlength="50"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="插件描述">
            <el-input 
              v-model="form.description" 
              type="textarea" 
              :rows="2" 
              placeholder="描述插件的功能和用途"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="插件类型">
                <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
                  <el-option label="内置插件" value="builtin">
                    <span class="flex items-center">
                      <el-icon class="mr-sm"><Box /></el-icon>
                      内置插件
                    </span>
                  </el-option>
                  <el-option label="自定义插件" value="custom">
                    <span class="flex items-center">
                      <el-icon class="mr-sm"><Edit /></el-icon>
                      自定义插件
                    </span>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="启用状态">
                <el-switch 
                  v-model="form.enabled" 
                  active-text="启用" 
                  inactive-text="禁用"
                  size="large"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <!-- OpenAPI 规范 -->
        <section class="form-section card shadow-card rounded-lg p-xl mb-lg">
          <div class="flex items-center justify-between mb-lg">
            <h2 class="section-title mb-0">🔌 OpenAPI 规范</h2>
            <el-button @click="handleFormat" text>
              <el-icon class="mr-xs"><Document /></el-icon>
              格式化 JSON
            </el-button>
          </div>
          
          <el-form-item>
            <el-input
              v-model="form.openapiSpec"
              type="textarea"
              :rows="20"
              placeholder="粘贴 OpenAPI 3.0 规范 JSON 内容..."
              class="spec-input"
            />
            <div class="form-tip">
              💡 提示：插件需要符合 OpenAPI 3.0 规范，<a href="#" class="text-primary">查看规范文档</a>
            </div>
          </el-form-item>

          <el-alert
            v-if="validationError"
            type="error"
            :title="validationError"
            :closable="false"
            show-icon
            class="mt-md"
          />

          <el-alert
            v-if="validationSuccess"
            type="success"
            title="✓ 规范验证通过"
            :closable="false"
            show-icon
            class="mt-md"
          />
        </section>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, 
  CircleCheck, 
  Check,
  Box,
  Edit,
  Document
} from '@element-plus/icons-vue'
import { getPluginById, updatePlugin } from '@/api/plugin'

const router = useRouter()
const route = useRoute()
const pluginId = route.params.id as string

const plugin = ref<any>(null)
const loading = ref(false)
const saving = ref(false)
const validationError = ref('')
const validationSuccess = ref(false)

const form = reactive({
  name: '',
  description: '',
  type: 'custom' as 'builtin' | 'custom',
  enabled: false,
  openapiSpec: ''
})

function goBack() {
  router.push('/plugins')
}

async function loadPlugin() {
  loading.value = true
  try {
    const data = await getPluginById(pluginId)
    plugin.value = data
    
    // 填充表单
    form.name = data.name || ''
    form.description = data.description || ''
    form.type = data.type || 'custom'
    form.enabled = data.status === 'enabled'
    form.openapiSpec = data.openapiSpec ? JSON.stringify(data.openapiSpec, null, 2) : ''
  } catch (error) {
    console.error('加载插件失败:', error)
    ElMessage.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  // 验证必填字段
  if (!form.name?.trim()) {
    ElMessage.warning('请输入插件名称')
    return
  }

  // 验证 OpenAPI 规范
  if (form.openapiSpec) {
    try {
      JSON.parse(form.openapiSpec)
    } catch {
      ElMessage.warning('OpenAPI 规范格式错误，请检查 JSON 格式')
      return
    }
  }

  saving.value = true
  try {
    await updatePlugin(pluginId, {
      name: form.name,
      description: form.description,
      type: form.type,
      status: form.enabled ? 'enabled' : 'disabled',
      openapiSpec: form.openapiSpec ? JSON.parse(form.openapiSpec) : null
    })
    ElMessage.success('✓ 保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

function handleValidate() {
  validationError.value = ''
  validationSuccess.value = false

  if (!form.openapiSpec?.trim()) {
    validationError.value = '请输入 OpenAPI 规范内容'
    return
  }

  try {
    const spec = JSON.parse(form.openapiSpec)
    
    // 基本验证
    if (!spec.openapi || !spec.openapi.startsWith('3.')) {
      validationError.value = '规范版本必须是 OpenAPI 3.x'
      return
    }
    
    if (!spec.info || !spec.info.title) {
      validationError.value = '缺少必需的 info.title 字段'
      return
    }

    if (!spec.paths || Object.keys(spec.paths).length === 0) {
      validationError.value = '至少需要定义一个 API 路径'
      return
    }

    validationSuccess.value = true
    ElMessage.success('✓ 规范验证通过')
  } catch (error) {
    validationError.value = `JSON 解析错误: ${error instanceof Error ? error.message : '未知错误'}`
  }
}

function handleFormat() {
  if (!form.openapiSpec?.trim()) {
    ElMessage.warning('请先输入 OpenAPI 规范内容')
    return
  }

  try {
    const spec = JSON.parse(form.openapiSpec)
    form.openapiSpec = JSON.stringify(spec, null, 2)
    ElMessage.success('✓ 格式化成功')
  } catch {
    ElMessage.error('JSON 格式错误，无法格式化')
  }
}

onMounted(() => {
  loadPlugin()
})
</script>

<style scoped>
.plugin-detail-page {
  min-height: 100vh;
  background: var(--color-bg-page);
}

.detail-nav {
  background: white;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.detail-content {
  max-width: 1200px;
  margin: 0 auto;
}

.detail-header h1 {
  color: var(--color-text-primary);
  line-height: 1.2;
}

.form-section {
  background: white;
  transition: all 0.2s ease;
}

.form-section:hover {
  box-shadow: var(--shadow-lg);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 2px solid var(--color-border);
}

.spec-input :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #2c3e50;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.form-tip a {
  text-decoration: none;
}

.form-tip a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-nav {
    padding: var(--spacing-md);
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .detail-content {
    padding: var(--spacing-md);
  }

  .form-section {
    padding: var(--spacing-md) !important;
  }

  :deep(.el-form-item__label) {
    width: 100px !important;
  }
}
</style>
