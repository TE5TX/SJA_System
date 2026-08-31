<template>
  <div class="responsive-pagination">
    <span v-if="isMobile" class="responsive-pagination__total">共 {{ total }} 条</span>
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next'"
      :pager-count="isMobile ? 5 : 7"
      background
      @update:current-page="$emit('update:currentPage', $event)"
      @update:page-size="$emit('update:pageSize', $event)"
      @current-change="$emit('current-change', $event)"
      @size-change="$emit('size-change', $event)"
    />
  </div>
</template>

<script setup>
import { useResponsive } from '@/composables/useResponsive'

defineProps({
  currentPage: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  pageSizes: { type: Array, default: () => [10, 20, 50] },
  total: { type: Number, default: 0 }
})

defineEmits(['update:currentPage', 'update:pageSize', 'current-change', 'size-change'])

const { isMobile } = useResponsive()
</script>

<style scoped>
.responsive-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.responsive-pagination__total {
  flex: 0 0 auto;
  color: #606266;
  font-size: 13px;
}

@media (max-width: 767px) {
  .responsive-pagination {
    justify-content: space-between;
    overflow: hidden;
  }

  .responsive-pagination :deep(.el-pagination) {
    flex: 0 1 auto;
  }
}
</style>
