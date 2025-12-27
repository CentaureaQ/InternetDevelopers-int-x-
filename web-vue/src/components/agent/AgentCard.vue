<template>
  <Card
    :title="agent.name"
    :description="descriptionText"
    :icon="Cpu"
    :icon-gradient="`linear-gradient(135deg, ${GRADIENTS.agent[0]} 0%, ${GRADIENTS.agent[1]} 100%)`"
    :meta="metaItems"
    :clickable="true"
    :show-menu="true"
    @click="$emit('view', agent)"
  >
    <template #menu-items>
      <el-dropdown-item @click="$emit('edit', agent)">
        <el-icon><Edit /></el-icon>
        编辑
      </el-dropdown-item>
      <el-dropdown-item @click="$emit('publish', agent)">
        <el-icon><Upload /></el-icon>
        {{ agent.status === 'published' ? '取消发布' : '发布' }}
      </el-dropdown-item>
      <el-dropdown-item divided @click="$emit('delete', agent)">
        <el-icon><Delete /></el-icon>
        删除
      </el-dropdown-item>
    </template>
  </Card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Cpu, Edit, Upload, Delete, Clock, Grid } from '@element-plus/icons-vue'
import Card from '@/components/common/Card.vue'
import type { AgentVO } from '@/api/agent'

const GRADIENTS = {
  agent: ['#6366f1', '#4f46e5']
}

const props = defineProps<{
  agent: AgentVO
}>()

defineEmits<{
  view: [agent: AgentVO]
  edit: [agent: AgentVO]
  publish: [agent: AgentVO]
  delete: [agent: AgentVO]
}>()

const descriptionText = computed(() => {
  const desc = props.agent.description?.trim()
  if (desc) return desc
  const fallback = props.agent.systemPrompt?.slice(0, 60) || ''
  return fallback ? `${fallback}${fallback.length === 60 ? '…' : ''}` : '暂无描述'
})

const metaItems = computed(() => [
  { icon: Clock, label: new Date(props.agent.updatedAt).toLocaleDateString() },
  { icon: Grid, label: props.agent.status === 'published' ? '已发布' : '草稿' }
])
</script>

<style scoped>
</style>
