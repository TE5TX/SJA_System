<template>
  <div class="fee-submit">
    <div class="page-title">
      <h1>任务提交</h1>
      <p>按部门和发布平台录入作品信息与作者。</p>
    </div>

    <el-row :gutter="20" class="fee-submit-grid">
      <el-col :xs="24" :sm="24" :md="24" :lg="16" :xl="15">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>作品信息</span>
              <el-radio-group v-model="form.workType" class="work-type-selector" @change="resetWorkInfo">
                <el-radio-button v-for="item in workTypeOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-form :model="form" label-width="112px" class="work-form mobile-form-top-label">
            <template v-if="form.workType === 'EDITORIAL'">
              <el-form-item label="文章标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入文章标题" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="发布平台" required>
                    <el-select v-model="form.workInfo.platform" @change="handlePlatformChange">
                      <el-option v-for="platform in platformOptions" :key="platform" :label="platform" :value="platform" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="类别" required>
                    <el-select v-model="form.workInfo.category" placeholder="请选择类别">
                      <el-option v-for="option in categoryOptions" :key="option" :label="option" :value="option" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <!-- 202609081000 - 修复了采编部微信未发布任务仍要求填写阅读量的问题。 -->
              <el-form-item v-if="form.workInfo.platform === '微信' && form.workInfo.isPublished === 1" label="阅读量">
                <el-input-number
                  v-model="form.workInfo.readCount"
                  :min="0"
                  :step="1000"
                  controls-position="right"
                  @focus="selectNumberInputText"
                  @click="selectNumberInputText"
                />
              </el-form-item>
              <el-form-item v-if="form.workInfo.platform === '校报'" label="字数" required>
                <el-input-number
                  v-model="form.workInfo.wordCount"
                  :min="0"
                  :step="1000"
                  controls-position="right"
                  @focus="selectNumberInputText"
                  @click="selectNumberInputText"
                />
              </el-form-item>
              <el-form-item label="文章链接">
                <el-input v-model="form.workInfo.articleLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else-if="form.workType === 'PHOTO'">
              <el-form-item label="作品标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入摄影作品标题" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="24">
                  <el-form-item label="发布平台" required>
                    <el-select v-model="form.workInfo.platform" @change="handlePlatformChange">
                      <el-option v-for="platform in platformOptions" :key="platform" :label="platform" :value="platform" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item v-if="form.workInfo.platform === '微信'" label="阅读量">
                <el-input-number
                  v-model="form.workInfo.readCount"
                  :min="0"
                  :step="1000"
                  controls-position="right"
                  @focus="selectNumberInputText"
                  @click="selectNumberInputText"
                />
              </el-form-item>
              <el-form-item label="文章链接">
                <el-input v-model="form.workInfo.articleLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else-if="form.workType === 'VIDEO'">
              <el-form-item label="视频标题" required>
                <el-input v-model="form.workInfo.videoTitle" placeholder="请输入视频标题" />
              </el-form-item>
              <el-form-item label="类别" required>
                <el-select v-model="form.workInfo.category">
                  <el-option label="风景类系列短视频" value="风景类系列短视频" />
                  <el-option label="活动新闻类视频" value="活动新闻类视频" />
                  <el-option label="原创类短视频" value="原创类短视频" />
                  <el-option label="原创类长视频" value="原创类长视频" />
                </el-select>
              </el-form-item>
              <el-form-item label="点赞数">
                <el-input-number
                  v-model="form.workInfo.likeCount"
                  :min="0"
                  controls-position="right"
                  @focus="selectNumberInputText"
                  @click="selectNumberInputText"
                />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="官方转载">
                    <el-switch v-model="form.workInfo.isReposted" :active-value="1" :inactive-value="0" />
                  </el-form-item>
                </el-col>
                <el-col v-if="form.workInfo.category === '原创类短视频'" :xs="24" :sm="12">
                  <el-form-item label="复杂剪辑">
                    <el-switch v-model="form.workInfo.isComplexEdit" :active-value="1" :inactive-value="0" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="视频链接">
                <el-input v-model="form.workInfo.videoLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else-if="form.workType === 'VISUAL'">
              <el-form-item label="作品标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入视觉作品标题" />
              </el-form-item>
              <el-form-item label="类别" required>
                <el-select v-model="form.workInfo.category">
                  <el-option v-for="option in visualCategoryOptions" :key="option" :label="option" :value="option" />
                </el-select>
              </el-form-item>
              <el-form-item label="作品链接">
                <el-input v-model="form.workInfo.articleLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="任务标题" required>
                <el-input v-model="form.workInfo.title" placeholder="如：2026年7月主席团补贴" />
              </el-form-item>
              <el-form-item label="补贴类型" required>
                <el-select v-model="form.workInfo.category">
                  <el-option v-for="option in subsidyCategoryOptions" :key="option" :label="option" :value="option" />
                </el-select>
              </el-form-item>
              <el-form-item label="补贴月份" required>
                <el-date-picker
                  v-model="form.workInfo.subsidyMonth"
                  type="month"
                  format="YYYY年MM月"
                  value-format="YYYY-MM"
                  :editable="false"
                  :clearable="false"
                />
              </el-form-item>
            </template>

            <el-row v-if="form.workType !== 'SUBSIDY'" :gutter="16">
              <!-- 202609281554 - 未发布任务继续隐藏发布日期，由后端在入库时按任务创建日期填写。 -->
              <el-col v-if="form.workInfo.isPublished === 1" :xs="24" :sm="12">
                <el-form-item label="发布时间" required>
                  <div
                    class="wheel-date-picker"
                    @wheel.capture.prevent.stop="adjustPublishDateByWheel"
                    @touchstart.passive="recordPublishDateTouchStart"
                    @touchend.passive="adjustPublishDateBySwipe"
                  >
                    <el-date-picker
                      v-model="form.workInfo.publishDate"
                      type="date"
                      format="YYYY年MM月DD日"
                      value-format="YYYY-MM-DD"
                      :editable="false"
                      :clearable="false"
                      :teleported="false"
                      popper-class="single-year-date-picker"
                      @visible-change="handlePublishDatePickerVisible"
                    />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="是否已发布">
                  <el-switch v-model="form.workInfo.isPublished" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="form.workType !== 'SUBSIDY' && canManageFeeAmount" :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="特殊节点">
                  <el-switch v-model="form.workInfo.isSpecialNode" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
              <el-col v-if="form.workInfo.isSpecialNode === 1" :xs="24" :sm="12">
                <el-form-item label="发放倍数" required>
                  <el-segmented v-model="form.workInfo.specialMultiplier" :options="[2, 3]" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注">
              <el-input v-model="form.workInfo.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="9" class="contributor-column">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ contributorPanelTitle }}</span>
            </div>
          </template>

          <template v-if="form.workType === 'SUBSIDY'">
            <el-alert
              class="role-match-alert"
              type="info"
              :closable="false"
              show-icon
              :title="`已按“${form.workInfo.category || '补贴类型'}”角色自动匹配 ${form.contributors.length} 人`"
            />

            <div v-for="item in form.contributors" :key="item.key" class="author-row">
              <div class="author-identity">
                <el-avatar class="person-avatar" :size="38">{{ authorInitial(item.userId) }}</el-avatar>
                <div>
                  <strong>{{ authorName(item.userId) }}</strong>
                  <span>{{ authorStudentNo(item.userId) }}</span>
                </div>
              </div>
              <!-- 202609081000 - 完成了月度补贴自动匹配人员支持单人删除的任务。 -->
              <el-button link type="danger" :icon="Delete" @click="removeSubsidyContributor(item.userId)">删除</el-button>
            </div>
            <el-empty v-if="form.contributors.length === 0" description="当前角色暂无有效人员" />
          </template>

          <template v-else-if="usesAuthorSelector">
            <AuthorInput
              v-model="authorNamesInput"
              :users="users"
              :person-label="inputPersonLabel"
              @validation-change="handleAuthorValidation"
            />

            <div v-for="(item, index) in form.contributors" :key="item.key" class="author-row">
              <div class="author-identity">
                <el-avatar class="person-avatar" :size="38">{{ authorInitial(item.userId) }}</el-avatar>
                <div>
                  <strong>{{ authorName(item.userId) }}</strong>
                  <span>{{ authorStudentNo(item.userId) }}</span>
                </div>
              </div>
              <div v-if="needsContributorWorkCount" class="work-count-control">
                <span>{{ form.workType === 'PHOTO' ? '选图数量' : '作品数量' }}</span>
                <el-input-number
                  v-model="item.workCount"
                  :min="1"
                  :step="1"
                  controls-position="right"
                  aria-label="作品数量"
                />
              </div>
              <el-button link type="danger" :icon="Delete" @click="removeContributor(index)">删除</el-button>
            </div>
            <el-empty v-if="form.contributors.length === 0" description="请选择至少一位作者" />
          </template>

          <template v-else-if="form.workType === 'VIDEO'">
            <div class="video-people-input">
              <span>拍摄人员</span>
              <AuthorInput
                v-model="shooterNamesInput"
                :users="users"
                person-label="拍摄人员"
                @validation-change="validation => handleVideoValidation('SHOOTER', validation)"
              />
            </div>
            <div class="video-people-input">
              <span>剪辑人员</span>
              <AuthorInput
                v-model="editorNamesInput"
                :users="users"
                person-label="剪辑人员"
                @validation-change="validation => handleVideoValidation('EDITOR', validation)"
              />
            </div>
            <el-empty v-if="form.contributors.length === 0" description="请输入拍摄人员或剪辑人员" />
          </template>

          <div class="submit-panel">
            <div>
              <span>参与人数</span>
              <strong>{{ form.contributors.length }} 人</strong>
            </div>
            <el-button type="success" size="large" :loading="submitting" @click="handleSubmit">提交任务</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuthorInput from '@/components/AuthorInput.vue'
