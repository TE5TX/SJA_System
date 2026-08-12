<template>
  <div class="fee-audit">
    <div class="page-title">
      <h1>{{ canAuditFee ? '稿费审核与导出' : '稿费任务管理' }}</h1>
      <p>{{ canAuditFee ? '查看稿费明细、审核通过或驳回，并按月份导出对账清单。' : '查看所有部门任务，并修改尚未审核的任务信息。' }}</p>
    </div>

    <el-card>
      <template #header>
        <div class="toolbar">
          <!-- 202608120849 - 修复了稿费审核筛选条件变化后必须点击查询按钮才会刷新数据的问题。 -->
          <el-select v-model="query.deptCode" placeholder="全部部门" clearable @change="searchFees">
            <el-option v-for="dept in deptOptions" :key="dept.value" :label="dept.label" :value="dept.value" />
          </el-select>
          <el-date-picker
            v-model="query.month"
            type="month"
            value-format="YYYY-MM"
            placeholder="全部月份"
            clearable
            @change="searchFees"
          />
          <el-select v-model="query.status" placeholder="全部状态" clearable @change="searchFees">
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <!-- 202608121041 - 新增了按参与人员姓名实时查找稿费任务的功能。 -->
          <el-input
            v-model="query.contributorName"
            class="contributor-search"
            clearable
            :prefix-icon="Search"
            placeholder="查找参与人员"
            @input="scheduleContributorSearch"
            @clear="searchFees"
            @keyup.enter="searchFees"
          />
          <el-button type="primary" :icon="Search" @click="searchFees">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-if="canManageFeeAmount" type="success" :icon="Download" @click="openExport(false)">导出</el-button>
          <el-dropdown v-if="canManageFeeAmount" trigger="click" @command="handleBatchCommand">
            <el-button :disabled="selectedRecords.length === 0">
              批量操作{{ selectedRecords.length ? `（${selectedRecords.length}）` : '' }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="approve">批量通过</el-dropdown-item>
                <el-dropdown-item command="reject">批量驳回</el-dropdown-item>
                <el-dropdown-item command="export" divided>批量导出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>

      <el-table
        ref="feeTableRef"
        :data="records"
        v-loading="loading"
        border
        :row-key="feeRowKey"
        @selection-change="handleSelectionChange"
      >
        <!-- 202608111533 - 修复了稿费审核与导出列表没有任务勾选框和批量操作入口的问题。 -->
        <el-table-column type="selection" width="48" fixed reserve-selection />
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
          :page-sizes="[10, 20, 50, 100, 200]"
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

    <el-dialog v-model="auditVisible" :title="auditDialogTitle" width="480px">
      <el-form label-width="86px">
        <el-form-item label="作品">
          <el-input :model-value="batchAuditMode ? `已选择 ${selectedRecords.length} 个任务` : currentRow?.title" disabled />
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
              popper-class="single-year-date-picker"
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
      <el-table :data="editForm.contributors" border size="small" v-loading="videoPreviewLoading">
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
        <el-table-column v-if="canManageFeeAmount" :label="isAutomaticEditType ? '金额（自动）' : '金额'" width="150">
          <template #default="{ row }">
            <el-input-number
              v-model="row.feeAmount"
              :min="0"
              :precision="1"
              :disabled="isAutomaticEditType"
              controls-position="right"
            />
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

    <el-dialog v-model="exportVisible" :title="exportSelectedOnly ? '批量导出已选稿费' : '导出稿费明细'" width="760px">
      <!-- 202608111715 - 新增了采编各平台、摄影、视觉、视频、补贴和自定义八种导出字段模板。 -->
      <el-radio-group v-model="exportPreset" class="export-preset-list" @change="applyExportPreset">
        <el-radio-button v-for="preset in exportPresetOptions" :key="preset.value" :label="preset.value">
          {{ preset.label }}
        </el-radio-button>
      </el-radio-group>
      <div class="export-actions">
        <el-button text class="export-action-btn" @click="toggleAllExportFields">
          {{ allExportFieldsSelected ? '取消全选' : '全选' }}
        </el-button>
        <el-button text class="export-action-btn" @click="restoreDefaultExportFields">恢复默认</el-button>
      </div>
      <el-checkbox-group v-model="exportFields" class="export-field-list" @change="handleExportFieldsChange">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ArrowDown, CircleCheck, CircleClose, Delete, Download, Edit, Refresh, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditFee, batchAuditFee, calculateFee, exportFee, getFeeDetail, getFeeList, invalidateRejectedFee, resetFeeAudit, updateFeeDetail } from '@/api/fee'
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

