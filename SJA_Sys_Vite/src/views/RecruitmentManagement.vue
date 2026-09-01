<template>
  <div class="recruitment-management">
    <div class="page-title">
      <h1>注册管理</h1>
      <p>控制公开注册表单的发布状态，报名人员将以游客身份加入所选部门。</p>
    </div>

    <el-card class="management-panel">
      <template #header>
        <div class="panel-header">
          <span>注册表单</span>
          <el-tag :type="statusTagType" effect="light">
            {{ statusLabel }}
          </el-tag>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="4" animated />
      <template v-else>
        <el-alert
          :title="statusMessage"
          :type="status?.acceptingRegistration ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />

        <div class="form-summary">
          <div>
            <span class="summary-label">注册身份</span>
            <strong>游客</strong>
          </div>
          <div>
            <span class="summary-label">最近发布时间</span>
            <strong>{{ formatTime(status?.publishedAt) }}</strong>
          </div>
        </div>

        <el-form class="schedule-form" label-position="top" :disabled="saving">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="开放时间">
                <el-date-picker
                  v-model="schedule.publishStartAt"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  placeholder="留空则发布后立即开放"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="结束时间">
                <el-date-picker
                  v-model="schedule.publishEndAt"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  placeholder="留空则不自动结束"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="schedule-tip">结束时间必须晚于开放时间。已发布的表单也可以在此调整开放和结束时间。</div>
        </el-form>

        <div class="public-link-row">
          <div class="public-link-field">
            <label for="recruitment-public-link">公开报名地址</label>
            <el-input id="recruitment-public-link" :model-value="publicUrl" readonly />
          </div>
          <el-button :icon="CopyDocument" @click="copyPublicUrl">复制地址</el-button>
          <el-button :icon="View" @click="openPublicForm">预览</el-button>
        </div>

        <div class="action-bar">
          <el-button :loading="saving" @click="saveSchedule">
            保存时间设置
          </el-button>
          <el-button
            v-if="!status?.published"
            type="primary"
            :icon="Promotion"
            :loading="saving"
            @click="changeStatus(true)"
          >
            发布注册表单
          </el-button>
          <el-button
            v-else
            type="warning"
            :icon="VideoPause"
            :loading="saving"
            @click="changeStatus(false)"
          >
            暂停注册表单
          </el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CopyDocument, Promotion, VideoPause, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRecruitmentStatus, updateRecruitmentStatus } from '@/api/recruitment'

const loading = ref(false)
const saving = ref(false)
const status = ref(null)
const schedule = reactive({ publishStartAt: null, publishEndAt: null })
const publicUrl = computed(() => `${window.location.origin}/recruit`)

const statusLabel = computed(() => {
  if (!status.value?.published) return '已暂停'
  if (status.value?.acceptingRegistration) return '开放中'
  if (status.value?.publishStartAt && new Date() < new Date(status.value.publishStartAt)) return '待开放'
  return '已结束'
})

const statusTagType = computed(() => status.value?.acceptingRegistration ? 'success' : 'info')

const statusMessage = computed(() => {
  if (!status.value?.published) return '公开注册入口当前不可填写。'
  if (status.value?.acceptingRegistration) return '公开注册入口当前可以填写并提交。'
  if (status.value?.publishStartAt && new Date() < new Date(status.value.publishStartAt)) {
    return `公开注册将于 ${formatTime(status.value.publishStartAt)} 开放。`
  }
  return '公开注册时间已结束，请调整结束时间或重新发布。'
})

const syncSchedule = value => {
  schedule.publishStartAt = value?.publishStartAt || null
  schedule.publishEndAt = value?.publishEndAt || null
}

const loadStatus = async () => {
  loading.value = true
  try {
    const response = await getRecruitmentStatus()
    status.value = response.data
    syncSchedule(response.data)
  } finally {
    loading.value = false
  }
}

const saveStatus = async (published, successMessage) => {
  if (schedule.publishStartAt && schedule.publishEndAt
    && new Date(schedule.publishEndAt) <= new Date(schedule.publishStartAt)) {
    ElMessage.warning('结束时间必须晚于开放时间')
    return
  }
  saving.value = true
  try {
    const response = await updateRecruitmentStatus({
      published,
      publishStartAt: schedule.publishStartAt,
      publishEndAt: schedule.publishEndAt
    })
    status.value = response.data
    syncSchedule(response.data)
    ElMessage.success(successMessage)
  } finally {
    saving.value = false
  }
}

const changeStatus = async (published) => {
  const action = published ? '发布' : '暂停'
  await ElMessageBox.confirm(
    published
      ? '发布后，任何获得报名地址的人都可以填写并提交注册信息。'
      : '暂停后，公开报名页将立即停止接收新的注册信息。',
    `确认${action}注册表单`,
    { type: published ? 'info' : 'warning', confirmButtonText: action, cancelButtonText: '取消' }
  )

  await saveStatus(published, `注册表单已${action}`)
}

// 202609011534 - 管理员可独立保存公开注册的开放和结束时间。
const saveSchedule = async () => saveStatus(Boolean(status.value?.published), '注册时间设置已保存')

const copyPublicUrl = async () => {
  try {
    await navigator.clipboard.writeText(publicUrl.value)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = publicUrl.value
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    textarea.remove()
  }
  ElMessage.success('报名地址已复制')
}

const openPublicForm = () => window.open(publicUrl.value, '_blank', 'noopener,noreferrer')

const formatTime = value => value ? value.replace('T', ' ').slice(0, 19) : '尚未发布'

onMounted(loadStatus)
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.management-panel {
  width: 100%;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-weight: 700;
  color: var(--secondary-color);
}

.form-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  margin: 24px 0;
  padding: 18px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.form-summary > div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.summary-label,
.public-link-field label {
  color: #6b7280;
  font-size: 13px;
}

.form-summary strong {
  color: #303133;
  overflow-wrap: anywhere;
}

.schedule-form {
  margin: 24px 0;
}

.schedule-form :deep(.el-date-editor) {
  width: 100%;
}

.schedule-tip {
  margin-top: -6px;
  color: #909399;
  font-size: 13px;
}

.public-link-row {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) auto auto;
  align-items: end;
  gap: 12px;
}

.public-link-field {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

@media (max-width: 767px) {
  .form-summary {
    grid-template-columns: minmax(0, 1fr);
    gap: 14px;
  }

  .public-link-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .public-link-field {
    grid-column: 1 / -1;
  }

  .public-link-row .el-button,
  .schedule-form :deep(.el-date-editor),
  .action-bar .el-button {
    width: 100%;
    margin: 0;
  }

  .action-bar {
    justify-content: stretch;
  }
}
</style>