import { getFeeContributors, submitFee } from '@/api/fee'
import { waitForAsyncTask } from '@/api/asyncTask'
import { useUserStore } from '@/store/user'
import { useResponsive } from '@/composables/useResponsive'

const userStore = useUserStore()
const { isMobile } = useResponsive()
const platformOptions = ['微信', '院网', '校报']
const editorialCategoryOptions = {
  微信: ['原创类', '半原创类'],
  院网: ['莲峰群英', '学校新闻（含学校要闻、校园时讯）'],
  校报: ['原创头版头条', '原创一般稿件', '重新编辑类稿件']
}
const visualCategoryOptions = ['普通消息类', '原创美文（不含制图）', '原创美文（含部分制图）', '互动、长图、SVG动图、手绘', '校园网首页大图']
const subsidyCategoryOptions = ['微信统筹编辑', '排版编辑', '校对', '主席团成员', '校园巡查组长', '校园巡查组员']
const allWorkTypeOptions = [
  { label: '采编部', value: 'EDITORIAL' },
  { label: '摄影部', value: 'PHOTO' },
  { label: '视觉部', value: 'VISUAL' },
  { label: '视频部', value: 'VIDEO' },
  { label: '月度补贴', value: 'SUBSIDY' }
]
const workTypeDeptMap = { EDITORIAL: 'EDITORIAL', PHOTO: 'PHOTOGRAPHY', VIDEO: 'VIDEO', VISUAL: 'VISUAL', SUBSIDY: 'MANAGEMENT' }

