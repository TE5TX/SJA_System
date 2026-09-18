<template>
  <div class="user-batch-import">
    <div class="page-title">
      <h1>人员批量导入</h1>
      <p>先设定本批人员的团内职务，再上传 XLSX 名单核对有效行和跳过原因。</p>
    </div>

    <el-card class="import-card">
      <el-steps :active="result ? 3 : preview ? 1 : 0" align-center class="import-steps">
        <el-step title="上传与设置" />
        <el-step title="核对数据" />
        <el-step title="导入完成" />
      </el-steps>

      <el-alert type="info" :closable="false" show-icon>
        <template #title>名单中的姓名、学号、身份证号码必填，其余字段可以留空。学号将作为登录账号，初始密码为 asdf123456，首次登录须修改。</template>
      </el-alert>

      <el-form label-position="top" class="import-form">
        <el-form-item label="本批人员的团内职务" required>
          <el-select
            v-model="selectedRoleId"
            filterable
            clearable
            :loading="rolesLoading"
            :disabled="confirmLoading"
            placeholder="请先选择统一职务"
            @change="handleRoleChange"
          >
            <el-option
              v-for="role in assignableRoles"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
          <span class="field-help">本批人员统一使用所选职务，不再从名单行另行指定成员身份。</span>
        </el-form-item>

        <el-form-item label="人员名单模板">
          <el-button :icon="Download" :loading="templateLoading" @click="downloadTemplate">
            下载九列 XLSX 模板
          </el-button>
          <span class="field-help">列名必须完整，顺序可以调整；团内部门仅限采编部、摄影部、视觉部、视频部，可用“、”分隔多个。学号、手机号码、身份证号码和银行卡号请保持文本格式。</span>
        </el-form-item>

        <el-form-item label="选择人员名单" required>
          <el-upload
            ref="uploadRef"
            drag
            action="#"
            accept=".xlsx"
            :disabled="!selectedRoleId || previewLoading || confirmLoading"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-exceed="handleFileExceed"
          >
            <div class="upload-copy">
              <el-icon :size="32"><UploadFilled /></el-icon>
              <strong>{{ selectedRoleId ? '拖拽 XLSX 文件到此处，或点击选择' : '请先选择团内职务' }}</strong>
              <span>一次选择一个文件，大小不超过 2 MB</span>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="selectedFile"
        type="success"
        :closable="false"
        show-icon
        :title="`已选择“${selectedFile.name}”，统一职务：${selectedRoleName}`"
      />
      <div class="preview-actions">
        <el-button type="primary" :disabled="!selectedRoleId || !selectedFile || confirmLoading" :loading="previewLoading" @click="loadPreview">
          解析并预览
        </el-button>
        <span class="field-help">预览只校验名单，不会创建账号或修改数据库。</span>
      </div>
      <el-alert class="phase-tip" type="info" :closable="false" show-icon
        title="预览不会写入数据库；请核对名单后再确认导入有效行。" />
    </el-card>

    <el-card v-if="preview" class="preview-card">
      <div class="preview-heading">
        <h2>名单核对</h2>
        <el-radio-group v-model="rowFilter" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="valid">可导入</el-radio-button>
          <el-radio-button value="invalid">将跳过</el-radio-button>
        </el-radio-group>
      </div>
      <div class="preview-summary">
        <span>团内职务：{{ preview.roleName }}</span>
        <span>总计：{{ preview.total }}</span>
        <span>可导入：{{ preview.validCount }}</span>
        <span>将跳过：{{ preview.invalidCount }}</span>
      </div>
      <el-alert v-if="preview.ignoredHeaders?.length" type="info" :closable="false"
        :title="`已忽略其他列：${preview.ignoredHeaders.join('、')}`" />
      <el-table :data="filteredRows" border class="preview-table" max-height="520">
        <el-table-column prop="rowNumber" label="Excel 行号" width="110" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="studentNo" label="学号" min-width="150" />
        <el-table-column label="团内部门" min-width="140">
          <template #default="{ row }">{{ row.deptNames?.length ? row.deptNames.join('、') : '未填写' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.valid ? 'success' : 'danger'">{{ row.valid ? '可导入' : '将跳过' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="原因" min-width="260">
          <template #default="{ row }">{{ row.errors?.length ? row.errors.join('；') : '—' }}</template>
        </el-table-column>
      </el-table>
      <div class="confirm-actions">
        <span>确认后将创建 {{ preview.validCount }} 个账号，跳过 {{ preview.invalidCount }} 行。</span>
        <el-button type="primary" :disabled="!preview.importToken || confirmLoading" @click="confirmDialogVisible = true">
          确认导入有效行
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="confirmDialogVisible" title="确认人员批量导入" width="520px"
      :close-on-click-modal="!confirmLoading" :close-on-press-escape="!confirmLoading"
      :show-close="!confirmLoading" @closed="currentPassword = ''">
      <p>将创建 <strong>{{ preview?.validCount || 0 }}</strong> 个账号；预览中无效的
        <strong>{{ preview?.invalidCount || 0 }}</strong> 行会跳过。</p>
      <p>统一团内职务：<strong>{{ preview?.roleName }}</strong></p>
      <el-alert type="warning" :closable="false" show-icon
        title="学号为登录账号，初始密码为 asdf123456；新用户首次登录必须修改密码。" />
      <el-form class="password-form" @submit.prevent="confirmImport">
        <el-form-item label="当前账号密码" required>
          <el-input v-model="currentPassword" type="password" show-password autocomplete="current-password"
            :disabled="confirmLoading" placeholder="输入当前账号密码以确认" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="confirmLoading" @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirmLoading" :disabled="!currentPassword || !preview?.importToken"
          @click="confirmImport">确认并导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultDialogVisible" title="人员批量导入结果" width="720px">
      <template v-if="result">
        <div class="result-summary">
          <span>总计 {{ result.totalCount }}</span><span>成功 {{ result.importedCount }}</span>
          <span>跳过 {{ result.skippedCount }}</span><span>未处理 {{ result.unprocessedCount }}</span>
        </div>
        <el-alert v-if="result.aborted" type="error" :closable="false" show-icon
          :title="result.abortReason || '导入中止，请核查后重试未处理人员'" />
        <el-table v-if="result.failures?.length" :data="result.failures" border max-height="360" class="preview-table">
          <el-table-column prop="rowNumber" label="Excel 行号" width="105" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column prop="studentNo" label="学号" min-width="150" />
          <el-table-column prop="reason" label="跳过原因" min-width="230" />
        </el-table>
        <p v-else>没有跳过的行。</p>
      </template>
      <template #footer>
        <el-button v-if="result?.failureReportBase64" @click="downloadFailureReport">下载失败明细 XLSX</el-button>
        <el-button v-if="result?.importedCount > 0" @click="showImportedUsers">查看人员信息</el-button>
        <el-button type="primary" @click="resultDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { downloadUserRosterImportTemplate, previewUserRosterImport, confirmUserRosterImport } from '@/api/user'
