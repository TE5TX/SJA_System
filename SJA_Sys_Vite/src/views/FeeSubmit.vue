<template>
  <div class="fee-submit">
    <div class="page-title">
      <h1>稿费提交</h1>
      <p>按部门作品类型录入基础信息和参与人员稿费分配。</p>
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

          <el-form ref="formRef" :model="form" label-width="112px" class="work-form">
            <template v-if="form.workType === 'EDITORIAL'">
              <el-form-item label="文章标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入文章标题" />
              </el-form-item>
              <el-form-item label="发布平台" required>
                <el-select v-model="form.workInfo.platform" placeholder="请选择平台">
                  <el-option label="微信" value="微信" />
                  <el-option label="院网" value="院网" />
                  <el-option label="校报" value="校报" />
                </el-select>
              </el-form-item>
              <el-form-item label="文章类别">
                <el-input v-model="form.workInfo.category" placeholder="如新闻、专访、评论" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="字数">
                    <el-input-number v-model="form.workInfo.wordCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="阅读量">
                    <el-input-number v-model="form.workInfo.readCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="文章链接">
                <el-input v-model="form.workInfo.articleLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else-if="form.workType === 'PHOTO'">
              <el-form-item label="作品标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入摄影作品标题" />
              </el-form-item>
              <el-form-item label="发布平台" required>
                <el-select v-model="form.workInfo.platform" placeholder="请选择平台">
                  <el-option label="微信" value="微信" />
                  <el-option label="院网" value="院网" />
                  <el-option label="校报" value="校报" />
                </el-select>
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="图片数">
                    <el-input-number v-model="form.workInfo.photoCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="阅读量">
                    <el-input-number v-model="form.workInfo.readCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="作品链接">
                <el-input v-model="form.workInfo.articleLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else-if="form.workType === 'VIDEO'">
              <el-form-item label="视频标题" required>
                <el-input v-model="form.workInfo.videoTitle" placeholder="请输入视频标题" />
              </el-form-item>
              <el-form-item label="类别" required>
                <el-select v-model="form.workInfo.category" placeholder="请选择类别">
                  <el-option label="风景" value="风景" />
                  <el-option label="活动新闻" value="活动新闻" />
                  <el-option label="原创短" value="原创短" />
                  <el-option label="原创长" value="原创长" />
                </el-select>
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="时长(秒)">
                    <el-input-number v-model="form.workInfo.videoDurationSec" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="点赞数">
                    <el-input-number v-model="form.workInfo.likeCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="转载情况">
                <el-input v-model="form.workInfo.reprintStatus" placeholder="无 / 校级平台转载 / 其他" />
              </el-form-item>
              <el-form-item label="视频链接">
                <el-input v-model="form.workInfo.videoLink" placeholder="https://" />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="作品标题" required>
                <el-input v-model="form.workInfo.title" placeholder="请输入视觉作品标题" />
              </el-form-item>
              <el-form-item label="类别">
                <el-input v-model="form.workInfo.category" placeholder="如海报、长图、排版" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="参与人数">
                    <el-input-number v-model="form.workInfo.authorCount" :min="0" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col v-if="canManageFeeAmount" :span="12">
                  <el-form-item label="人均金额">
                    <el-input-number v-model="form.workInfo.authorAvgFee" :min="0" :precision="2" controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="作品链接">
                <el-input v-model="form.workInfo.articleLink" placeholder="https://" />
              </el-form-item>
            </template>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="发布日期" required>
                  <el-date-picker v-model="form.workInfo.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否发布">
                  <el-switch v-model="form.workInfo.isPublished" :active-value="1" :inactive-value="0" />
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
              <span>参与人员</span>
              <el-button type="primary" :icon="Plus" @click="addContributor">添加</el-button>
            </div>
          </template>

          <div class="user-hint">
            可选择已加载人员；若当前账号无人员查询权限，也可以直接填写用户 ID。
          </div>

          <div v-for="(item, index) in form.contributors" :key="item.key" class="contributor-row">
            <div class="contributor-title">
              <strong>人员 {{ index + 1 }}</strong>
              <el-button link type="danger" :icon="Delete" @click="removeContributor(index)">删除</el-button>
            </div>
            <el-form label-width="80px">
              <el-form-item label="成员">
                <el-select
                  v-model="item.userId"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="姓名 / 用户ID"
                >
                  <el-option v-for="user in users" :key="user.id" :label="`${user.realName}（${user.studentNo || user.username}）`" :value="user.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="item.roleInWork" placeholder="选择角色">
                  <el-option v-for="role in contributorRoles" :key="role.value" :label="role.label" :value="role.value" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="canManageFeeAmount" label="金额">
                <el-input-number v-model="item.feeAmount" :min="0" :precision="2" controls-position="right" />
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="item.remark" placeholder="选填" />
              </el-form-item>
            </el-form>
          </div>

          <el-empty v-if="form.contributors.length === 0" description="请添加至少一位参与人员" />

          <div class="submit-panel">
            <div v-if="canManageFeeAmount">
              <span>合计金额</span>
              <strong>¥{{ totalAmount }}</strong>
            </div>
            <span v-else class="amount-hidden">金额由秘书长统一维护</span>
            <el-button type="success" size="large" :loading="submitting" @click="handleSubmit">提交任务</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { submitFee } from '@/api/fee'