const roleCodes = computed(() => userStore.userInfo?.roleCodes || [])
const deptCodes = computed(() => userStore.userInfo?.deptCodes || [])
const highestRoleCode = computed(() => roleCodes.value.length ? Math.min(...roleCodes.value) : 13)
const canViewAllTaskTypes = computed(() => [0, 1, 2, 3].includes(highestRoleCode.value))
const canManageFeeAmount = computed(() => [0, 3].includes(highestRoleCode.value))
const workTypeOptions = computed(() => canViewAllTaskTypes.value
  ? allWorkTypeOptions
  : allWorkTypeOptions.filter(item => deptCodes.value.includes(workTypeDeptMap[item.value])))

const users = ref([])
const submitting = ref(false)
const authorNamesInput = ref('')
const shooterNamesInput = ref('')
const editorNamesInput = ref('')
const authorInputValid = ref(true)
const shooterInputValid = ref(true)
const editorInputValid = ref(true)
const form = reactive({ workType: 'EDITORIAL', workInfo: {}, contributors: [] })
const draftReady = ref(false)
const DRAFT_VERSION = 1
const usesAuthorSelector = computed(() => ['EDITORIAL', 'PHOTO', 'VISUAL'].includes(form.workType))
const needsContributorWorkCount = computed(() =>
  (form.workType === 'PHOTO' && ['院网', '校报'].includes(form.workInfo.platform)) ||
  (form.workType === 'VISUAL' && form.workInfo.category === '校园网首页大图'))
