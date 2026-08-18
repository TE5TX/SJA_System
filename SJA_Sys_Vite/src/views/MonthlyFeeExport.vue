<template>
  <div class="monthly-fee-export">
    <div class="page-title">
      <h1>月份稿费导出</h1>
      <p>校学生记者团稿费发放名单</p>
    </div>

    <el-card class="export-panel">
      <!-- 202608111715 - 新增了按月份导出全团成员稿费发放名单的页面。 -->
      <el-form label-width="92px" class="export-form mobile-form-top-label">
        <el-form-item label="稿费月份" required>
          <el-date-picker
            v-model="month"
            type="month"
            value-format="YYYY-MM"
            placeholder="请选择月份"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="统计范围">
          <!-- 202608121123 - 修复了月份稿费导出范围包含未审核通过任务的问题。 -->
          <el-tag type="success">仅已通过</el-tag>
        </el-form-item>
        <el-form-item label="人员顺序">
          <div class="order-list">
            <el-tag effect="plain">主席团</el-tag>
            <el-icon><ArrowRight /></el-icon>
            <el-tag effect="plain">校园小队</el-tag>
            <el-icon><ArrowRight /></el-icon>
            <el-tag effect="plain">各部门部员</el-tag>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">
            导出月份稿费名单
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

const month = ref(currentMonth())
const exporting = ref(false)

const handleExport = async () => {
  if (!month.value) {
    ElMessage.warning('请选择稿费月份')
    return
  }
  exporting.value = true
  try {
    const response = await exportMonthlyFeeSummary({ month: month.value })
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `校学生记者团稿费发放名单计算（${month.value}）.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('月份稿费名单导出完成')
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
  max-width: 760px;
}

.export-form {
  padding: 12px 8px 2px;
}

.order-list {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 767px) {
  .export-panel,
  .export-form :deep(.el-date-editor),
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
