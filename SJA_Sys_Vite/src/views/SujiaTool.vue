<template>
  <div class="sujia-tool">
    <div class="page-title">
      <h1>素拓名单</h1>
      <p>输入姓名后查询姓名和学号，并导出 Excel 名单。</p>
    </div>

    <el-row :gutter="20">
      <el-col :lg="12" :md="24">
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

      <el-col :lg="12" :md="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>解析结果</span>
              <el-tag type="primary">{{ rows.length }} 人</el-tag>
            </div>
          </template>
          <el-table :data="rows" border height="390">
            <el-table-column prop="index" label="#" width="70" />
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="studentNo" label="学号" min-width="150" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.matched ? 'success' : 'danger'">{{ row.matched ? '已匹配' : '未找到' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
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

@media (max-width: 768px) {
  .actions .el-button {
    width: 100%;
  }
}
</style>