const contributorPanelTitle = computed(() => form.workType === 'SUBSIDY'
  ? '补贴人员'
  : (form.workType === 'VISUAL' ? '设计人员' : (usesAuthorSelector.value ? '作者信息' : '参与人员')))
const inputPersonLabel = computed(() => form.workType === 'SUBSIDY'
  ? '补贴人员'
  : (form.workType === 'VISUAL' ? '设计人员' : '作者'))
const categoryOptions = computed(() => editorialCategoryOptions[form.workInfo.platform] || [])
const subsidyRoleCodes = {
  微信统筹编辑: [5],
  排版编辑: [6],
  校对: [7],
  主席团成员: [1, 3, 4],
  校园巡查组长: [11],
  校园巡查组员: [12]
}

// 202608101716 - 分别记住采编部和摄影部上次选择的发布平台。
const preferredPlatform = workType => {
  const value = localStorage.getItem(`sja-fee-submit-platform-${workType.toLowerCase()}`)
  return platformOptions.includes(value) ? value : '微信'
}

const rememberPlatform = (workType, platform) => {
  if (['EDITORIAL', 'PHOTO'].includes(workType) && platformOptions.includes(platform)) {
    localStorage.setItem(`sja-fee-submit-platform-${workType.toLowerCase()}`, platform)
  }
}

const createWorkInfo = workType => {
  const base = {
    publishDate: new Date().toISOString().slice(0, 10),
    isPublished: 1,
    isSpecialNode: 0,
    specialMultiplier: 2,
    remark: ''
  }
  if (workType === 'EDITORIAL') {
    const platform = preferredPlatform(workType)
    return { ...base, title: '', platform, category: editorialCategoryOptions[platform][0], wordCount: 0, readCount: 0, articleLink: '' }
  }
  if (workType === 'PHOTO') {
    return { ...base, title: '', platform: preferredPlatform(workType), category: '', readCount: 0, articleLink: '' }
  }
  if (workType === 'VIDEO') {
    // 202608111533 - 修复了视频部提交任务仍要求录入无计费作用的视频时长且点赞数不便直接输入的问题。
    return { ...base, videoTitle: '', category: '活动新闻类视频', likeCount: 0, isReposted: 0, isComplexEdit: 0, reprintStatus: '无', videoLink: '' }
  }
  if (workType === 'VISUAL') {
    return { ...base, title: '', category: visualCategoryOptions[0], authorCount: 0, articleLink: '' }
  }
  return { title: '', category: subsidyCategoryOptions[0], subsidyMonth: new Date().toISOString().slice(0, 7), remark: '' }
}

const createContributor = (userId = '', roleInWork = 'AUTHOR') => ({
  key: `${Date.now()}-${Math.random()}`,
  userId,
  roleInWork,
  workCount: 1,
  feeAmount: 0,
  remark: ''
})

const resetWorkInfo = () => {
  form.workInfo = createWorkInfo(form.workType)
  form.contributors = []
  authorNamesInput.value = ''
  shooterNamesInput.value = ''
  editorNamesInput.value = ''
  authorInputValid.value = true
  shooterInputValid.value = true
  editorInputValid.value = true
}

// 202608120848 - 修复了稿费提交页面刷新后未提交的表单和参与人员数据丢失的问题。
const feeDraftKey = () => {
  const info = userStore.userInfo || {}
  const identity = info.id ?? info.userId ?? info.username ?? info.studentNo
  return identity == null || identity === '' ? '' : `sja-fee-submit-draft-v${DRAFT_VERSION}-${identity}`
}

