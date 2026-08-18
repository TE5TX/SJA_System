<template>
  <div :class="['mobile-action-bar', { 'mobile-action-bar--mobile': isMobile }]">
    <slot />
  </div>
</template>

<script setup>
import { useResponsive } from '@/composables/useResponsive'

const { isMobile } = useResponsive()
</script>

<style scoped>
.mobile-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-action-bar--mobile {
  position: sticky;
  z-index: 20;
  left: 0;
  right: 0;
  bottom: 0;
  justify-content: flex-end;
  margin: 16px -16px -16px;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid #ebeef5;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -4px 14px rgba(1, 41, 112, 0.08);
  backdrop-filter: blur(8px);
}

.mobile-action-bar--mobile :deep(.el-button) {
  min-height: 44px;
}
</style>

