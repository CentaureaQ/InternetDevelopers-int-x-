<template>
  <div class="grid-container" :class="gridClass">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    cols?: 1 | 2 | 3 | 4
    gap?: 'sm' | 'md' | 'lg'
    minWidth?: string
  }>(),
  {
    cols: 3,
    gap: 'lg',
    minWidth: '340px'
  }
)

const gridClass = computed(() => {
  const classes = []
  if (props.gap) classes.push(`gap-${props.gap}`)
  return classes.join(' ')
})
</script>

<style scoped>
.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 340px), 1fr));
}

.gap-sm {
  gap: var(--spacing-md);
}

.gap-md {
  gap: var(--spacing-lg);
}

.gap-lg {
  gap: var(--spacing-xl);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .grid-container {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .grid-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1025px) and (max-width: 1440px) {
  .grid-container {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1441px) {
  .grid-container {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