const saveFeeDraft = () => {
  const key = feeDraftKey()
  if (!key || !draftReady.value) return
  try {
    localStorage.setItem(key, JSON.stringify({
      version: DRAFT_VERSION,
      savedAt: Date.now(),
      workType: form.workType,
      workInfo: { ...form.workInfo },
      contributors: form.contributors.map(item => ({
        userId: item.userId,
        roleInWork: item.roleInWork,
        workCount: item.workCount,
        feeAmount: item.feeAmount,
        remark: item.remark || ''
      })),
      authorNamesInput: authorNamesInput.value,
      shooterNamesInput: shooterNamesInput.value,
      editorNamesInput: editorNamesInput.value
    }))
  } catch (error) {
    console.warn('Failed to save fee submission draft:', error)
  }
}

const clearFeeDraft = () => {
  const key = feeDraftKey()
  if (key) localStorage.removeItem(key)
}

const restoreFeeDraft = () => {
  const key = feeDraftKey()
  if (!key) return false
  try {
    const stored = localStorage.getItem(key)
    if (!stored) return false
    const draft = JSON.parse(stored)
    const allowedWorkTypes = workTypeOptions.value.map(item => item.value)
    if (draft?.version !== DRAFT_VERSION || !allowedWorkTypes.includes(draft.workType)) {
      localStorage.removeItem(key)
      return false
    }

    form.workType = draft.workType
    form.workInfo = { ...createWorkInfo(draft.workType), ...(draft.workInfo || {}) }
    form.contributors = Array.isArray(draft.contributors)
      ? draft.contributors
        .filter(item => item?.userId != null && item?.roleInWork)
        .map(item => ({ ...createContributor(item.userId, item.roleInWork), ...item }))
      : []
    authorNamesInput.value = String(draft.authorNamesInput || '')
    shooterNamesInput.value = String(draft.shooterNamesInput || '')
    editorNamesInput.value = String(draft.editorNamesInput || '')
    return true
  } catch (error) {
    console.warn('Failed to restore fee submission draft:', error)
    localStorage.removeItem(key)
    return false
  }
}

const handlePlatformChange = () => {
  rememberPlatform(form.workType, form.workInfo.platform)
  if (form.workType === 'EDITORIAL') {
    form.workInfo.category = categoryOptions.value[0] || ''
  } else if (form.workType === 'PHOTO') {
    form.workInfo.category = ''
  }
  form.contributors.forEach(item => { item.workCount = 1 })
}

const formatDateValue = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const shiftPublishDateMonth = offset => {
  const parts = String(form.workInfo.publishDate || '').split('-').map(Number)
  const current = parts.length === 3 && parts.every(Number.isFinite)
    ? new Date(parts[0], parts[1] - 1, parts[2])
    : new Date()
  const originalDay = current.getDate()
  current.setDate(1)
  current.setMonth(current.getMonth() + offset)
  const lastDay = new Date(current.getFullYear(), current.getMonth() + 1, 0).getDate()
  current.setDate(Math.min(originalDay, lastDay))
  // 202608121041 - 优化了稿费提交日期选择并将鼠标滚轮调整单位由日期改为月份。
  form.workInfo.publishDate = formatDateValue(current)
}

const adjustPublishDateByWheel = event => {
  event.preventDefault()
  shiftPublishDateMonth(event.deltaY > 0 ? 1 : -1)
}

const publishDateTouchStart = ref(null)

const recordPublishDateTouchStart = event => {
  if (!isMobile.value) return
  const touch = event.changedTouches?.[0]
  publishDateTouchStart.value = touch ? { x: touch.clientX, y: touch.clientY } : null
}

