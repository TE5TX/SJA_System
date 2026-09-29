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
          <el-button :loading="previewing" @click="handlePreview">预览</el-button>
          <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">
            导出时间段稿费名单
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 202609281642 - 完成了与 Excel 六列、排序和合计一致的月份稿费核对表。 -->
    <el-card v-if="preview" class="preview-panel">
      <template #header>
        <div class="preview-heading">
          <div class="preview-heading__title">
            <!-- <span class="preview-heading__eyebrow">导出前核对</span> -->
            <h2>稿费发放名单预览</h2>
            <!-- <p>{{ preview.startMonth }} 至 {{ preview.endMonth }} · 与下载的 Excel 使用同一份清单</p> -->
          </div>
          <div class="preview-heading__summary">
            <span>共 <strong>{{ preview.rows.length }}</strong> 人</span>
            <span>实发合计 <strong>¥{{ preview.grandTotal }}</strong></span>
          </div>
        </div>
      </template>
      <!-- <div class="preview-tip">点击计算式中的金额，可查看对应稿费任务；鼠标悬停也能快速查看。</div> -->
      <el-table
        :data="previewTableRows"
        border
        stripe
        class="preview-table"
        :row-class-name="previewRowClass"
        :span-method="previewSpanMethod"
      >
        <el-table-column prop="department" label="所属部门" min-width="125" align="center" header-align="center" />
        <el-table-column prop="realName" label="人员姓名" min-width="110" align="center" header-align="center">
          <template #default="{ row }"><strong class="member-name">{{ row.realName }}</strong></template>
        </el-table-column>
        <el-table-column prop="idCard" label="人员身份证号码" min-width="190" align="center" header-align="center" />
        <el-table-column label="人员稿费计算情况" min-width="240" align="center" header-align="center">
          <template #default="{ row }">
            <template v-for="(term, index) in row.terms" :key="term.contributorId ?? `${row.realName}-${index}`">
              <span v-if="index" class="amount-separator">+</span>
              <el-popover trigger="hover" placement="top" :width="300" :show-after="180" popper-class="monthly-fee-popover">
                <template #reference>
                  <button type="button" class="amount-term" @click="openTermDetail(term, row)">
                    {{ term.amountText }}
                  </button>
                </template>
                <div class="fee-hover-card">
                  <div class="fee-hover-card__tags">
                    <el-tag size="small" effect="plain">{{ workTypeName(term.workType) }}</el-tag>
                    <el-tag v-if="term.workType !== 'SUBSIDY'" size="small" :type="term.published ? 'success' : 'info'" effect="light">
                      {{ term.published ? '已发布' : '未发布' }}
                    </el-tag>
                  </div>
                  <strong class="fee-hover-card__title">{{ term.title || '未命名任务' }}</strong>
                  <div class="fee-hover-card__amount">¥{{ term.amountText }}<small>{{ row.realName }}的本项稿费</small></div>
                  <div class="fee-hover-card__meta">
                    {{ term.workType === 'SUBSIDY' ? '所属月份' : '发布日期' }}：{{ termDate(term) }}
                    <span>·</span> {{ term.participants?.length || 0 }} 人参与
                  </div>
                  <!-- <div class="fee-hover-card__footer">点击查看完整任务与分配明细 <el-icon><ArrowRight /></el-icon></div> -->
                </div>
              </el-popover>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="实发金额" min-width="120" align="center" header-align="center">
          <template #default="{ row }"><span class="payable-amount">¥{{ row.total }}</span></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="135" align="center" header-align="center" />
      </el-table>
    </el-card>

    <!-- 202609290934 - 修复了月份稿费任务详情弹窗按默认顶部边距定位、长内容超出视口的问题。 -->
    <el-dialog v-model="termDialogVisible" class="monthly-task-dialog" width="min(640px, 94vw)" append-to-body align-center>
      <template #header>
        <div class="task-dialog-heading">
          <span>稿费任务详情</span>
          <!-- <small>逐笔核对 · 已保存金额</small> -->
        </div>
      </template>
      <template v-if="selectedTerm">
        <!-- 202609281715 - 点击详情展示任务元数据与全部参与人的实际金额，区别于悬停摘要。 -->
        <div class="task-dialog-overview">
          <div class="task-dialog-tags">
            <el-tag effect="plain">{{ workTypeName(selectedTerm.workType) }}</el-tag>
            <el-tag v-if="selectedTerm.workType !== 'SUBSIDY'" :type="selectedTerm.published ? 'success' : 'info'">
              {{ selectedTerm.published ? '已发布' : '未发布' }}
            </el-tag>
          </div>
          <h3>{{ selectedTerm.title || '未命名任务' }}</h3>
        </div>

        <div class="task-amount-summary">
          <div><span>{{ selectedMemberName }}的本项稿费</span><strong>¥{{ selectedTerm.amountText }}</strong></div>
          <div><span>任务全部参与人合计</span><strong>{{ formatAmount(selectedTerm.workTotal) }}</strong></div>
        </div>

        <div class="task-facts">
          <div><span>{{ selectedTerm.workType === 'SUBSIDY' ? '补贴月份' : '发布日期' }}</span><strong>{{ termDate(selectedTerm) }}</strong></div>
          <div><span>参与人数</span><strong>{{ selectedTerm.participants?.length || 0 }} 人</strong></div>
          <!-- <div><span>任务编号</span><strong>{{ selectedTerm.workId ?? '—' }}</strong></div>
          <div><span>稿费明细编号</span><strong>{{ selectedTerm.contributorId ?? '—' }}</strong></div> -->
        </div>

        <div class="task-participants-heading">
          <h4>参与人员与实际金额</h4>
          <!-- <span>按任务参与记录展示</span> -->
        </div>
        <el-table v-if="selectedTerm.participants?.length" :data="selectedTerm.participants" border size="small" :max-height="320" class="task-participants-table">
          <el-table-column prop="name" label="姓名" min-width="125" align="center" header-align="center" />
          <el-table-column prop="role" label="任务角色" min-width="130" align="center" header-align="center" />
          <el-table-column label="实得金额" min-width="135" align="center" header-align="center">
            <template #default="{ row }"><strong>{{ formatAmount(row.amount) }}</strong></template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无参与人员明细" :image-size="70" />
        <!-- <p class="task-detail-note">金额取自已保存的稿费明细，预览与 Excel 导出使用同一数据。</p> -->
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ArrowRight, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportMonthlyFeeSummary, previewMonthlyFeeSummary } from '@/api/fee'

const currentMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

// 202609100927 - 完成了月份稿费名单按起止月份区间导出的功能。
const monthRange = ref([currentMonth(), currentMonth()])
const exporting = ref(false)
const previewing = ref(false)
const preview = ref(null)
const previewTableRows = computed(() => preview.value ? [
  ...preview.value.rows,
  { department: '', realName: '合计', idCard: '', calculation: '', total: preview.value.grandTotal, remark: '', terms: [], isTotal: true }
] : [])
const previewRowClass = ({ row }) => row.isTotal ? 'monthly-total-row' : ''
// 202609281706 - 将同一部门的连续人员行真正合并；合计行保持独立单元格。
const previewSpanMethod = ({ row, columnIndex, rowIndex }) => {
  if (columnIndex !== 0 || row.isTotal) return [1, 1]
  if (!row.department) return [0, 0]
  let span = 1
  while (rowIndex + span < previewTableRows.value.length) {
    const next = previewTableRows.value[rowIndex + span]
    if (next.isTotal || next.department) break
    span += 1
  }
  return [span, 1]
}
const selectedTerm = ref(null)
const selectedMemberName = ref('')
const termDialogVisible = computed({
  get: () => selectedTerm.value !== null,
  set: value => { if (!value) selectedTerm.value = null }
})

watch(monthRange, () => { preview.value = null })

