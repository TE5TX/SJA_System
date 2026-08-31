<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="isMobile ? undefined : desktopWidth"
    :fullscreen="isMobile"
    class="responsive-dialog"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <slot />
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-dialog>
</template>

<script setup>
import { useResponsive } from '@/composables/useResponsive'

defineOptions({ inheritAttrs: false })

defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  desktopWidth: { type: String, default: '720px' }
})

defineEmits(['update:modelValue'])

const { isMobile } = useResponsive()
</script>

<style>
/* Element Plus 会把 Dialog Teleport 到 body，因此这里不能使用 scoped。 */
.responsive-dialog:not(.is-fullscreen) .el-dialog__body {
  max-height: min(68vh, 720px);
  overflow-y: auto;
}

.responsive-dialog.is-fullscreen {
  display: flex;
  flex-direction: column;
  height: 100dvh;
  margin: 0;
}

.responsive-dialog.is-fullscreen .el-dialog__header {
  flex: 0 0 auto;
  padding: 16px 48px 14px 16px;
  border-bottom: 1px solid #ebeef5;
}

.responsive-dialog.is-fullscreen .el-dialog__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}

.responsive-dialog.is-fullscreen .el-dialog__footer {
  flex: 0 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid #ebeef5;
  background: #fff;
}
</style>

