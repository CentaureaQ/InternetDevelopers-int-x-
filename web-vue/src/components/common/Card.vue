<template>
  <div 
    class="card-item"
    :class="{ 'is-clickable': clickable, 'is-add': isAdd }"
    @click="handleClick"
  >
    <div v-if="!isAdd" class="card-content">
      <div class="card-header">
        <div class="card-icon" :style="iconStyle">
          <slot name="icon">
            <el-icon :size="20"><component :is="icon" /></el-icon>
          </slot>
        </div>
        <div v-if="$slots.menu || showMenu" class="card-menu" @click.stop>
          <slot name="menu">
            <el-dropdown trigger="click">
              <el-icon class="menu-icon"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <slot name="menu-items" />
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </slot>
        </div>
      </div>

      <div class="card-body">
        <h3 class="card-title">{{ title }}</h3>
        <p class="card-description">{{ description }}</p>
        
        <div v-if="$slots.meta || meta?.length" class="card-meta">
          <slot name="meta">
            <div v-for="item in meta" :key="item.label" class="meta-item">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </div>
          </slot>
        </div>

        <div v-if="$slots.footer || tags?.length" class="card-footer">
          <slot name="footer">
            <div class="tag-list">
              <el-tag 
                v-for="tag in tags" 
                :key="tag.label"
                :type="tag.type"
                size="small"
              >
                {{ tag.label }}
              </el-tag>
            </div>
          </slot>
        </div>
      </div>
    </div>

    <div v-else class="add-content">
      <slot name="add-content">
        <el-icon :size="36" color="var(--text-tertiary)"><Plus /></el-icon>
        <p>{{ addText || '新建' }}</p>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { MoreFilled, Plus } from '@element-plus/icons-vue'
import type { Component } from 'vue'

interface MetaItem {
  icon?: Component
  label: string
}

interface TagItem {
  label: string
  type?: 'success' | 'info' | 'warning' | 'danger'
}

const props = defineProps<{
  title?: string
  description?: string
  icon?: Component
  iconGradient?: string
  meta?: MetaItem[]
  tags?: TagItem[]
  clickable?: boolean
  isAdd?: boolean
  addText?: string
  showMenu?: boolean
}>()

const emit = defineEmits<{
  click: []
}>()

const iconStyle = computed(() => {
  if (props.iconGradient) {
    return { background: props.iconGradient }
  }
  return {}
})

function handleClick() {
  if (props.clickable || props.isAdd) {
    emit('click')
  }
}
</script>

<style scoped>
.card-item {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  transition: all var(--transition-base) ease;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  max-width: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.card-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-primary);
  transform: scaleX(0);
  transition: transform var(--transition-base) ease;
}

.card-item.is-clickable {
  cursor: pointer;
}

.card-item.is-clickable:hover {
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
  border-color: var(--color-primary-lighter);
  transform: translateY(-2px);
}

.card-item.is-clickable:hover::before {
  transform: scaleX(1);
}

.card-item.is-add {
  border: 2px dashed var(--border-color-dark);
  background: var(--bg-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-item.is-add::before {
  display: none;
}

.card-item.is-add:hover {
  border-color: var(--color-primary);
  background: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-xs);
  gap: var(--spacing-xs);
}

.card-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.card-menu {
  position: relative;
}

.menu-icon {
  font-size: 18px;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 2px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast) ease;
}

.menu-icon:hover {
  color: var(--text-primary);
  background: var(--bg-tertiary);
}

.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  min-height: 0;
  overflow: hidden;
}

.card-title {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.card-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-tertiary);
  font-size: 11px;
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.card-footer {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding-top: var(--spacing-xs);
  margin-top: auto;
  width: auto;
}

.tag-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  min-height: auto;
  justify-content: flex-end;
}

.tag-list :deep(.el-tag) {
  width: 100%;
  justify-content: center;
  background: #f5f5f5 !important;
  border: 1px solid #e0e0e0 !important;
  color: #666 !important;
  font-weight: normal !important;
  box-shadow: none !important;
  font-size: 11px !important;
  padding: 2px 8px !important;
  height: auto !important;
}

.add-content {
  text-align: center;
}

.add-content p {
  margin-top: var(--spacing-xs);
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}
</style>
