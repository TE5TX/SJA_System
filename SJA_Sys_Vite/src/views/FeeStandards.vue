<!-- 202608101042 - 修复了稿费标准缺少配置管理页面的问题。 -->
<template>
  <div>
    <div class="page-title">
      <h1>稿费标准</h1>
      <p>维护系统自动计算使用的金额、比例和阈值。</p>
    </div>

    <el-card>
      <el-table :data="standards" v-loading="loading" border>
        <el-table-column prop="deptCode" label="业务" width="120" />
        <el-table-column prop="platform" label="平台" width="110" />
        <el-table-column prop="category" label="类别" min-width="220" show-overflow-tooltip />
        <el-table-column prop="description" label="规则" min-width="260" show-overflow-tooltip />
        <el-table-column label="规则值" width="170">
          <template #default="{ row }">
            <el-input-number v-if="editingId === row.id" v-model="editValue" :min="0" :precision="4" :controls="false" />
            <span v-else>{{ formatValue(row.ruleValue) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="editingId === row.id">
              <el-button link type="primary" @click="save(row)">保存</el-button>
              <el-button link @click="cancel">取消</el-button>
            </template>
            <el-button v-else link type="primary" @click="edit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFeeStandards, updateFeeStandard } from '@/api/fee'

const standards = ref([])
const loading = ref(false)
const editingId = ref(null)
const editValue = ref(0)

const load = async () => {
  loading.value = true
  try {
    standards.value = (await getFeeStandards()).data || []
  } finally {
    loading.value = false
  }
}

const edit = row => {
  editingId.value = row.id
  editValue.value = Number(row.ruleValue || 0)
}

const cancel = () => {
  editingId.value = null
}

const save = async row => {
  await updateFeeStandard(row.id, {
    ruleValue: editValue.value,
    description: row.description || '',
    remark: row.remark || ''
  })
  ElMessage.success('稿费标准已更新')
  editingId.value = null
  await load()
}

const formatValue = value => Number(value || 0).toFixed(4).replace(/\.?0+$/, '')

onMounted(load)
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

:deep(.el-input-number) {
  width: 130px;
}
</style>
