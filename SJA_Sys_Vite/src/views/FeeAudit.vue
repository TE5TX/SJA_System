<template>
  <div class="fee-audit">
    <div class="page-title">
      <h1>{{ canAuditFee ? '稿费审核与导出' : '稿费任务管理' }}</h1>
      <p>{{ canAuditFee ? '查看稿费明细、审核通过或驳回，并按月份导出对账清单。' : '查看所有部门任务，并修改尚未审核的任务信息。' }}</p>
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
          <el-button v-if="canManageFeeAmount" type="success" :icon="Download" @click="openExport">导出</el-button>
        </div>
      </template>

      <el-table :data="records" v-loading="loading" border row-key="id">
        <el-table-column prop="workType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ workTypeName(row.workType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="作品标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="发布时间" width="120">
          <template #default="{ row }">{{ row.publishDate || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="canManageFeeAmount" label="金额" width="120">
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
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
              <el-button v-if="canEditFeeTask && (canManageFeeAmount || isEditableStatus(row.status))" link type="primary" :icon="Edit" @click="openEdit(row)">
                {{ row.status === 'REJECTED' ? '修改重提' : '编辑' }}
              </el-button>
              <el-button v-if="canAuditFee && row.status === 'SUBMITTED'" link type="success" :icon="CircleCheck" @click="openAudit(row, true)">通过</el-button>
              <el-button v-if="canAuditFee && row.status === 'SUBMITTED'" link type="danger" :icon="CircleClose" @click="openAudit(row, false)">驳回</el-button>
              <el-button v-if="canAuditFee && row.status === 'APPROVED'" link type="warning" :icon="RefreshLeft" @click="handleResetAudit(row)">退回待审</el-button>
              <el-button v-if="canEditFeeTask && row.status === 'REJECTED'" link type="danger" :icon="Delete" @click="handleInvalidate(row)">删除</el-button>
            </div>
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
          <el-descriptions-item v-if="canManageFeeAmount" label="总金额">¥{{ money(detail.totalAmount) }}</el-descriptions-item>
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
          <el-table-column v-if="detailShowsWorkCount" prop="workCount" label="作品数量" width="100" />
          <el-table-column v-if="canManageFeeAmount" label="金额" width="120">
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

    <el-dialog v-model="editVisible" title="编辑稿费任务" width="760px">
      <el-form label-width="110px" class="edit-work-form">
        <el-form-item v-for="field in editWorkFields" :key="field.key" :label="field.label">
          <el-select
            v-if="field.key === 'platform'"
            v-model="editForm.workInfo[field.key]"
            @change="handleEditPlatformChange"
          >
            <el-option v-for="option in platformOptions" :key="option" :label="option" :value="option" />
          </el-select>
          <el-select
            v-else-if="field.key === 'category' && editCategoryOptions.length"
            v-model="editForm.workInfo[field.key]"
            placeholder="请选择类别"
          >
            <el-option v-for="option in editCategoryOptions" :key="option" :label="option" :value="option" />
          </el-select>
          <el-switch
            v-else-if="switchFieldKeys.includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            :active-value="1"
            :inactive-value="0"
          />
          <el-input-number
            v-else-if="stepThousandNumberFields.includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            :min="0"
            :step="1000"
            controls-position="right"
            @focus="selectNumberInputText"
            @click="selectNumberInputText"
          />
          <el-input-number
            v-else-if="numberFieldKeys.includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            :min="0"
            :precision="field.key === 'authorAvgFee' ? 1 : undefined"
            controls-position="right"
            @focus="selectNumberInputText"
            @click="selectNumberInputText"
          />
          <el-segmented
            v-else-if="field.key === 'specialMultiplier'"
            v-model="editForm.workInfo[field.key]"
            :options="[2, 3]"
          />
          <div
            v-else-if="field.key === 'publishDate'"
            class="wheel-date-picker"
            @wheel.capture.prevent.stop="event => adjustEditDateByWheel(field.key, event)"
          >
            <el-date-picker
              v-model="editForm.workInfo[field.key]"
              type="date"
              value-format="YYYY-MM-DD"
              :teleported="false"
            />
          </div>
          <el-date-picker
            v-else-if="field.key === 'subsidyMonth'"
            v-model="editForm.workInfo[field.key]"
            type="month"
            value-format="YYYY-MM"
          />
          <el-input
            v-else-if="['remark', 'reprintStatus'].includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
          <el-input
            v-else
            v-model="editForm.workInfo[field.key]"
            :placeholder="['articleLink', 'videoLink'].includes(field.key) ? 'https://' : ''"
          />
        </el-form-item>
      </el-form>
      <h3 class="detail-title">{{ canManageFeeAmount ? '参与人员金额' : '参与人员' }}</h3>
      <el-table :data="editForm.contributors" border size="small">
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column label="用户ID" width="110">
          <template #default="{ row }">
            <el-input v-model="row.userId" />
          </template>
        </el-table-column>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-select v-model="row.roleInWork">
              <el-option v-for="role in contributorRoles" :key="role.value" :label="role.label" :value="role.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column v-if="canManageFeeAmount" label="金额" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.feeAmount" :min="0" :precision="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column v-if="editShowsWorkCount" label="作品数量" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.workCount" :min="1" controls-position="right" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="exportVisible" title="导出稿费明细" width="680px">
      <div class="export-actions">
        <el-button text class="export-action-btn" @click="toggleAllExportFields">
          {{ allExportFieldsSelected ? '取消全选' : '全选' }}
        </el-button>
        <el-button text class="export-action-btn" @click="restoreDefaultExportFields">恢复默认</el-button>
      </div>
      <el-checkbox-group v-model="exportFields" class="export-field-list">
        <el-checkbox v-for="field in exportFieldOptions" :key="field.key" :label="field.key">
          {{ field.label }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="handleExport">导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleCheck, CircleClose, Delete, Download, Edit, Refresh, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditFee, exportFee, getFeeDetail, getFeeList, invalidateRejectedFee, resetFeeAudit, updateFeeDetail } from '@/api/fee'
