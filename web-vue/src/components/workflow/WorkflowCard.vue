<template>
  <Card
    :title="workflow.name"
    :description="descriptionText"
    :icon="Share"
    :icon-gradient="`linear-gradient(135deg, ${GRADIENTS.workflow[0]} 0%, ${GRADIENTS.workflow[1]} 100%)`"
    :meta="metaItems"
    :clickable="true"
    :show-menu="true"
    @click="$emit('view', workflow)"
  >
    <template #menu-items>
      <el-dropdown-item @click="$emit('edit', workflow)">
        <el-icon><Edit /></el-icon>
        编辑
      </el-dropdown-item>
      <el-dropdown-item @click="$emit('publish', workflow)">
        <el-icon><Upload /></el-icon>
        {{ workflow.status === 'published' ? '取消发布' : '发布' }}
      </el-dropdown-item>
      <el-dropdown-item divided @click="$emit('delete', workflow)">
        <el-icon><Delete /></el-icon>
        删除
      </el-dropdown-item>
    </template>
  </Card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Share, Edit, Upload, Delete, Clock, Grid } from '@element-plus/icons-vue'
import Card from '@/components/common/Card.vue'
import type { WorkflowVO } from '@/api/workflow'

const GRADIENTS = {
  workflow: ['#10b981', '#059669']
}

const props = defineProps<{
  workflow: WorkflowVO
}>()

defineEmits<{
  view: [workflow: WorkflowVO]
  edit: [workflow: WorkflowVO]
  publish: [workflow: WorkflowVO]
  delete: [workflow: WorkflowVO]
}>()

const descriptionText = computed(() => {
  const desc = props.workflow.description?.trim()
  if (desc) return desc
  return '暂无描述'
})

const metaItems = computed(() => [
  { icon: Clock, label: new Date(props.workflow.updatedAt).toLocaleDateString() },
  { icon: Grid, label: props.workflow.status === 'published' ? '已发布' : '草稿' }
])
</script>

<style scoped>
</style>
