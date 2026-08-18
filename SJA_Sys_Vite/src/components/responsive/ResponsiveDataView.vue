<template>
  <div class="responsive-data-view" v-loading="loading">
    <div v-if="!isMobile" class="responsive-data-view__desktop">
      <slot name="desktop" />
    </div>

    <div v-else class="responsive-data-view__mobile">
      <template v-if="rows.length">
        <slot v-for="row in rows" :key="rowKeyValue(row)" name="mobile" :row="row" />
      </template>
      <el-empty v-else :description="emptyText" />
    </div>
  </div>
</template>

<script setup>
import { useResponsive } from '@/composables/useResponsive'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  rowKey: { type: [String, Function], default: 'id' },
  loading: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' }
})

const { isMobile } = useResponsive()

const rowKeyValue = (row) => (
  typeof props.rowKey === 'function' ? props.rowKey(row) : row?.[props.rowKey]
)
</script>

<style scoped>
.responsive-data-view__mobile {
  display: grid;
  gap: 12px;
  min-height: 120px;
}
</style>

