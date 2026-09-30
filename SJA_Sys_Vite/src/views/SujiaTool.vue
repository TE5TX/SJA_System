<template>
  <div class="sujia-tool">
    <div class="page-title">
      <h1>素拓名单</h1>
      <p>输入姓名后查询姓名和学号，并导出 Excel 名单。</p>
    </div>

    <el-row :gutter="20" class="sujia-grid">
      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>姓名输入</span>
              <el-button :icon="Refresh" @click="clearInput">清空</el-button>
            </div>
          </template>
          <el-input
            v-model="rawText"
            type="textarea"
            :rows="14"
            resize="none"
            placeholder="支持换行、空格、逗号、顿号分隔"
          />
          <div class="actions">
            <el-button type="primary" :icon="DocumentChecked" :loading="loading" @click="parseNames">查询名单</el-button>
            <el-button type="success" :icon="Download" :disabled="rows.length === 0" :loading="exporting" @click="downloadExcel">导出 Excel</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>解析结果</span>
              <el-tag type="primary">{{ rows.length }} 人</el-tag>
            </div>
          </template>
          <!-- 202609291001 - 修复了空名单时表格内置提示与页面空状态重复显示的问题。 -->
          <el-table v-if="rows.length > 0" class="desktop-only" :data="rows" border height="390">
            <el-table-column prop="index" label="#" width="70" />
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="studentNo" label="学号" min-width="150" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.matched ? 'success' : 'danger'">{{ row.matched ? '已匹配' : '未找到' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="rows.length > 0" class="mobile-only sujia-results">
            <article v-for="row in rows" :key="`${row.index}-${row.name}`" class="mobile-record-card">
              <div class="mobile-record-card__header">
                <strong>{{ row.index }}. {{ row.name }}</strong>
                <el-tag :type="row.matched ? 'success' : 'danger'">{{ row.matched ? '已匹配' : '未找到' }}</el-tag>
              </div>
              <div class="sujia-student-no">学号：{{ row.studentNo || '-' }}</div>
            </article>
          </div>
          <el-empty v-if="rows.length === 0" description="暂无名单" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { DocumentChecked, Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportSujiaNames, resolveSujiaNames } from '@/api/sujia'

const rawText = ref('')
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)

const getInputNames = () => rawText.value
  .split(/[\s,，、;；]+/)
  .map(item => item.trim())
  .filter(Boolean)

const parseNames = async () => {
  const names = [...new Set(getInputNames())]
  if (!names.length) {
    ElMessage.warning('请先输入姓名')
    return
  }
  loading.value = true
  try {
    const res = await resolveSujiaNames(names)
    rows.value = (res.data || []).map((item, index) => ({
      index: index + 1,
      name: item.realName,
      studentNo: item.studentNo || '',
      matched: item.matched
    }))
    ElMessage.success(`已查询 ${rows.value.length} 条结果`)
  } finally {
    loading.value = false
  }
}

const clearInput = () => {
  rawText.value = ''
  rows.value = []
}

const downloadExcel = async () => {
  const names = [...new Set(getInputNames())]
  if (!names.length) {
    ElMessage.warning('请先输入姓名')
    return
  }
  exporting.value = true
  try {
    const response = await exportSujiaNames(names)
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `素拓名单_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
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

.card-header,
.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.actions {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin-top: 16px;
}

@media (max-width: 767px) {
  .sujia-grid {
    row-gap: 14px;
  }

  .actions {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }

  .actions .el-button,
  .actions .el-button + .el-button {
    width: 100%;
    margin: 0;
  }

  .sujia-results {
    max-height: 390px;
    overflow-y: auto;
  }

  .sujia-student-no {
    margin-top: 10px;
    color: #606266;
  }
}
</style>