// 202608111533 - 修复了稿费审核与导出列表单页不能选择100或200条的问题。
const query = reactive({ deptCode: '', month: '', status: '', contributorName: '', pageNum: 1, pageSize: 10 })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const exportVisible = ref(false)
const exportSelectedOnly = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const auditVisible = ref(false)
const auditing = ref(false)
const batchAuditMode = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const videoPreviewLoading = ref(false)
const currentRow = ref(null)
const feeTableRef = ref(null)
const selectedRecords = ref([])
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
const exportPresetOptions = [
  { label: '采编-微信', value: 'EDITORIAL_WECHAT' },
  { label: '采编-院网', value: 'EDITORIAL_WEBSITE' },
  { label: '采编-校报', value: 'EDITORIAL_NEWSPAPER' },
  { label: '摄影', value: 'PHOTO' },
  { label: '视觉', value: 'VISUAL' },
  { label: '视频', value: 'VIDEO' },
  { label: '补贴', value: 'SUBSIDY' },
  { label: '自定义', value: 'CUSTOM' }
]
const commonWorkFields = [
  'workType', 'title', 'category', 'publishDate', 'articleLink',
  'contributorNames', 'contributorAmounts', 'totalAmount',
  'isSpecialNode', 'specialMultiplier', 'remark'
]
const exportPresetFields = {
  EDITORIAL_WECHAT: [...commonWorkFields.slice(0, 5), 'readCount', ...commonWorkFields.slice(5)],
  EDITORIAL_WEBSITE: [...commonWorkFields],
  EDITORIAL_NEWSPAPER: [...commonWorkFields.slice(0, 5), 'wordCount', ...commonWorkFields.slice(5)],
  PHOTO: [...commonWorkFields.slice(0, 6), 'workCounts', ...commonWorkFields.slice(6)],
  VISUAL: [...commonWorkFields],
  VIDEO: [
    ...commonWorkFields.slice(0, 6), 'likeCount', 'isReposted', 'isComplexEdit',
    ...commonWorkFields.slice(6)
  ],
  SUBSIDY: [
    'workType', 'title', 'category', 'subsidyMonth',
    'contributorNames', 'contributorAmounts', 'totalAmount', 'remark'
  ]
}
const exportPreset = ref('CUSTOM')
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
const automaticEditTypes = new Set(['EDITORIAL', 'PHOTO', 'VISUAL', 'VIDEO'])
const isAutomaticEditType = computed(() => automaticEditTypes.has(editForm.workType))
const auditDialogTitle = computed(() => {
  const action = auditForm.approved ? '通过' : '驳回'
  return batchAuditMode.value ? `批量${action}稿费任务` : `审核${action}`
})
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
  .filter(key => !(editForm.workType === 'VIDEO' && key === 'videoDurationSec'))
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

const feeRowKey = row => `${row.workType}:${row.id}`

const handleSelectionChange = selection => {
  selectedRecords.value = selection
}

const clearSelection = () => {
  feeTableRef.value?.clearSelection()
  selectedRecords.value = []
}

const selectedTaskRefs = () => selectedRecords.value.map(item => ({
  workType: item.workType,
  id: Number(item.id)
}))

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

const searchFees = () => {
  if (contributorSearchTimer) clearTimeout(contributorSearchTimer)
  clearSelection()
  query.pageNum = 1
  loadList()
}

let contributorSearchTimer
const scheduleContributorSearch = () => {
  if (contributorSearchTimer) clearTimeout(contributorSearchTimer)
  contributorSearchTimer = setTimeout(searchFees, 300)
}

const resetQuery = () => {
  clearSelection()
  Object.assign(query, { deptCode: '', month: '', status: '', contributorName: '', pageNum: 1, pageSize: 10 })
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
  batchAuditMode.value = false
  currentRow.value = row
  auditForm.approved = approved
  auditForm.opinion = approved ? '审核通过' : ''
  auditVisible.value = true
}

const openBatchAudit = approved => {
  if (!selectedRecords.value.length) return
  if (selectedRecords.value.some(item => item.status !== 'SUBMITTED')) {
    ElMessage.warning('批量审核只能选择状态为“已提交”的任务')
    return
  }
  batchAuditMode.value = true
  currentRow.value = null
  auditForm.approved = approved
  auditForm.opinion = approved ? '批量审核通过' : ''
  auditVisible.value = true
}

const handleBatchCommand = command => {
  if (command === 'approve') openBatchAudit(true)
  else if (command === 'reject') openBatchAudit(false)
  else if (command === 'export') openExport(true)
}

const openEdit = async row => {
  await openDetail(row)
  editForm.workType = detail.value.workType
  editForm.workInfo = { ...(detail.value.workInfo || {}), remark: detail.value.remark || '' }
  editForm.contributors = (detail.value.contributors || []).map(item => ({ ...item }))
  editVisible.value = true
  if (isAutomaticEditType.value && canManageFeeAmount.value) {
    scheduleAutomaticFeePreview()
  }
}