import { useUserStore } from '@/store/user'

const fallbackDepts = [
  { label: '采编部', value: 'EDITORIAL' },
  { label: '采编部-微信', value: 'EDITORIAL:微信' },
  { label: '采编部-院网', value: 'EDITORIAL:院网' },
  { label: '采编部-校报', value: 'EDITORIAL:校报' },
  { label: '摄影部', value: 'PHOTOGRAPHY' },
  { label: '摄影部-微信', value: 'PHOTOGRAPHY:微信' },
  { label: '摄影部-院网', value: 'PHOTOGRAPHY:院网' },
  { label: '摄影部-校报', value: 'PHOTOGRAPHY:校报' },
  { label: '视觉部', value: 'VISUAL' },
  { label: '视频部', value: 'VIDEO' },
  { label: '月度补贴', value: 'MANAGEMENT' }
]
const platformOptions = ['微信', '院网', '校报']
const photoCategoryOptions = ['原创类', '半原创类']
const visualCategoryOptions = ['普通消息类', '原创美文（不含制图）', '原创美文（含部分制图）', '互动、长图、SVG动图、手绘', '校园网首页大图']
const subsidyCategoryOptions = ['微信统筹编辑', '排版编辑', '校对', '主席团成员', '校园巡查组长', '校园巡查组员']
const videoCategoryOptions = ['活动新闻类视频', '风景类系列短视频', '原创类短视频', '原创类长视频']
const switchFieldKeys = ['isPublished', 'isSpecialNode', 'isReposted', 'isComplexEdit']
const stepThousandNumberFields = ['readCount', 'wordCount']
const numberFieldKeys = ['videoDurationSec', 'likeCount', 'authorCount', 'authorAvgFee']

const query = reactive({ deptCode: '', month: '', status: '', pageNum: 1, pageSize: 10 })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const exportVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const auditVisible = ref(false)
const auditing = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const currentRow = ref(null)
const deptOptions = ref(fallbackDepts)
const auditForm = reactive({ approved: true, opinion: '' })
const editForm = reactive({ workType: '', workInfo: {}, contributors: [] })
const userStore = useUserStore()