import { getUserList } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const allWorkTypeOptions = [
  { label: '采编部', value: 'EDITORIAL' },
  { label: '摄影部', value: 'PHOTO' },
  { label: '视频部', value: 'VIDEO' },
  { label: '视觉部', value: 'VISUAL' }
]
const workTypeDeptMap = { EDITORIAL: 'EDITORIAL', PHOTO: 'PHOTOGRAPHY', VIDEO: 'VIDEO', VISUAL: 'VISUAL' }

const roleCodes = computed(() => userStore.userInfo?.roleCodes || [])
const deptCodes = computed(() => userStore.userInfo?.deptCodes || [])
const highestRoleCode = computed(() => roleCodes.value.length ? Math.min(...roleCodes.value) : 13)
const canViewAllTaskTypes = computed(() => [0, 1, 2, 3].includes(highestRoleCode.value))
const canManageFeeAmount = computed(() => [0, 3].includes(highestRoleCode.value))
const workTypeOptions = computed(() => {
  if (canViewAllTaskTypes.value) return allWorkTypeOptions
  return allWorkTypeOptions.filter(item => deptCodes.value.includes(workTypeDeptMap[item.value]))
})

const contributorRoles = [
  { label: '作者', value: 'AUTHOR' },
  { label: '拍摄', value: 'SHOOTER' },
  { label: '编辑/剪辑', value: 'EDITOR' },
  { label: '设计', value: 'DESIGNER' }
]

const users = ref([])
const submitting = ref(false)
const form = reactive({
  workType: 'EDITORIAL',
  workInfo: {},
  contributors: []
})

const createWorkInfo = workType => {
  const base = {
    publishDate: new Date().toISOString().slice(0, 10),
    isPublished: 1,
    remark: ''
  }
  if (workType === 'EDITORIAL') {
    return { ...base, title: '', platform: '微信', category: '', wordCount: 0, readCount: 0, articleLink: '' }
  }
  if (workType === 'PHOTO') {
    return { ...base, title: '', platform: '微信', photoCount: 0, readCount: 0, articleLink: '' }
  }
  if (workType === 'VIDEO') {
    return { ...base, videoTitle: '', category: '活动新闻', videoDurationSec: 0, likeCount: 0, reprintStatus: '无', videoLink: '' }
  }
  return { ...base, title: '', category: '', authorCount: 0, authorAvgFee: 0, articleLink: '' }
}

const resetWorkInfo = () => {
  form.workInfo = createWorkInfo(form.workType)
  loadUsers()
}

const addContributor = () => {
  form.contributors.push({
    key: Date.now() + Math.random(),
    userId: '',
    roleInWork: form.workType === 'VISUAL' ? 'DESIGNER' : 'AUTHOR',
    feeAmount: 0,
    remark: ''
  })
}

const removeContributor = index => {
  form.contributors.splice(index, 1)
}

const totalAmount = computed(() => form.contributors.reduce((sum, item) => sum + Number(item.feeAmount || 0), 0).toFixed(2))

const validate = () => {
  const title = form.workType === 'VIDEO' ? form.workInfo.videoTitle : form.workInfo.title
  if (!title) return '请填写作品标题'
  if (!form.workInfo.publishDate) return '请选择发布日期'
  if (form.contributors.length === 0) return '请添加参与人员'
  const invalid = form.contributors.some(item => !item.userId || !item.roleInWork)
  if (invalid) return '请补全参与人员和角色'
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
    const workInfo = { ...form.workInfo }
    if (!canManageFeeAmount.value) {
      delete workInfo.authorAvgFee
    }
    await submitFee({
      workType: form.workType,
      workInfo,
      contributors: form.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: item.roleInWork,
        feeAmount: canManageFeeAmount.value ? Number(item.feeAmount || 0) : 0,
        remark: item.remark
      }))
    })
    ElMessage.success('稿费已提交')
    resetWorkInfo()
    form.contributors = []
    addContributor()
  } finally {
    submitting.value = false
  }
}

const loadUsers = async () => {
  try {
    const deptCode = workTypeDeptMap[form.workType]
    const res = await getUserList({ pageNum: 1, pageSize: 200, deptCode })
    users.value = res.data?.list || []
  } catch {
    users.value = []
  }
}

resetWorkInfo()
addContributor()
onMounted(() => {
  if (!workTypeOptions.value.some(item => item.value === form.workType) && workTypeOptions.value.length) {
    form.workType = workTypeOptions.value[0].value
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
  color: #6b7280;
  margin: 0;
}

.card-header,
.contributor-title,
.submit-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.work-form :deep(.el-select),
.work-form :deep(.el-date-editor),
.contributor-row :deep(.el-select),
.contributor-row :deep(.el-input-number) {
  width: 100%;
}

.user-hint {
  margin-bottom: 12px;
  color: #6b7280;
  font-size: 13px;
}

.contributor-row {
  border: 1px solid #ebeef4;
  border-radius: 8px;
  padding: 14px 14px 2px;
  margin-bottom: 12px;
  background: #fff;
}

.contributor-title {
  margin-bottom: 10px;
  color: var(--secondary-color);
}

.submit-panel {
  margin-top: 18px;
  padding: 16px;
  border-radius: 8px;
  background: #f6f9ff;
}

.submit-panel span {
  display: block;
  color: #6b7280;
  font-size: 13px;
}

.submit-panel strong {
  display: block;
  color: var(--secondary-color);
  font-size: 24px;
  line-height: 1.2;
}

.amount-hidden {
  color: #6b7280;
  font-size: 14px;
}

@media (max-width: 768px) {
  .card-header,
  .submit-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