const workTypeName = type => ({
  EDITORIAL: '采编', PHOTO: '摄影', VISUAL: '视觉', VIDEO: '视频', SUBSIDY: '月度补贴'
})[type] || type || '未知'
const termDate = term => term.workType === 'SUBSIDY'
  ? (term.subsidyMonth || '—')
  : (term.published ? (term.publishDate || '—') : '未发布')
const formatAmount = amount => amount == null ? '—' : `¥${amount}`
const openTermDetail = (term, row) => {
  selectedMemberName.value = row.realName
  selectedTerm.value = term
}

const selectedMonths = () => {
  if (!Array.isArray(monthRange.value) || monthRange.value.length !== 2) {
    ElMessage.warning('请选择完整的稿费月份范围')
    return null
  }
  const [startMonth, endMonth] = monthRange.value
  return { startMonth, endMonth }
}

const handlePreview = async () => {
  const range = selectedMonths()
  if (!range) return
  preview.value = null
  previewing.value = true
  try {
    const response = await previewMonthlyFeeSummary(range)
    preview.value = response.data
  } finally {
    previewing.value = false
  }
}

const handleExport = async () => {
  const range = selectedMonths()
  if (!range) return
  const { startMonth, endMonth } = range
  exporting.value = true
  try {
    // 202609281642 - 预览后导出提交清单指纹；未预览时保留直接导出最新数据的入口。
    const response = await exportMonthlyFeeSummary({
      ...range,
      ...(preview.value ? { fingerprint: preview.value.fingerprint } : {})
    })
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
  } catch (error) {
    let message = '稿费名单导出失败'
    if (error.response?.data instanceof Blob) {
      try { message = JSON.parse(await error.response.data.text()).message || message } catch { /* 非 JSON 错误响应 */ }
    }
    if (error.response?.status === 409) preview.value = null
    ElMessage.error(message)
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

/* 202609281706 - 预览区沿用系统蓝白卡片风格，并统一表头、内容和合计的居中样式。 */
.preview-panel {
  margin-top: 20px;
}

.preview-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.preview-heading__eyebrow {
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.preview-heading h2 {
  margin: 2px 0;
  color: var(--secondary-color);
  font-size: 19px;
  font-weight: 700;
}

.preview-heading p {
  margin: 0;
  color: #71809b;
  font-size: 13px;
}

.preview-heading__summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.preview-heading__summary > span {
  padding: 7px 12px;
  border: 1px solid #e1e8fb;
  border-radius: 6px;
  color: #5e6d89;
  background: #f6f9ff;
  font-size: 13px;
  white-space: nowrap;
}

.preview-heading__summary strong {
  color: var(--secondary-color);
  font-size: 16px;
}

.preview-tip {
  margin-bottom: 14px;
  padding: 9px 12px;
  border-radius: 6px;
  color: #657593;
  background: #f6f9ff;
  font-size: 13px;
  text-align: center;
}

.preview-table {
  width: 100%;
  color: #44516a;
}

.preview-table :deep(.el-table__cell),
.preview-table :deep(.el-table__cell .cell) {
  text-align: center;
  vertical-align: middle;
}

.preview-table :deep(th.el-table__cell) {
  color: var(--secondary-color);
  font-weight: 700;
  background: #f6f6fe !important;
}

.preview-table :deep(.el-table__body td:first-child) {
  color: var(--secondary-color);
  font-weight: 600;
  background: #f8faff;
}

.preview-table :deep(.monthly-total-row td.el-table__cell) {
  color: var(--secondary-color);
  font-weight: 700;
  background: #eef3ff !important;
}

.amount-term {
  border: 0;
  border-radius: 4px;
  padding: 2px 4px;
  background: transparent;
  color: var(--primary-color);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.amount-term:hover {
  background: #edf1ff;
  text-decoration: underline;
}

.amount-term:focus-visible {
  outline: 2px solid var(--primary-color);
}

.amount-separator {
  color: #8794ab;
}

.payable-amount {
  color: var(--secondary-color);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.member-name {
  color: var(--secondary-color);
  font-weight: 700;
}

/* 202609281715 - 悬停卡片只呈现摘要，避免长名单挤占表格视野。 */
:global(.monthly-fee-popover.el-popper) {
  padding: 0 !important;
  overflow: hidden;
  border: 1px solid #dce6fb !important;
  border-radius: 10px !important;
  box-shadow: 0 12px 32px rgba(1, 41, 112, 0.16) !important;
}

.fee-hover-card {
  padding: 15px 16px 0;
  color: #44516a;
  text-align: center;
}

.fee-hover-card__tags,
.task-dialog-tags {
  display: flex;
  justify-content: center;
  gap: 7px;
}

.fee-hover-card__title {
  display: -webkit-box;
  margin: 10px 0 6px;
  overflow: hidden;
  color: var(--secondary-color);
  font-size: 15px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.fee-hover-card__amount {
  color: var(--primary-color);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.fee-hover-card__amount small {
  display: block;
  color: #7a87a0;
  font-size: 12px;
  font-weight: 400;
}

.fee-hover-card__meta {
  margin: 10px 0 14px;
  color: #63728e;
  font-size: 12px;
}

.fee-hover-card__meta span {
  margin: 0 4px;
  color: #aab5c8;
}

.fee-hover-card__footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin: 0 -16px;
  padding: 9px 10px;
  color: var(--primary-color);
  background: #f5f7ff;
  font-size: 12px;
  font-weight: 600;
}

:global(.monthly-task-dialog.el-dialog) {
  max-width: calc(100vw - 24px);
  max-height: calc(100dvh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
}

:global(.monthly-task-dialog .el-dialog__header) {
  margin: 0;
  padding: 16px 22px;
  border-bottom: 1px solid #e8edf7;
}

:global(.monthly-task-dialog .el-dialog__body) {
  padding: 20px 22px 22px;
  overflow-y: auto;
}

.task-dialog-heading {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 10px;
  color: var(--secondary-color);
  font-size: 18px;
  font-weight: 700;
}

.task-dialog-heading small {
  color: #8a97ac;
  font-size: 12px;
  font-weight: 400;
}

.task-dialog-overview {
  text-align: center;
}

.task-dialog-overview h3 {
  margin: 12px 0 18px;
  color: var(--secondary-color);
  font-size: 19px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.task-amount-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.task-amount-summary > div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 15px 8px;
  border: 1px solid #e1e8fb;
  border-radius: 8px;
  background: #f6f9ff;
  text-align: center;
}

.task-amount-summary span,
.task-facts span {
  color: #7b89a2;
  font-size: 12px;
}

.task-amount-summary strong {
  color: var(--secondary-color);
  font-size: 24px;
  font-variant-numeric: tabular-nums;
}

.task-amount-summary > div:first-child strong {
  color: var(--primary-color);
}

.task-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 20px 0;
}

.task-facts > div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 0;
  text-align: center;
}

.task-facts strong {
  color: #344460;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.task-participants-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.task-participants-heading h4 {
  color: var(--secondary-color);
  font-size: 15px;
}

.task-participants-heading span,
.task-detail-note {
  color: #8995a8;
  font-size: 12px;
}

.task-participants-table :deep(.el-table__cell .cell) {
  text-align: center;
}

.task-participants-table :deep(th.el-table__cell) {
  color: var(--secondary-color);
  background: #f6f6fe !important;
}

.task-participants-table strong {
  color: var(--secondary-color);
  font-variant-numeric: tabular-nums;
}

.task-detail-note {
  margin: 12px 0 0;
  text-align: center;
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

  .preview-heading {
    justify-content: center;
    text-align: center;
  }

  .preview-heading__summary {
    width: 100%;
  }

  .task-dialog-heading {
    flex-direction: column;
    align-items: center;
    gap: 0;
  }

  .task-amount-summary strong {
    font-size: 20px;
  }
}
</style>