const workTypeMap = { EDITORIAL: '采编', PHOTO: '摄影', VIDEO: '视频', VISUAL: '视觉', SUBSIDY: '月度补贴' }
const roleMap = { AUTHOR: '作者', SHOOTER: '拍摄', EDITOR: '编辑/剪辑', DESIGNER: '设计', MEMBER: '补贴人员' }
const contributorRoles = [
  { label: '作者', value: 'AUTHOR' },
  { label: '拍摄', value: 'SHOOTER' },
  { label: '编辑/剪辑', value: 'EDITOR' },
  { label: '设计', value: 'DESIGNER' },
  { label: '补贴人员', value: 'MEMBER' }
]
const exportFieldOptions = [
  { key: 'workType', label: '作品类型' },
  { key: 'deptName', label: '部门' },
  { key: 'title', label: '标题' },
  { key: 'platform', label: '发布平台' },
  { key: 'category', label: '类别' },
  { key: 'publishDate', label: '发布时间' },
  { key: 'articleLink', label: '作品链接' },
  { key: 'isPublished', label: '是否已发布' },
  { key: 'readCount', label: '阅读量' },
  { key: 'wordCount', label: '字数' },
  { key: 'videoDurationSec', label: '视频时长' },
  { key: 'likeCount', label: '点赞数' },
  { key: 'isReposted', label: '官方转载' },
  { key: 'isComplexEdit', label: '复杂剪辑' },
  { key: 'subsidyMonth', label: '补贴月份' },
  { key: 'contributorNames', label: '参与人员姓名' },
  { key: 'contributorStudentNos', label: '参与人员学号' },
  { key: 'contributorRoles', label: '参与角色' },
  { key: 'workCounts', label: '作品数量' },
  { key: 'contributorAmounts', label: '个人金额' },
  { key: 'totalAmount', label: '总金额' },
  { key: 'auditStatus', label: '审核状态' },
  { key: 'isSpecialNode', label: '特殊节点' },
  { key: 'specialMultiplier', label: '特殊节点倍数' },
  { key: 'remark', label: '备注' },
  { key: 'createTime', label: '创建时间' }
]
const defaultExportFields = [
  'workType', 'deptName', 'title', 'platform', 'category', 'publishDate',
  'contributorNames', 'contributorStudentNos', 'contributorRoles',
  'contributorAmounts', 'totalAmount', 'auditStatus', 'remark'
]
const exportFields = ref([...defaultExportFields])
const allExportFieldsSelected = computed(() => exportFields.value.length === exportFieldOptions.length)

// 202608101716 - 将稿费金额统一显示为四舍五入后的一位小数。
const money = value => Number(value || 0).toFixed(1)
const time = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const workTypeName = type => workTypeMap[type] || type || '-'
const roleName = role => roleMap[role] || role || '-'
const statusName = status => ({ SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '-')
const statusType = status => ({ APPROVED: 'success', REJECTED: 'danger', SUBMITTED: 'warning' }[status] || 'info')
const isEditableStatus = status => status === 'SUBMITTED' || status === 'REJECTED'
const roleCodes = computed(() => userStore.userInfo?.roleCodes || [])
const highestRoleCode = computed(() => roleCodes.value.length ? Math.min(...roleCodes.value) : 13)
const canManageFeeAmount = computed(() => [0, 3].includes(highestRoleCode.value))
const canAuditFee = computed(() => [0, 3].includes(highestRoleCode.value))
const canEditFeeTask = computed(() => [0, 3, 4].includes(highestRoleCode.value))
const fieldLabelMap = {
  title: '标题',
  videoTitle: '视频标题',
  platform: '发布平台',
  category: '类别',
  readCount: '阅读量',
  wordCount: '字数',
  videoDurationSec: '时长（秒）',
  likeCount: '点赞数',
  reprintStatus: '转载情况',
  isReposted: '是否官方转载',
  isComplexEdit: '是否复杂剪辑',
  authorCount: '参与人数',
  authorAvgFee: '人均金额',
  publishDate: '发布时间',
  articleLink: '文章链接',
  videoLink: '视频链接',
  isPublished: '是否已发布',
  isSpecialNode: '特殊节点',
  specialMultiplier: '特殊节点倍数',
  subsidyMonth: '补贴月份',
  remark: '备注'
}

const workInfoRows = computed(() => {
  const info = detail.value?.workInfo || {}
  return Object.entries(info).map(([key, value]) => ({
    label: fieldLabelMap[key] || key,
    value: key === 'authorAvgFee'
      ? money(value)
      : (['isPublished', 'isSpecialNode', 'isReposted', 'isComplexEdit'].includes(key) ? (Number(value) === 1 ? '是' : '否') : (value ?? '-'))
  }))
})
const amountSensitiveFields = new Set(['isSpecialNode', 'specialMultiplier', 'authorAvgFee'])
const editWorkFields = computed(() => Object.keys(editForm.workInfo || {})
  .filter(key => canManageFeeAmount.value || !amountSensitiveFields.has(key))
  .filter(key => key !== 'specialMultiplier' || Number(editForm.workInfo?.isSpecialNode) === 1)
  .map(key => ({ key, label: fieldLabelMap[key] || key })))
const detailShowsWorkCount = computed(() => (detail.value?.contributors || []).some(item => item.workCount != null))
const editShowsWorkCount = computed(() => (editForm.contributors || []).some(item => item.workCount != null))
const editCategoryOptions = computed(() => {
  if (editForm.workType === 'VIDEO') return videoCategoryOptions
  if (editForm.workType === 'VISUAL') return visualCategoryOptions
  if (editForm.workType === 'SUBSIDY') return subsidyCategoryOptions
  if (editForm.workType === 'PHOTO') {
    return editForm.workInfo?.platform === '微信' ? photoCategoryOptions : []
  }
  if (editForm.workType === 'EDITORIAL') {
    return {
      微信: ['原创类', '半原创类'],
      院网: ['莲峰群英', '学校新闻（含学校要闻、校园时讯）'],
      校报: ['原创头版头条', '原创一般稿件', '重新编辑类稿件']
    }[editForm.workInfo?.platform] || []
  }
  return []
})

const formatDateValue = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const adjustEditDateByWheel = (key, event) => {
  event.preventDefault()
  const deltaY = Number(event.deltaY || 0)
  if (deltaY === 0) return
  const parts = String(editForm.workInfo?.[key] || '').split('-').map(Number)
  const current = parts.length === 3 && parts.every(Number.isFinite)
    ? new Date(parts[0], parts[1] - 1, parts[2])
    : new Date()
  current.setDate(current.getDate() + (deltaY > 0 ? 1 : -1))
  editForm.workInfo[key] = formatDateValue(current)
}

const selectNumberInputText = event => {
  const target = event?.target
  if (target?.closest?.('.el-input-number__decrease, .el-input-number__increase')) return
  const input = target?.tagName === 'INPUT'
    ? target
    : event?.currentTarget?.querySelector?.('input')
  if (input?.select) {
    requestAnimationFrame(() => input.select())
  }
}

const handleEditPlatformChange = () => {
  if (editForm.workType === 'EDITORIAL') {
    editForm.workInfo.category = editCategoryOptions.value[0] || ''
  } else if (editForm.workType === 'PHOTO') {
    editForm.workInfo.category = editForm.workInfo.platform === '微信' ? photoCategoryOptions[0] : ''
  }
}

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

const openEdit = async row => {
  await openDetail(row)
  editForm.workType = detail.value.workType
  editForm.workInfo = { ...(detail.value.workInfo || {}), remark: detail.value.remark || '' }
  editForm.contributors = (detail.value.contributors || []).map(item => ({ ...item }))
  editVisible.value = true
}

const handleSaveEdit = async () => {
  if (!currentRow.value) return
  saving.value = true
  try {
    await updateFeeDetail(currentRow.value.workType, currentRow.value.id, {
      workType: currentRow.value.workType,
      workInfo: { ...editForm.workInfo },
      contributors: editForm.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: item.roleInWork,
        feeAmount: canManageFeeAmount.value ? Number(item.feeAmount || 0) : null,
        workCount: item.workCount == null ? null : Number(item.workCount),
        remark: item.remark || ''
      }))
    })
    ElMessage.success('稿费任务已保存')
    editVisible.value = false
    detailVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
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