let feePreviewSequence = 0
let feePreviewTimer
const refreshAutomaticFeePreview = async () => {
  if (!editVisible.value || !isAutomaticEditType.value || !canManageFeeAmount.value || !editForm.contributors.length) return
  const sequence = ++feePreviewSequence
  videoPreviewLoading.value = true
  try {
    const res = await calculateFee({
      workType: editForm.workType,
      workInfo: { ...editForm.workInfo },
      contributors: editForm.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: item.roleInWork,
        workCount: item.workCount == null ? null : Number(item.workCount),
        feeAmount: null,
        remark: item.remark || ''
      }))
    })
    if (sequence !== feePreviewSequence) return
    ;(res.data || []).forEach((amount, index) => {
      if (editForm.contributors[index]) editForm.contributors[index].feeAmount = Number(amount)
    })
  } finally {
    if (sequence === feePreviewSequence) videoPreviewLoading.value = false
  }
}

const scheduleAutomaticFeePreview = () => {
  if (feePreviewTimer) clearTimeout(feePreviewTimer)
  feePreviewTimer = setTimeout(refreshAutomaticFeePreview, 200)
}

watch(
  () => isAutomaticEditType.value ? JSON.stringify({
    workType: editForm.workType,
    workInfo: editForm.workInfo,
    contributors: editForm.contributors.map(item => ({
      userId: item.userId,
      roleInWork: item.roleInWork,
      workCount: item.workCount
    }))
  }) : '',
  scheduleAutomaticFeePreview
)

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
  if (!batchAuditMode.value && !currentRow.value) return
  auditing.value = true
  try {
    if (batchAuditMode.value) {
      await batchAuditFee({
        tasks: selectedTaskRefs(),
        approved: auditForm.approved,
        opinion: auditForm.opinion
      })
      ElMessage.success(`已批量${auditForm.approved ? '通过' : '驳回'} ${selectedRecords.value.length} 个任务`)
      clearSelection()
    } else {
      await auditFee(currentRow.value.workType, currentRow.value.id, { ...auditForm })
      ElMessage.success('审核操作已提交')
    }
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

const openExport = (selectedOnly = false) => {
  if (selectedOnly && !selectedRecords.value.length) {
    ElMessage.warning('请至少勾选一个稿费任务')
    return
  }
  exportSelectedOnly.value = selectedOnly
  if (!selectedOnly) {
    const inferredPreset = {
      'EDITORIAL:微信': 'EDITORIAL_WECHAT',
      'EDITORIAL:院网': 'EDITORIAL_WEBSITE',
      'EDITORIAL:校报': 'EDITORIAL_NEWSPAPER',
      PHOTOGRAPHY: 'PHOTO',
      VISUAL: 'VISUAL',
      VIDEO: 'VIDEO',
      MANAGEMENT: 'SUBSIDY'
    }[query.deptCode]
    if (inferredPreset) {
      exportPreset.value = inferredPreset
      applyExportPreset(inferredPreset)
    }
  }
  exportVisible.value = true
}

const applyExportPreset = preset => {
  if (preset === 'CUSTOM') return
  exportFields.value = [...exportPresetFields[preset]]
}

const handleExportFieldsChange = () => {
  const expected = exportPresetFields[exportPreset.value]
  if (!expected || expected.length !== exportFields.value.length || expected.some(field => !exportFields.value.includes(field))) {
    exportPreset.value = 'CUSTOM'
  }
}

const toggleAllExportFields = () => {
  exportFields.value = allExportFieldsSelected.value ? [] : exportFieldOptions.map(field => field.key)
}

const restoreDefaultExportFields = () => {
  const presetFields = exportPresetFields[exportPreset.value]
  exportFields.value = [...(presetFields || defaultExportFields)]
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
      contributorName: query.contributorName,
      fields: exportFields.value,
      selectedTasks: exportSelectedOnly.value ? selectedTaskRefs() : null
    })
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${exportSelectedOnly.value ? '已选稿费清单' : '稿费清单'}_${query.month || '全部'}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    exportVisible.value = false
    ElMessage.success(exportSelectedOnly.value ? `已导出 ${selectedRecords.value.length} 个选中任务` : '导出完成')
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

.toolbar .contributor-search {
  width: 180px;
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

/* 202608121123 - 移除了审核编辑日期选择器中容易误触的跨年双箭头按钮。 */
:global(.single-year-date-picker .d-arrow-left),
:global(.single-year-date-picker .d-arrow-right) {
  display: none;
}

@media (max-width: 768px) {
  .toolbar :deep(.el-select),
  .toolbar :deep(.el-date-editor),
  .toolbar .contributor-search,
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

.export-preset-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  width: 100%;
  margin-bottom: 18px;
}

.export-preset-list :deep(.el-radio-button) {
  width: 100%;
}

.export-preset-list :deep(.el-radio-button__inner) {
  width: 100%;
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