import { getRoleList } from '@/api/system'
import { useUserStore } from '@/store/user'

// 202609181028 - 完成了批量导入的权限入口、统一职务选择、模板下载与文件选择界面。
const userStore = useUserStore()
const router = useRouter()
const uploadRef = ref(null)
const roles = ref([])
const rolesLoading = ref(false)
const templateLoading = ref(false)
const selectedRoleId = ref(null)
const selectedFile = ref(null)
const preview = ref(null)
const previewLoading = ref(false)
const confirmLoading = ref(false)
const confirmDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const currentPassword = ref('')
const result = ref(null)
const rowFilter = ref('all')
const filteredRows = computed(() => (preview.value?.rows || []).filter(row =>
  rowFilter.value === 'all' || (rowFilter.value === 'valid' ? row.valid : !row.valid)))

const currentRoleCode = computed(() => {
  const codes = (userStore.userInfo?.roleCodes || []).map(Number).filter(Number.isFinite)
  return codes.length ? Math.min(...codes) : 13
})
const assignableRoles = computed(() => roles.value.filter(role => {
  const code = Number(role.roleCode)
  return Number(role.isValid) === 1 && Number.isFinite(code)
    && code >= currentRoleCode.value && ![0, 1, 2, 3, 13].includes(code)
}))
const selectedRoleName = computed(() => assignableRoles.value.find(role => role.id === selectedRoleId.value)?.roleName || '')

const loadRoles = async () => {
  rolesLoading.value = true
  try {
    const response = await getRoleList()
    roles.value = Array.isArray(response.data) ? response.data : []
  } finally {
    rolesLoading.value = false
  }
}

