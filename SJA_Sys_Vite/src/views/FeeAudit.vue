<template>
  <div class="fee-audit">
    <div class="page-title">
      <h1>稿费审核与导出</h1>
      <p>查看稿费明细、审核通过或驳回，并按月份导出对账清单。</p>
    </div>

    <el-card>
      <template #header>
        <div class="toolbar">
          <el-select v-model="query.deptCode" placeholder="全部部门" clearable>
            <el-option v-for="dept in deptOptions" :key="dept.value" :label="dept.label" :value="dept.value" />
          </el-select>
          <el-date-picker v-model="query.month" type="month" value-format="YYYY-MM" placeholder="全部月份" clearable />
          <el-select v-model="query.status" placeholder="全部状态" clearable>
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
        </div>
      </template>

      <el-table :data="records" v-loading="loading" border row-key="id">
        <el-table-column prop="workType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ workTypeName(row.workType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="作品标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="submitterName" label="提交/关联人员" width="130" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ time(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button link type="success" :icon="CircleCheck" @click="openAudit(row, true)">通过</el-button>
            <el-button link type="danger" :icon="CircleClose" @click="openAudit(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="稿费详情" size="620px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="作品类型">{{ workTypeName(detail.workType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusName(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ money(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="detail-title">作品字段</h3>
        <el-table :data="workInfoRows" border size="small">
          <el-table-column prop="label" label="字段" width="160" />
          <el-table-column prop="value" label="内容" min-width="260" show-overflow-tooltip />
        </el-table>

        <h3 class="detail-title">参与人员</h3>
        <el-table :data="detail?.contributors || []" border size="small">
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="roleInWork" label="角色" width="110">
            <template #default="{ row }">{{ roleName(row.roleInWork) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥{{ money(row.feeAmount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="auditVisible" :title="auditForm.approved ? '审核通过' : '审核驳回'" width="480px">
      <el-form label-width="86px">
        <el-form-item label="作品">
          <el-input :model-value="currentRow?.title" disabled />
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="auditForm.approved ? 'success' : 'danger'" :loading="auditing" @click="handleAudit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleCheck, CircleClose, Download, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { auditFee, exportFee, getFeeDetail, getFeeList } from '@/api/fee'
import { getDeptList } from '@/api/system'

const fallbackDepts = [
  { label: '采编部', value: 'EDITORIAL' },
  { label: '摄影部', value: 'PHOTOGRAPHY' },
  { label: '视觉部', value: 'VISUAL' },
  { label: '视频部', value: 'VIDEO' }
]

const query = reactive({ deptCode: '', month: '', status: '', pageNum: 1, pageSize: 10 })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const auditVisible = ref(false)
const auditing = ref(false)
const currentRow = ref(null)
const deptOptions = ref(fallbackDepts)
const auditForm = reactive({ approved: true, opinion: '' })

const workTypeMap = { EDITORIAL: '采编', PHOTO: '摄影', VIDEO: '视频', VISUAL: '视觉' }
const roleMap = { AUTHOR: '作者', SHOOTER: '拍摄', EDITOR: '编辑/剪辑', DESIGNER: '设计' }

const money = value => Number(value || 0).toFixed(2)
const time = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const workTypeName = type => workTypeMap[type] || type || '-'
const roleName = role => roleMap[role] || role || '-'
const statusName = status => ({ SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '-')
const statusType = status => ({ APPROVED: 'success', REJECTED: 'danger', SUBMITTED: 'warning' }[status] || 'info')

const workInfoRows = computed(() => {
  const info = detail.value?.workInfo || {}
  return Object.entries(info).map(([label, value]) => ({ label, value: value ?? '-' }))
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getFeeList({ ...query })
    records.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(query, { deptCode: '', month: '', status: '', pageNum: 1, pageSize: 10 })
  loadList()
}

const openDetail = async row => {
  currentRow.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getFeeDetail(row.workType, row.id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

const openAudit = (row, approved) => {
  currentRow.value = row
  auditForm.approved = approved
  auditForm.opinion = approved ? '审核通过' : ''
  auditVisible.value = true
}

const handleAudit = async () => {
  if (!currentRow.value) return
  auditing.value = true
  try {
    await auditFee(currentRow.value.workType, currentRow.value.id, { ...auditForm })
    ElMessage.success('审核操作已提交')
    auditVisible.value = false
    await loadList()
  } finally {
    auditing.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const response = await exportFee({ deptCode: query.deptCode, month: query.month })
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `稿费清单_${query.month || '全部'}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出完成')
  } finally {
    exporting.value = false
  }
}

const loadDepts = async () => {
  try {
    const res = await getDeptList()
    const list = res.data || []
    deptOptions.value = list.map(item => ({ label: item.deptName, value: item.deptCode }))
  } catch {
    deptOptions.value = fallbackDepts
  }
}

onMounted(() => {
  loadDepts()
  loadList()
})
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.toolbar :deep(.el-select),
.toolbar :deep(.el-date-editor) {
  width: 160px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-title {
  margin: 18px 0 10px;
  font-size: 16px;
  color: var(--secondary-color);
}

@media (max-width: 768px) {
  .toolbar :deep(.el-select),
  .toolbar :deep(.el-date-editor),
  .toolbar .el-button {
    width: 100%;
  }
}
</style>
