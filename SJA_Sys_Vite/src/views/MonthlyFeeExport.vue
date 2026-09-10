<template>
  <div class="monthly-fee-export">
    <div class="page-title">
      <h1>月份稿费导出</h1>
      <p>校学生记者团稿费发放名单</p>
    </div>

    <el-card class="export-panel">
      <!-- 202608111715 - 新增了按月份导出全团成员稿费发放名单的页面。 -->
      <el-form label-position="top" class="export-form mobile-form-top-label">
        <el-form-item class="month-field" label="稿费月份范围" required>
          <el-date-picker
            v-model="monthRange"
            type="monthrange"
            value-format="YYYY-MM"
            start-placeholder="开始月份"
            end-placeholder="结束月份"
            range-separator="至"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item class="scope-field" label="统计范围">
          <!-- 202608121123 - 修复了月份稿费导出范围包含未审核通过任务的问题。 -->
          <el-tag type="success">仅已通过</el-tag>
        </el-form-item>
        <el-form-item class="order-field" label="人员顺序">
          <div class="order-list">
            <el-tag effect="plain">主席团</el-tag>
            <el-icon><ArrowRight /></el-icon>
            <el-tag effect="plain">校园小队</el-tag>
            <el-icon><ArrowRight /></el-icon>
            <el-tag effect="plain">各部门部员</el-tag>
          </div>
        </el-form-item>
        <el-form-item class="export-action">
          <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">
            导出时间段稿费名单
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowRight, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportMonthlyFeeSummary } from '@/api/fee'

const currentMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

// 202609100927 - 完成了月份稿费名单按起止月份区间导出的功能。
const monthRange = ref([currentMonth(), currentMonth()])
const exporting = ref(false)

const handleExport = async () => {
  if (!Array.isArray(monthRange.value) || monthRange.value.length !== 2) {
    ElMessage.warning('请选择完整的稿费月份范围')
    return
  }
  const [startMonth, endMonth] = monthRange.value
  exporting.value = true
  try {
    const response = await exportMonthlyFeeSummary({ startMonth, endMonth })
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `校学生记者团稿费发放名单计算（${startMonth}至${endMonth}）.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('时间段稿费名单导出完成')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.export-panel {
  width: 100%;
}

.export-form {
  /* 202608312307 - 桌面端改为横向导出工具栏，避免宽屏页面左侧拥挤。 */
  display: grid;
  grid-template-columns: minmax(320px, 380px) minmax(120px, 150px) minmax(360px, 1fr) auto;
  align-items: end;
  gap: 20px 28px;
}

.export-form :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.export-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 8px;
  line-height: 20px;
}

.month-field :deep(.el-date-editor) {
  width: 100%;
}

.scope-field :deep(.el-form-item__content),
.order-field :deep(.el-form-item__content) {
  min-height: 40px;
  align-items: center;
}

.export-action :deep(.el-form-item__content) {
  justify-content: flex-end;
}

.order-list {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1199px) {
  .export-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-field,
  .export-action {
    grid-column: 1 / -1;
  }

  .export-action :deep(.el-form-item__content) {
    justify-content: flex-start;
  }
}

@media (max-width: 767px) {
  .export-panel {
    width: 100%;
  }

  .export-form {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .month-field,
  .scope-field,
  .order-field,
  .export-action {
    grid-column: 1;
  }

  .export-form .el-button {
    width: 100%;
  }

  .order-list {
    align-items: center;
    flex-direction: row;
    flex-wrap: nowrap;
    gap: 6px;
    overflow-x: auto;
    padding-bottom: 3px;
  }

  .order-list :deep(.el-icon) {
    flex: 0 0 auto;
    transform: none;
  }

  .order-list :deep(.el-tag) {
    flex: 0 0 auto;
  }
}
</style>