const handleRoleChange = () => {
  // 变更整批职务后要求重新选择文件，避免后续预览使用旧选择。
  selectedFile.value = null
  preview.value = null
  result.value = null
  uploadRef.value?.clearFiles()
}

const handleFileChange = uploadFile => {
  preview.value = null
  result.value = null
  const file = uploadFile.raw
  if (!file?.name?.toLowerCase().endsWith('.xlsx') || file.size > 2 * 1024 * 1024) {
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    ElMessage.warning('请选择不超过 2 MB 的 XLSX 文件')
    return
  }
  selectedFile.value = file
}

const handleFileRemove = () => { selectedFile.value = null; preview.value = null }
const handleFileExceed = () => { ElMessage.warning('请先移除已选择的文件') }

// 202609181104 - 完成了名单上传预览、有效/跳过筛选及逐行错误展示。
const loadPreview = async () => {
  if (!selectedRoleId.value || !selectedFile.value) return
  previewLoading.value = true
  preview.value = null
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('roleId', String(selectedRoleId.value))
    const response = await previewUserRosterImport(formData)
    preview.value = response.data
    rowFilter.value = 'all'
    ElMessage.success(`解析完成：${response.data.validCount} 人可导入，${response.data.invalidCount} 人将跳过`)
  } finally {
    previewLoading.value = false
  }
}

// 202609181147 - 完成了密码二次确认、一次性导入和失败明细结果弹窗。
const confirmImport = async () => {
  if (confirmLoading.value || !preview.value?.importToken || !currentPassword.value) return
  confirmLoading.value = true
  try {
    const response = await confirmUserRosterImport(preview.value.importToken, currentPassword.value)
    result.value = response.data
    confirmDialogVisible.value = false
    resultDialogVisible.value = true
    preview.value = null
    selectedFile.value = null
    uploadRef.value?.clearFiles()
  } catch (error) {
    if (!String(error.message || '').includes('当前用户密码错误')) {
      // The request may have reached the server even when its response was lost; never retry the same token blindly.
      preview.value = null
      confirmDialogVisible.value = false
      ElMessage.warning('确认结果未能取得。请先核对人员信息，再重新预览名单；不要直接重复导入。')
    }
  } finally {
    currentPassword.value = ''
    confirmLoading.value = false
  }
}

const downloadFailureReport = () => {
  if (!result.value?.failureReportBase64) return
  const binary = atob(result.value.failureReportBase64)
  const bytes = Uint8Array.from(binary, character => character.charCodeAt(0))
  const url = URL.createObjectURL(new Blob([bytes], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  }))
  const link = document.createElement('a')
  link.href = url
  link.download = '人员批量导入失败明细.xlsx'
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

const showImportedUsers = () => {
  resultDialogVisible.value = false
  router.push({ name: 'UserManagement' })
}

const downloadTemplate = async () => {
  templateLoading.value = true
  try {
    const response = await downloadUserRosterImportTemplate()
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '人员批量导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } finally {
    templateLoading.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.page-title p { margin: 0; color: #6b7280; }
.import-card, .preview-card { max-width: 1200px; }
.preview-card { margin-top: 20px; }
.preview-heading, .preview-summary { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.preview-heading h2 { margin: 0 0 16px; font-size: 19px; }
.preview-summary { justify-content: flex-start; margin-bottom: 16px; color: #344054; }
.preview-table { margin-top: 16px; }
.confirm-actions, .result-summary { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-top: 18px; }
.result-summary { justify-content: flex-start; gap: 20px; margin: 0 0 16px; font-weight: 600; }
.password-form { margin-top: 18px; }
.preview-actions { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin: 0 auto; max-width: 740px; }
.preview-actions .field-help { width: auto; margin: 0; }
.import-steps { margin-bottom: 28px; }
.import-form { max-width: 740px; margin: 28px auto; }
.import-form :deep(.el-select), .import-form :deep(.el-upload), .import-form :deep(.el-upload-dragger) { width: 100%; }
.field-help { display: block; width: 100%; margin-top: 8px; color: #667085; font-size: 13px; line-height: 1.6; }
.upload-copy { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 12px; }
.upload-copy span { color: #667085; font-size: 13px; }
.phase-tip { margin-top: 18px; }
@media (max-width: 640px) {
  .import-form { margin: 20px 0; }
  .import-steps { margin-bottom: 20px; }
}
</style>
