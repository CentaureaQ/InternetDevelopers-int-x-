<template>
  <div class="page-header">
    <div class="header-content">
      <div class="header-main">
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-xl);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-main {
  flex: 1;
}

.page-title {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.page-subtitle {
  margin: var(--spacing-xs) 0 0 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}
</style>
