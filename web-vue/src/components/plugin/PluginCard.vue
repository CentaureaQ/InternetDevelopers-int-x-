<template>
  <Card
    :title="plugin.name"
    :description="plugin.description || '暂无描述'"
    :icon="Connection"
    :icon-gradient="`linear-gradient(135deg, ${GRADIENTS.plugin[0]} 0%, ${GRADIENTS.plugin[1]} 100%)`"
    :meta="metaItems"
    :tags="tagItems"
    :clickable="true"
    :show-menu="true"
    @click="$emit('view', plugin)"
  >
    <template #menu-items>
      <el-dropdown-item @click="$emit('edit', plugin)">
        <el-icon><Edit /></el-icon>
        编辑
      </el-dropdown-item>
      <el-dropdown-item @click="$emit('toggle', plugin)">
        <el-icon><Switch /></el-icon>
        {{ plugin.status === 'enabled' ? '禁用' : '启用' }}
      </el-dropdown-item>
      <el-dropdown-item divided @click="$emit('delete', plugin)">
        <el-icon><Delete /></el-icon>
        删除
      </el-dropdown-item>
    </template>
  </Card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Connection, Edit, Switch, Delete, Grid, Clock } from '@element-plus/icons-vue'
import Card from '@/components/common/Card.vue'

const GRADIENTS = {
  plugin: ['#ec4899', '#db2777']
}

interface Plugin {
  id: number
  name: string
  description?: string
  type: 'builtin' | 'custom'
  status: 'enabled' | 'disabled'
  openapiSpec: string
  config: string
  createdAt?: string
}

const props = defineProps<{
  plugin: Plugin
}>()

defineEmits<{
  view: [plugin: Plugin]
  edit: [plugin: Plugin]
  toggle: [plugin: Plugin]
  delete: [plugin: Plugin]
}>()

const metaItems = computed(() => [
  { icon: Clock, label: formatDate(props.plugin.createdAt) },
  { icon: Grid, label: props.plugin.status === 'enabled' ? '已启用' : '已禁用' },
  { icon: Grid, label: props.plugin.type === 'builtin' ? '内置' : '自定义' }
])

const tagItems = computed(() => [])

function formatDate(date?: string) {
  if (!date) return '未知'
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>