const handleResetAudit = async row => {
  await ElMessageBox.confirm(
    `确定将“${row.title}”退回待审核状态吗？`,
    '退回待审核',
    { confirmButtonText: '确定退回', cancelButtonText: '取消', type: 'warning' }
  )
  await resetFeeAudit(row.workType, row.id)
  ElMessage.success('已退回待审核状态')
  await loadList()
}

const handleInvalidate = async row => {
  await ElMessageBox.confirm(
    `确定删除已驳回的任务“${row.title}”吗？删除后不会出现在任务列表和稿费导出中，数据库记录仍会保留。`,
    '删除稿费任务',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  )
  await invalidateRejectedFee(row.workType, row.id)
  ElMessage.success('任务已删除')
  await loadList()
}

const openExport = () => {
  exportVisible.value = true
}

const toggleAllExportFields = () => {
  exportFields.value = allExportFieldsSelected.value ? [] : exportFieldOptions.map(field => field.key)
}

const restoreDefaultExportFields = () => {
  exportFields.value = [...defaultExportFields]
}

const handleExport = async () => {
  if (!exportFields.value.length) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }
  exporting.value = true
  try {
    const response = await exportFee({
      deptCode: query.deptCode,
      month: query.month,
      status: query.status,
      fields: exportFields.value
    })
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `稿费清单_${query.month || '全部'}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    exportVisible.value = false
    ElMessage.success('导出完成')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
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

.edit-work-form :deep(.el-select),
.edit-work-form :deep(.el-date-editor),
.edit-work-form :deep(.el-input-number),
.edit-work-form :deep(.el-segmented),
.wheel-date-picker {
  width: 100%;
}

@media (max-width: 768px) {
  .toolbar :deep(.el-select),
  .toolbar :deep(.el-date-editor),
  .toolbar .el-button {
    width: 100%;
  }
}

.action-buttons {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 10px;
  white-space: nowrap;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.export-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 14px;
}

.export-field-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px 18px;
}

.export-field-list :deep(.el-checkbox) {
  margin-right: 0;
}

@media (max-width: 640px) {
  .export-field-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