const adjustPublishDateBySwipe = event => {
  if (!isMobile.value || !publishDateTouchStart.value) return
  const touch = event.changedTouches?.[0]
  if (!touch) return

  const deltaX = touch.clientX - publishDateTouchStart.value.x
  const deltaY = touch.clientY - publishDateTouchStart.value.y
  publishDateTouchStart.value = null

  // 只处理明显的横向手势，保证页面正常上下滚动；左滑下个月，右滑上个月。
  if (Math.abs(deltaX) < 45 || Math.abs(deltaX) <= Math.abs(deltaY)) return
  shiftPublishDateMonth(deltaX < 0 ? 1 : -1)
}

let publishDatePanel = null

const detachPublishDatePanelTouch = () => {
  if (!publishDatePanel) return
  publishDatePanel.removeEventListener('touchstart', recordPublishDateTouchStart)
  publishDatePanel.removeEventListener('touchend', adjustPublishDateBySwipe)
  publishDatePanel = null
}

const handlePublishDatePickerVisible = visible => {
  detachPublishDatePanelTouch()
  if (!visible || !isMobile.value) return

  nextTick(() => {
    publishDatePanel = document.querySelector('.single-year-date-picker')
    publishDatePanel?.addEventListener('touchstart', recordPublishDateTouchStart, { passive: true })
    publishDatePanel?.addEventListener('touchend', adjustPublishDateBySwipe, { passive: true })
  })
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

const roleForCurrentPeople = () => {
  if (form.workType === 'VISUAL') return 'DESIGNER'
  if (form.workType === 'SUBSIDY') return 'MEMBER'
  return 'AUTHOR'
}

const handleAuthorValidation = validation => {
  authorInputValid.value = validation.valid
  const role = roleForCurrentPeople()
  const existing = new Map(form.contributors.map(item => [String(item.userId), item]))
  form.contributors = (validation.matchedUsers || []).map(user =>
    existing.get(String(user.id)) || createContributor(user.id, role))
}

// 202608131602 - 修复了月度补贴必须手动输入人员名单而未使用系统角色数据的问题。
const syncSubsidyContributors = () => {
  if (form.workType !== 'SUBSIDY') return
  const expectedRoleCodes = subsidyRoleCodes[form.workInfo.category] || []
  const existing = new Map(form.contributors.map(item => [String(item.userId), item]))
  form.contributors = users.value
    .filter(user => (user.roleCodes || []).some(roleCode => expectedRoleCodes.includes(Number(roleCode))))
    .map(user => existing.get(String(user.id)) || createContributor(user.id, 'MEMBER'))
  authorNamesInput.value = ''
  authorInputValid.value = true
}

const handleVideoValidation = (role, validation) => {
  if (role === 'SHOOTER') shooterInputValid.value = validation.valid
  else editorInputValid.value = validation.valid
  const retained = form.contributors.filter(item => item.roleInWork !== role)
  const existing = new Map(form.contributors
    .filter(item => item.roleInWork === role)
    .map(item => [String(item.userId), item]))
  const matched = (validation.matchedUsers || []).map(user =>
    existing.get(String(user.id)) || createContributor(user.id, role))
  form.contributors = role === 'SHOOTER' ? [...matched, ...retained] : [...retained, ...matched]
}

const removeContributor = index => {
  form.contributors.splice(index, 1)
  authorNamesInput.value = form.contributors.map(item => authorName(item.userId)).join(',')
}

/**
 * 月度补贴人员由角色自动匹配，但允许本次任务排除个别不应参与补贴的成员。
 */
const removeSubsidyContributor = userId => {
  form.contributors = form.contributors.filter(item => String(item.userId) !== String(userId))
}

const findAuthor = userId => users.value.find(user => String(user.id) === String(userId))
const authorName = userId => findAuthor(userId)?.realName || `用户 ${userId}`
const authorStudentNo = userId => findAuthor(userId)?.studentNo || findAuthor(userId)?.username || '-'
const authorInitial = userId => authorName(userId).slice(0, 1)

const validate = () => {
  const title = form.workType === 'VIDEO' ? form.workInfo.videoTitle : form.workInfo.title
  if (!title) return '请填写作品标题'
  if (form.workType !== 'SUBSIDY' && form.workInfo.isPublished === 1 && !form.workInfo.publishDate) {
    return '请选择发布时间'
  }
  if (form.workType === 'SUBSIDY' && !form.workInfo.subsidyMonth) return '请选择补贴月份'
  // 202608101716 - 移除了摄影部稿费提交中的类别字段及对应校验。
  const categoryRequired = form.workType !== 'PHOTO'
  if (categoryRequired && !form.workInfo.category) return '请选择类别'
  if (form.workType !== 'SUBSIDY' && form.workInfo.isSpecialNode === 1 && ![2, 3].includes(Number(form.workInfo.specialMultiplier))) {
    return '请选择特殊节点发放倍数'
  }
  if (form.workType === 'EDITORIAL' && form.workInfo.platform === '校报' && Number(form.workInfo.wordCount) <= 0) {
    return '校报文章字数必须大于 0'
  }
  if (usesAuthorSelector.value && !authorInputValid.value) return '请修正未识别或重名的人员'
  if (form.workType === 'VIDEO' && (!shooterInputValid.value || !editorInputValid.value)) return '请修正未识别或重名的视频参与人员'
  if (form.contributors.length === 0) {
    if (form.workType === 'SUBSIDY') return '当前补贴类型未配置有效人员，请先在人员管理中维护对应角色'
    return usesAuthorSelector.value ? '请添加作者' : '请添加参与人员'
  }
  if (form.contributors.some(item => !item.userId || !item.roleInWork)) return '请补全参与人员和角色'
  if (needsContributorWorkCount.value && form.contributors.some(item => Number(item.workCount) <= 0)) {
    return '请填写每位人员对应的作品数量'
  }
  return ''
}

const handleSubmit = async () => {
  const message = validate()
  if (message) {
    ElMessage.warning(message)
    return
  }

  submitting.value = true
  try {
    const submittedWorkInfo = { ...form.workInfo }
    // 202609281554 - 未发布任务不发送客户端日期，避免覆盖后端按数据库创建时间填写的隐藏发布日期。
    if (form.workType !== 'SUBSIDY' && submittedWorkInfo.isPublished !== 1) {
      delete submittedWorkInfo.publishDate
    }
    // 202609081000 - 修复了采编部微信未发布任务仍写入无意义阅读量的问题。
    if (form.workType === 'EDITORIAL' && submittedWorkInfo.platform === '微信' && submittedWorkInfo.isPublished !== 1) {
      delete submittedWorkInfo.readCount
    }
    const accepted = await submitFee({
      workType: form.workType,
      workInfo: submittedWorkInfo,
      contributors: form.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: form.workType === 'VISUAL' ? 'DESIGNER' : (form.workType === 'SUBSIDY' ? 'MEMBER' : (usesAuthorSelector.value ? 'AUTHOR' : item.roleInWork)),
        workCount: needsContributorWorkCount.value ? Number(item.workCount) : null,
        feeAmount: null,
        remark: item.remark || ''
      }))
    })
    ElMessage.info('稿费任务已进入队列，正在处理')
    // 202608192148 - 修复了后台尚未写入完成时前端提前清空稿费表单的问题。
    await waitForAsyncTask(accepted.data.taskId, accepted.data.statusToken)
    ElMessage.success('稿费任务已提交')
    draftReady.value = false
    clearFeeDraft()
    resetWorkInfo()
    await nextTick()
    draftReady.value = true
  } catch (error) {
    if (error.isAsyncTaskFailure) ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

const loadUsers = async () => {
  try {
    const res = await getFeeContributors()
    users.value = res.data || []
  } catch {
    users.value = []
  }
}

resetWorkInfo()
onMounted(async () => {
  if (!workTypeOptions.value.some(item => item.value === form.workType) && workTypeOptions.value.length) {
    form.workType = workTypeOptions.value[0].value
    resetWorkInfo()
  }
  await loadUsers()
  const restored = restoreFeeDraft()
  syncSubsidyContributors()
  draftReady.value = true
  if (restored) ElMessage.success('已恢复未提交的稿费草稿')
})

onBeforeUnmount(detachPublishDatePanelTouch)

watch(workTypeOptions, options => {
  if (!options.some(item => item.value === form.workType) && options.length) {
    form.workType = options[0].value
    resetWorkInfo()
  }
})

watch(
  () => [form.workType, form.workInfo.category, users.value],
  syncSubsidyContributors
)

watch(
  () => ({
    workType: form.workType,
    workInfo: form.workInfo,
    contributors: form.contributors,
    authorNamesInput: authorNamesInput.value,
    shooterNamesInput: shooterNamesInput.value,
    editorNamesInput: editorNamesInput.value
  }),
  saveFeeDraft,
  { deep: true, flush: 'sync' }
)
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.card-header,
.contributor-title,
.submit-panel,
.author-row,
.author-identity {
  display: flex;
  align-items: center;
}

.card-header,
.contributor-title,
.submit-panel,
.author-row {
  justify-content: space-between;
  gap: 12px;
}

.work-form :deep(.el-select),
.work-form :deep(.el-date-editor),
.work-form :deep(.el-input-number),
.contributor-row :deep(.el-select),
.contributor-row :deep(.el-input-number),
.wheel-date-picker,
.author-selector {
  width: 100%;
}

/* 202608121123 - 移除了日期选择器中容易误触的跨年双箭头按钮。 */
:global(.single-year-date-picker .d-arrow-left),
:global(.single-year-date-picker .d-arrow-right) {
  display: none;
}

.author-selector {
  margin-bottom: 14px;
}

.video-people-input + .video-people-input {
  margin-top: 18px;
}

.video-people-input > span {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

:deep(.author-input) {
  margin-bottom: 14px;
}

.role-match-alert {
  margin-bottom: 14px;
}

.author-row,
.contributor-row {
  margin-bottom: 12px;
  border: 1px solid #ebeef4;
  border-radius: 6px;
  background: #fff;
}

.author-row {
  min-height: 68px;
  padding: 10px 12px;
}

.author-identity {
  min-width: 0;
  gap: 10px;
}

:deep(.el-avatar.person-avatar) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 38px;
  background-color: #4169e1 !important;
  color: #fff !important;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  text-align: center;
}

.author-identity > div {
  min-width: 0;
}

.author-identity strong,
.author-identity span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-identity span {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
}

.work-count-control {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.work-count-control :deep(.el-input-number) {
  width: 118px;
  flex: 0 0 118px;
}

.contributor-row {
  padding: 14px 14px 2px;
}

.contributor-title {
  margin-bottom: 10px;
  color: var(--secondary-color);
}

.submit-panel {
  margin-top: 18px;
  padding: 16px;
  border-radius: 6px;
  background: #f6f9ff;
}

.submit-panel span,
.submit-panel strong {
  display: block;
}

.submit-panel span {
  color: #6b7280;
  font-size: 13px;
}

.submit-panel strong {
  color: var(--secondary-color);
  font-size: 24px;
}

@media (max-width: 767px) {
  .fee-submit-grid {
    row-gap: 14px;
  }

  .card-header,
  .submit-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .author-row {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  /* 类型按钮允许横向滚动，避免五种业务类型在窄屏上被压成不可读文字。 */
  .card-header :deep(.el-radio-group) {
    display: flex;
    flex-wrap: nowrap;
    justify-content: flex-start;
    max-width: 100%;
    width: 100%;
    overflow-x: auto;
    padding: 0 0 4px;
    scrollbar-width: thin;
  }

  .card-header :deep(.el-radio-button) {
    flex: 0 0 auto;
  }

  .card-header :deep(.el-radio-button__inner) {
    min-width: 72px;
    min-height: 40px;
    padding: 10px 14px;
    white-space: nowrap;
  }

  .wheel-date-picker {
    touch-action: pan-y;
  }

  :global(.single-year-date-picker) {
    touch-action: pan-y;
  }

  .work-count-control {
    width: 100%;
    justify-content: space-between;
  }

  .submit-panel {
    position: sticky;
    bottom: calc(8px + env(safe-area-inset-bottom));
    z-index: 10;
    box-shadow: 0 -4px 18px rgba(1, 41, 112, 0.12);
  }

  .submit-panel .el-button {
    width: 100%;
  }
}
</style>
