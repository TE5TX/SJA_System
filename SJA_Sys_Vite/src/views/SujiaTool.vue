<template>
  <div class="sujia-tool">
    <div class="page-title">
      <h1>素拓名单</h1>
      <p>粘贴姓名后生成可用 Excel 打开的名单文件。</p>
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
            <el-input-number v-model="duration" :min="0" :precision="1" controls-position="right" />
            <el-button type="primary" :icon="DocumentChecked" @click="parseNames">解析名单</el-button>
            <el-button type="success" :icon="Download" :disabled="names.length === 0" @click="downloadCsv">导出名单</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :lg="12" :md="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>解析结果</span>
              <el-tag type="primary">{{ names.length }} 人</el-tag>
            </div>
          </template>
          <el-table :data="rows" border height="390">
            <el-table-column prop="index" label="#" width="70" />
            <el-table-column prop="studentNo" label="学号" min-width="130" />
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="duration" label="时长" width="100" />
            <el-table-column prop="remark" label="备注" min-width="120" />
          </el-table>
          <el-empty v-if="names.length === 0" description="暂无名单" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { DocumentChecked, Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const rawText = ref('')
const names = ref([])
const duration = ref(1)

const rows = computed(() => names.value.map((name, index) => ({
  index: index + 1,
  studentNo: '',
  name,
  duration: duration.value,
  remark: ''
})))

const parseNames = () => {
  const list = rawText.value
    .split(/[\s,，、;；]+/)
    .map(item => item.trim())
    .filter(Boolean)
  names.value = [...new Set(list)]
  ElMessage.success(`已解析 ${names.value.length} 人`)
}

const clearInput = () => {
  rawText.value = ''
  names.value = []
}

const csvCell = value => `"${String(value ?? '').replace(/"/g, '""')}"`

const downloadCsv = () => {
  const header = ['学号', '姓名', '时长', '备注']
  const lines = [header, ...rows.value.map(row => [row.studentNo, row.name, row.duration, row.remark])]
  const csv = '\ufeff' + lines.map(line => line.map(csvCell).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `素拓名单_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  window.URL.revokeObjectURL(url)
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

.actions :deep(.el-input-number) {
  width: 140px;
}

@media (max-width: 768px) {
  .actions .el-button,
  .actions :deep(.el-input-number) {
    width: 100%;
  }
}
</style>
