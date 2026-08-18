<template>
  <div class="fee-standards-page">
    <div class="page-heading">
      <div>
        <h1>稿费标准</h1>
        <p>这些规则会用于新提交或重新编辑任务的稿费自动计算。</p>
      </div>
      <div class="rule-summary">
        <span class="summary-number">{{ standards.length }}</span>
        <span>条生效规则</span>
      </div>
    </div>

    <el-alert
      class="scope-notice"
      type="info"
      :closable="false"
      show-icon
      title="修改标准不会追溯改变已经保存的历史稿费；新提交或重新编辑保存的任务将使用新标准。"
    />

    <el-card class="standard-card" shadow="never">
      <div class="toolbar">
        <div class="business-filters" role="tablist" aria-label="稿费业务筛选">
          <button
            v-for="item in businessOptions"
            :key="item.value"
            type="button"
            class="business-filter"
            :class="{ active: activeBusiness === item.value }"
            @click="activeBusiness = item.value"
          >
            <span>{{ item.label }}</span>
            <span class="filter-count">{{ businessCount(item.value) }}</span>
          </button>
        </div>
        <el-input
          v-model="keyword"
          class="rule-search"
          :prefix-icon="Search"
          clearable
          placeholder="搜索平台、类别或规则"
        />
      </div>

      <el-table
        :data="filteredStandards"
        v-loading="loading"
        border
        stripe
        row-key="id"
        class="standard-table desktop-only"
        empty-text="没有符合条件的稿费规则"
      >
        <el-table-column label="业务" width="130">
          <template #default="{ row }">
            <el-tag :type="businessMeta(row).tagType" effect="light">
              {{ businessMeta(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="适用范围" min-width="310">
          <template #default="{ row }">
            <div class="scope-cell">
              <strong>{{ row.category || categoryFallback(row) }}</strong>
              <span>{{ row.platform || '全部平台' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="计算规则" min-width="280">
          <template #default="{ row }">
            <div class="description-cell">
              <span>{{ row.description || ruleName(row.ruleKey) }}</span>
              <small>{{ ruleHint(row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="当前标准" width="210" align="right">
          <template #default="{ row }">
            <div class="value-cell">
              <strong>{{ formattedRuleValue(row) }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-loading="loading" class="mobile-only">
        <div v-if="!loading && filteredStandards.length === 0" class="mobile-empty">没有符合条件的稿费规则</div>
        <article v-for="rule in filteredStandards" :key="rule.id" class="mobile-record-card">
          <div class="mobile-record-card__header">
            <strong>{{ rule.category || categoryFallback(rule) }}</strong>
            <el-tag :type="businessMeta(rule).tagType" effect="light">{{ businessMeta(rule).label }}</el-tag>
          </div>
          <dl class="mobile-record-card__grid">
            <div><dt>平台</dt><dd>{{ rule.platform || '全部平台' }}</dd></div>
            <div><dt>当前标准</dt><dd class="mobile-rule-value">{{ formattedRuleValue(rule) }}</dd></div>
          </dl>
          <p class="mobile-rule-description">{{ rule.description || ruleName(rule.ruleKey) }}</p>
          <small>{{ ruleHint(rule) }}</small>
          <div class="mobile-record-card__actions">
            <el-button type="primary" plain :icon="Edit" @click="openEdit(rule)">修改标准</el-button>
          </div>
        </article>
      </div>
    </el-card>

    <ResponsiveDialog
      v-model="editVisible"
      title="修改稿费标准"
      desktop-width="520px"
      destroy-on-close
      @closed="resetEdit"
    >
      <template v-if="editingRule">
        <div class="edit-context">
          <div>
            <span>业务</span>
            <strong>{{ businessMeta(editingRule).label }}</strong>
          </div>
          <div>
            <span>适用范围</span>
            <strong>{{ editScopeText }}</strong>
          </div>
          <div>
            <span>计算规则</span>
            <strong>{{ editingRule.description || ruleName(editingRule.ruleKey) }}</strong>
          </div>
        </div>

        <el-form label-position="top" class="edit-form" @submit.prevent>
          <el-form-item :label="editLabel">
            <el-input
              v-model="editValue"
              class="rule-value-input"
              type="number"
              inputmode="decimal"
              :min="0"
              :max="editUnit.max"
              :step="editUnit.step"
              autofocus
            >
              <template v-if="editUnit.prefix" #prepend>{{ editUnit.prefix }}</template>
              <template v-if="editUnit.suffix" #append>{{ editUnit.suffix }}</template>
            </el-input>
          </el-form-item>
        </el-form>

        <div class="change-preview">
          <span>保存后</span>
          <strong>{{ formattedEditValue }}</strong>
        </div>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="保存后立即生效，并会记录到操作日志。"
        />
      </template>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="editValue == null" @click="save">
          确认修改
        </el-button>
      </template>
    </ResponsiveDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getFeeStandards, updateFeeStandard } from '@/api/fee'
import ResponsiveDialog from '@/components/responsive/ResponsiveDialog.vue'

const standards = ref([])
const loading = ref(false)
const saving = ref(false)
const activeBusiness = ref('ALL')
const keyword = ref('')
const editVisible = ref(false)
const editingRule = ref(null)
const editValue = ref(null)

const businessOptions = [
  { value: 'ALL', label: '全部' },
  { value: 'EDITORIAL', label: '采编' },
  { value: 'PHOTO', label: '摄影' },
  { value: 'VIDEO', label: '视频' },
  { value: 'VISUAL', label: '视觉' },
  { value: 'MANAGEMENT', label: '月度补贴' }
]

const businessMap = {
  EDITORIAL: { label: '采编', tagType: 'primary' },
  WECHAT: { label: '采编', tagType: 'primary' },
  PHOTO: { label: '摄影', tagType: 'warning' },
  VIDEO: { label: '视频', tagType: 'danger' },
  VISUAL: { label: '视觉', tagType: '' },
  MANAGEMENT: { label: '月度补贴', tagType: 'info' }
}

const moneyRuleKeys = new Set([
  'PER_PERSON_FEE', 'PER_ARTICLE_FEE', 'PER_1000_WORDS_FEE', 'PER_IMAGE_FEE',
  'BASE_FEE', 'READ_BASE_FEE', 'READ_MIDDLE_FEE', 'READ_HIGH_FEE',
  'COUNT_1_FEE', 'COUNT_2_TO_5_FEE', 'COUNT_OVER_5_FEE',
  'COUNT_UP_TO_2_FEE', 'COUNT_3_TO_4_FEE', 'COUNT_5_PLUS_FEE',
  'LIKE_BONUS', 'REPRINT_BONUS', 'COMPLEX_EDIT_BONUS'
])

const integerRuleKeys = new Set([
  'READ_MIDDLE_THRESHOLD', 'READ_HIGH_THRESHOLD', 'LIKE_THRESHOLD', 'LIKE_STEP'
])

const ruleNameMap = {
  PER_PERSON_FEE: '每人稿费',
  PER_ARTICLE_FEE: '每篇稿费',
  PER_1000_WORDS_FEE: '每千字稿费',
  PER_IMAGE_FEE: '每幅稿费',
  BASE_FEE: '基础稿费',
  SHOOT_RATIO: '拍摄分配比例',
  READ_MIDDLE_THRESHOLD: '中档阅读量起点',
  READ_HIGH_THRESHOLD: '高档阅读量起点',
  READ_BASE_FEE: '基础阅读量稿费',
  READ_MIDDLE_FEE: '中档阅读量稿费',
  READ_HIGH_FEE: '高档阅读量稿费',
  LIKE_THRESHOLD: '点赞奖励起点',
  LIKE_STEP: '点赞奖励档距',
  LIKE_BONUS: '每档点赞奖励',
  REPRINT_BONUS: '官方转载奖励',
  COMPLEX_EDIT_BONUS: '复杂剪辑奖励',
  COUNT_1_FEE: '选用 1 张稿费',
  COUNT_2_TO_5_FEE: '选用 2–5 张稿费',
  COUNT_OVER_5_FEE: '选用 6 张以上稿费',
  COUNT_UP_TO_2_FEE: '选用 2 张以下稿费',
  COUNT_3_TO_4_FEE: '选用 3–4 张稿费',
  COUNT_5_PLUS_FEE: '选用 5 张以上稿费'
}

const load = async () => {
  loading.value = true
  try {
    standards.value = (await getFeeStandards()).data || []
  } finally {
    loading.value = false
  }
}

const businessMeta = row => businessMap[row?.deptCode] || { label: row?.deptCode || '其他', tagType: 'info' }
const ruleName = key => ruleNameMap[key] || key || '计费规则'

const categoryFallback = row => {
  if (row.deptCode === 'VIDEO') return '全部视频类别'
  if (row.deptCode === 'PHOTO') return '按选用图片数量'
  return '全部类别'
}

const ruleHint = row => {
  switch (row.ruleKey) {
    case 'SHOOT_RATIO': return '剩余比例自动分配给剪辑人员'
    case 'READ_MIDDLE_THRESHOLD': return '阅读量超过此数值进入中档'
    case 'READ_HIGH_THRESHOLD': return '阅读量超过此数值进入高档'
    case 'LIKE_THRESHOLD': return '超过此点赞数后开始计算奖励'
    case 'LIKE_STEP': return '每增加此数量的点赞计为一档'
    case 'LIKE_BONUS': return '每达到一个点赞档位增加的金额'
    default: return ruleName(row.ruleKey)
  }
}

const moneySuffix = row => {
  switch (row.ruleKey) {
    case 'PER_PERSON_FEE': return row.deptCode === 'MANAGEMENT' ? ' / 人·月' : ' / 人'
    case 'PER_ARTICLE_FEE': return ' / 篇'
    case 'PER_1000_WORDS_FEE': return ' / 千字'
    case 'PER_IMAGE_FEE': return ' / 幅'
    case 'LIKE_BONUS': return ' / 档'
    default: return ''
  }
}

const numberText = value => Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 4 })

const formattedRuleValue = row => {
  const value = Number(row.ruleValue || 0)
  if (row.ruleKey === 'SHOOT_RATIO') return `${numberText(value * 100)}%`
  if (row.ruleKey.startsWith('READ_') && row.ruleKey.endsWith('_THRESHOLD')) {
    return `${numberText(value)} 次阅读`
  }
  if (row.ruleKey === 'LIKE_THRESHOLD' || row.ruleKey === 'LIKE_STEP') {
    return `${numberText(value)} 个赞`
  }
  if (moneyRuleKeys.has(row.ruleKey)) return `¥${numberText(value)}${moneySuffix(row)}`
  return numberText(value)
}

const belongsToBusiness = (row, value) => {
  if (value === 'EDITORIAL') return ['EDITORIAL', 'WECHAT'].includes(row.deptCode)
  return row.deptCode === value
}

const businessCount = value => value === 'ALL'
  ? standards.value.length
  : standards.value.filter(item => belongsToBusiness(item, value)).length

const filteredStandards = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return standards.value.filter(row => {
    if (activeBusiness.value !== 'ALL' && !belongsToBusiness(row, activeBusiness.value)) return false
    if (!normalizedKeyword) return true
    return [
      businessMeta(row).label, row.platform, row.category,
      row.description, ruleName(row.ruleKey)
    ].some(value => String(value || '').toLowerCase().includes(normalizedKeyword))
  })
})

const editUnit = computed(() => {
  const row = editingRule.value
  if (!row) return { prefix: '', suffix: '', precision: 1, step: 1 }
  if (row.ruleKey === 'SHOOT_RATIO') {
    return { prefix: '', suffix: '%', precision: 1, step: 1, max: 100 }
  }
  if (integerRuleKeys.has(row.ruleKey)) {
    return {
      prefix: '',
      suffix: row.ruleKey.startsWith('READ_') ? '次阅读' : '个赞',
      precision: 0,
      step: row.ruleKey.startsWith('READ_') ? 100 : 10
    }
  }
  return { prefix: '¥', suffix: moneySuffix(row).replace(' / ', '/'), precision: 1, step: 10 }
})

const editLabel = computed(() => {
  if (editingRule.value?.ruleKey === 'SHOOT_RATIO') return '新的拍摄分配比例'
  if (integerRuleKeys.has(editingRule.value?.ruleKey)) return '新的阈值'
  return '新的金额标准'
})

const editScopeText = computed(() => {
  if (!editingRule.value) return '-'
  const row = editingRule.value
  return [row.platform || '全部平台', row.category || categoryFallback(row)].join(' · ')
})

const formattedEditValue = computed(() => {
  if (!editingRule.value || editValue.value == null) return '-'
  const row = { ...editingRule.value }
  row.ruleValue = row.ruleKey === 'SHOOT_RATIO' ? Number(editValue.value) / 100 : editValue.value
  return formattedRuleValue(row)
})

const openEdit = row => {
  editingRule.value = row
  editValue.value = row.ruleKey === 'SHOOT_RATIO'
    ? Number(row.ruleValue || 0) * 100
    : Number(row.ruleValue || 0)
  editVisible.value = true
}

const resetEdit = () => {
  editingRule.value = null
  editValue.value = null
}

const save = async () => {
  if (!editingRule.value || editValue.value == null) return
  saving.value = true
  try {
    const storedValue = editingRule.value.ruleKey === 'SHOOT_RATIO'
      ? Number(editValue.value) / 100
      : Number(editValue.value)
    await updateFeeStandard(editingRule.value.id, {
      ruleValue: storedValue,
      description: editingRule.value.description || '',
      remark: editingRule.value.remark || ''
    })
    ElMessage.success('稿费标准已更新，新任务将使用该标准计算')
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.fee-standards-page {
  --brand: #0f4c9a;
  --brand-soft: #eaf2ff;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.page-heading h1 {
  margin: 0 0 8px;
  color: #0b376f;
  font-size: 28px;
}

.page-heading p {
  margin: 0;
  color: #64748b;
}

.rule-summary {
  display: flex;
  align-items: baseline;
  gap: 6px;
  color: #64748b;
  white-space: nowrap;
}

.summary-number {
  color: var(--brand);
  font-size: 28px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.scope-notice {
  margin-bottom: 16px;
}

.standard-card {
  border: 0;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.business-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.business-filter {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 36px;
  padding: 0 13px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.business-filter:hover {
  color: var(--brand);
  border-color: #93b7e8;
}

.business-filter.active {
  color: #fff;
  background: var(--brand);
  border-color: var(--brand);
  box-shadow: 0 5px 14px rgb(15 76 154 / 18%);
}

.filter-count {
  min-width: 20px;
  padding: 1px 6px;
  color: inherit;
  background: rgb(148 163 184 / 18%);
  border-radius: 999px;
  font-size: 12px;
  text-align: center;
}

.business-filter.active .filter-count {
  background: rgb(255 255 255 / 20%);
}

.rule-search {
  flex: 0 0 270px;
}

.scope-cell,
.description-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.scope-cell strong {
  color: #1e293b;
  font-weight: 600;
}

.scope-cell span,
.description-cell small {
  color: #8491a3;
  font-size: 13px;
}

.description-cell > span {
  color: #334155;
}

.value-cell strong {
  color: #0b5cad;
  font-size: 17px;
  font-variant-numeric: tabular-nums;
}

.edit-context {
  display: grid;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.edit-context > div {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 12px;
}

.edit-context span {
  color: #8491a3;
}

.edit-context strong {
  color: #25364d;
  font-weight: 600;
}

.edit-form {
  margin-top: 20px;
}

.rule-value-input :deep(.el-input__inner) {
  text-align: left;
  font-size: 18px;
  font-weight: 600;
}

.rule-value-input :deep(.el-input-group__prepend),
.rule-value-input :deep(.el-input-group__append) {
  min-width: 48px;
  padding: 0 14px;
  color: #475569;
  background: #f8fafc;
  font-weight: 600;
  text-align: center;
}

.rule-value-input :deep(.el-input-group__prepend) {
  color: #0b5cad;
  font-size: 18px;
}

.change-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 0 16px;
  padding: 12px 14px;
  color: #64748b;
  background: var(--brand-soft);
  border-radius: 8px;
}

.change-preview strong {
  color: var(--brand);
  font-size: 18px;
}

@media (max-width: 900px) {
  .page-heading,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .rule-summary {
    align-self: flex-start;
  }

  .rule-search {
    flex-basis: auto;
    width: 100%;
  }
}

@media (max-width: 767px) {
  .business-filters {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .business-filter {
    flex: 0 0 auto;
    min-height: 44px;
  }

  .mobile-rule-value {
    color: #0b5cad;
    font-weight: 700;
  }

  .mobile-rule-description {
    margin: 12px 0 4px;
    color: #334155;
    line-height: 1.6;
  }

  .mobile-record-card > small {
    color: #8491a3;
  }

  .mobile-record-card__actions .el-button {
    width: 100%;
  }
}
</style>
