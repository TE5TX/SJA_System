<template>
  <div class="fee-submit">
    <div class="page-title">
      <h1>稿费提交</h1>
      <p>按部门和发布平台录入作品信息与作者。</p>
    </div>

    <el-row :gutter="20">
      <el-col :xl="15" :lg="16" :md="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>作品信息</span>
              <el-radio-group v-model="form.workType" @change="resetWorkInfo">
                <el-radio-button v-for="item in workTypeOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-form :model="form" label-width="112px" class="work-form">
            <template v-if="form.workType === 'EDITORIAL'">
              <el-form-item label="文章标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入文章标题" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="发布平台" required>
                    <el-select v-model="form.workInfo.platform" @change="handlePlatformChange">
                      <el-option v-for="platform in platformOptions" :key="platform" :label="platform" :value="platform" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="类别" required>
                    <el-select v-model="form.workInfo.category" placeholder="请选择类别">
                      <el-option v-for="option in categoryOptions" :key="option" :label="option" :value="option" />
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
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="时长（秒）">
                    <el-input-number v-model="form.workInfo.videoDurationSec" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="点赞数">
                    <el-input-number v-model="form.workInfo.likeCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="官方转载">
                    <el-switch v-model="form.workInfo.isReposted" :active-value="1" :inactive-value="0" />
                  </el-form-item>
                </el-col>
                <el-col v-if="form.workInfo.category === '原创类短视频'" :span="12">
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
                <el-date-picker v-model="form.workInfo.subsidyMonth" type="month" value-format="YYYY-MM" />
              </el-form-item>
            </template>

            <el-row v-if="form.workType !== 'SUBSIDY'" :gutter="16">
              <el-col :span="12">
                <el-form-item label="发布时间" required>
                  <div class="wheel-date-picker" @wheel.capture.prevent.stop="adjustPublishDateByWheel">
                    <el-date-picker
                      v-model="form.workInfo.publishDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      :teleported="false"
                    />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否已发布">
                  <el-switch v-model="form.workInfo.isPublished" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="form.workType !== 'SUBSIDY' && canManageFeeAmount" :gutter="16">
              <el-col :span="12">
                <el-form-item label="特殊节点">
                  <el-switch v-model="form.workInfo.isSpecialNode" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
              <el-col v-if="form.workInfo.isSpecialNode === 1" :span="12">
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

      <el-col :xl="9" :lg="8" :md="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ contributorPanelTitle }}</span>
            </div>
          </template>

          <template v-if="usesAuthorSelector">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuthorInput from '@/components/AuthorInput.vue'
import { getFeeContributors, submitFee } from '@/api/fee'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
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
const usesAuthorSelector = computed(() => ['EDITORIAL', 'PHOTO', 'VISUAL', 'SUBSIDY'].includes(form.workType))
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
    return { ...base, videoTitle: '', category: '活动新闻类视频', videoDurationSec: 0, likeCount: 0, isReposted: 0, isComplexEdit: 0, reprintStatus: '无', videoLink: '' }
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

const adjustPublishDateByWheel = event => {
  event.preventDefault()
  const parts = String(form.workInfo.publishDate || '').split('-').map(Number)
  const current = parts.length === 3 && parts.every(Number.isFinite)
    ? new Date(parts[0], parts[1] - 1, parts[2])
    : new Date()
  current.setDate(current.getDate() + (event.deltaY > 0 ? 1 : -1))
  form.workInfo.publishDate = formatDateValue(current)
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

const findAuthor = userId => users.value.find(user => String(user.id) === String(userId))
const authorName = userId => findAuthor(userId)?.realName || `用户 ${userId}`
const authorStudentNo = userId => findAuthor(userId)?.studentNo || findAuthor(userId)?.username || '-'
const authorInitial = userId => authorName(userId).slice(0, 1)

const validate = () => {
  const title = form.workType === 'VIDEO' ? form.workInfo.videoTitle : form.workInfo.title
  if (!title) return '请填写作品标题'
  if (form.workType !== 'SUBSIDY' && !form.workInfo.publishDate) return '请选择发布时间'
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
  if (form.contributors.length === 0) return usesAuthorSelector.value ? '请添加作者' : '请添加参与人员'
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
    await submitFee({
      workType: form.workType,
      workInfo: { ...form.workInfo },
      contributors: form.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: form.workType === 'VISUAL' ? 'DESIGNER' : (form.workType === 'SUBSIDY' ? 'MEMBER' : (usesAuthorSelector.value ? 'AUTHOR' : item.roleInWork)),
        workCount: needsContributorWorkCount.value ? Number(item.workCount) : null,
        feeAmount: null,
        remark: item.remark || ''
      }))
    })
    ElMessage.success('稿费任务已提交')
    resetWorkInfo()
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
onMounted(() => {
  if (!workTypeOptions.value.some(item => item.value === form.workType) && workTypeOptions.value.length) {
    form.workType = workTypeOptions.value[0].value
    resetWorkInfo()
  }
  loadUsers()
})

watch(workTypeOptions, options => {
  if (!options.some(item => item.value === form.workType) && options.length) {
    form.workType = options[0].value
    resetWorkInfo()
  }
})
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

@media (max-width: 768px) {
  .card-header,
  .submit-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .author-row {
    align-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
