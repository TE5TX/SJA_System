<template>
  <el-dialog
    v-model="visible"
    title="批量注册人员"
    width="980px"
    destroy-on-close
    :close-on-click-modal="false"
    :close-on-press-escape="!busy"
    @closed="resetDialog"
  >
    <el-steps :active="step" finish-status="success" align-center class="import-steps">
      <el-step title="上传与设置" />
      <el-step title="核对数据" />
      <el-step title="注册完成" />
    </el-steps>

    <section v-if="step === 0" class="import-section">
      <el-alert
        title="Excel第一行必须是“姓名、学号”，所有账号初始密码均为 asdf123456，并要求首次登录修改。"
        type="warning"
        :closable="false"
        show-icon
      />

      <div class="template-action">
        <el-button type="primary" plain :loading="templateLoading" @click="downloadTemplate">
          下载标准xlsx模板
        </el-button>
      </div>

      <el-form label-width="100px" class="import-form">
        <el-form-item label="所属部门" required>
          <el-select v-model="deptIds" multiple filterable placeholder="请选择导入人员的部门">
            <el-option v-for="dept in depts" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册身份" required>
          <el-select v-model="roleIds" multiple filterable placeholder="请选择导入人员的身份">
            <el-option
              v-for="role in assignableRoles"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
          <div class="form-help">不能选择权限高于当前用户的身份，后端会再次校验。</div>
        </el-form-item>
        <el-form-item label="人员文件" required>
          <el-upload
            ref="uploadRef"
            drag
            action="#"
            accept=".xlsx"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-exceed="handleFileExceed"
          >
            <div class="upload-copy">
              <strong>拖拽xlsx文件到此处，或点击选择</strong>
              <span>仅读取第一个工作表，最多500人，文件不超过2MB</span>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
    </section>

    <section v-else-if="step === 1" class="import-section">
      <div class="preview-summary">
        <el-tag>总计 {{ preview.total }} 人</el-tag>
        <el-tag type="success">通过 {{ preview.validCount }} 人</el-tag>
        <el-tag v-if="preview.invalidCount" type="danger">失败 {{ preview.invalidCount }} 人</el-tag>
        <span v-if="preview.expiresIn" class="token-tip">确认令牌约 {{ Math.ceil(preview.expiresIn / 60) }} 分钟内有效</span>
      </div>

      <el-alert
        v-if="preview.invalidCount"
        title="存在校验失败的数据，本次不会注册任何人员。请返回修改Excel后重新预览。"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        v-else
        title="请核对姓名、学号、部门和身份。点击确认后将以单一事务完成注册。"
        type="success"
        :closable="false"
        show-icon
      />

      <el-table :data="preview.rows" border height="430" class="preview-table">
        <el-table-column prop="rowNumber" label="Excel行" width="82" align="center" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="155" />
        <el-table-column label="部门" min-width="145">
          <template #default="{ row }">{{ join(row.deptNames) }}</template>
        </el-table-column>
        <el-table-column label="身份" min-width="145">
          <template #default="{ row }">{{ join(row.roleNames) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.valid ? 'success' : 'danger'">{{ row.valid ? '通过' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="校验说明" min-width="210">
          <template #default="{ row }">{{ row.errors?.length ? row.errors.join('；') : '-' }}</template>
        </el-table-column>
      </el-table>
    </section>

    <section v-else class="import-complete">
      <el-result icon="success" title="批量注册完成" :sub-title="`已成功注册 ${result.importedCount} 名人员`">
        <template #extra>
          <el-button type="primary" @click="closeAfterSuccess">返回人员管理</el-button>
        </template>
      </el-result>
    </section>

    <template #footer>
      <template v-if="step === 0">
        <el-button :disabled="busy" @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="previewLoading" @click="createPreview">解析并预览</el-button>
      </template>
      <template v-else-if="step === 1">
        <el-button :disabled="busy" @click="backToUpload">返回修改</el-button>
        <el-button
          type="primary"
          :disabled="preview.invalidCount > 0 || !preview.importToken"
          :loading="confirmLoading"
          @click="confirmImport"
        >
          确认注册 {{ preview.validCount }} 人
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmUserImport, downloadUserImportTemplate, previewUserImport } from '@/api/user'
import { useUserStore } from '@/store/user'

// 202608210907 - 实现了批量注册的XLSX上传、校验列表、二次确认和完成反馈三步交互。
const visible = defineModel({ type: Boolean, default: false })
const props = defineProps({
  roles: { type: Array, default: () => [] },
  depts: { type: Array, default: () => [] }
})
const emit = defineEmits(['success'])
const userStore = useUserStore()

const uploadRef = ref(null)
const selectedFile = ref(null)
const deptIds = ref([])
const roleIds = ref([])
const step = ref(0)
const previewLoading = ref(false)
const confirmLoading = ref(false)
const templateLoading = ref(false)
const preview = reactive({
  importToken: '',
  expiresIn: null,
  total: 0,
  validCount: 0,
  invalidCount: 0,
  rows: []
})
const result = reactive({ importedCount: 0, userIds: [] })

const currentRoleCode = computed(() => {
  const codes = (userStore.userInfo?.roleCodes || []).map(Number).filter(Number.isFinite)
  return codes.length ? Math.min(...codes) : 13
})
const assignableRoles = computed(() => currentRoleCode.value === 0
  ? props.roles
  : props.roles.filter(role => Number(role.roleCode) >= currentRoleCode.value))
const busy = computed(() => previewLoading.value || confirmLoading.value || templateLoading.value)
const join = values => Array.isArray(values) && values.length ? values.join('、') : '-'

const handleFileChange = uploadFile => {
  selectedFile.value = uploadFile.raw || null
}

const handleFileRemove = () => {
  selectedFile.value = null
}

const handleFileExceed = () => {
  ElMessage.warning('一次只能选择一个xlsx文件，请先移除当前文件')
}

const validateInput = () => {
  if (!deptIds.value.length) return '请选择所属部门'
  if (!roleIds.value.length) return '请选择注册身份'
  if (!selectedFile.value) return '请选择xlsx人员文件'
  if (!selectedFile.value.name?.toLowerCase().endsWith('.xlsx')) return '只允许上传xlsx格式文件'
  if (selectedFile.value.size > 2 * 1024 * 1024) return 'xlsx文件不能超过2MB'
  return ''
}

const createPreview = async () => {
  const validationMessage = validateInput()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  deptIds.value.forEach(id => formData.append('deptIds', String(id)))
  roleIds.value.forEach(id => formData.append('roleIds', String(id)))

  previewLoading.value = true
  try {
    const response = await previewUserImport(formData)
    Object.assign(preview, response.data || {})
    step.value = 1
  } finally {
    previewLoading.value = false
  }
}

const downloadTemplate = async () => {
  templateLoading.value = true
  try {
    const response = await downloadUserImportTemplate()
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '人员批量注册模板.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } finally {
    templateLoading.value = false
  }
}

const confirmImport = async () => {
  if (!preview.importToken || preview.invalidCount > 0) return
  let currentPassword = ''
  try {
    const promptResult = await ElMessageBox.prompt(
      `请输入当前用户密码，确认注册核对通过的 ${preview.validCount} 名人员。`,
      '批量注册身份校验',
      {
        type: 'warning',
        confirmButtonText: '确认注册',
        cancelButtonText: '继续核对',
        inputType: 'password',
        inputPlaceholder: '当前用户密码',
        inputValidator: value => String(value || '').trim() ? true : '请输入当前用户密码',
        closeOnClickModal: false,
        showClose: false
      }
    )
    currentPassword = promptResult.value
  } catch {
    return
  }

  confirmLoading.value = true
  try {
    const response = await confirmUserImport(preview.importToken, currentPassword)
    Object.assign(result, response.data || {})
    step.value = 2
    emit('success')
    ElMessage.success(`已成功注册 ${result.importedCount} 名人员`)
  } finally {
    confirmLoading.value = false
  }
}

const backToUpload = () => {
  step.value = 0
  Object.assign(preview, { importToken: '', expiresIn: null, total: 0, validCount: 0, invalidCount: 0, rows: [] })
}

const closeAfterSuccess = () => {
  visible.value = false
}

const resetDialog = () => {
  step.value = 0
  selectedFile.value = null
  deptIds.value = []
  roleIds.value = []
  uploadRef.value?.clearFiles()
  Object.assign(preview, { importToken: '', expiresIn: null, total: 0, validCount: 0, invalidCount: 0, rows: [] })
  Object.assign(result, { importedCount: 0, userIds: [] })
}
</script>

<style scoped>
.import-steps {
  margin: 4px 0 28px;
}

.import-section {
  min-height: 470px;
}

.import-form {
  max-width: 760px;
  margin: 24px auto 0;
}

.template-action {
  display: flex;
  justify-content: flex-end;
  max-width: 760px;
  margin: 16px auto -8px;
}

.import-form :deep(.el-select),
.import-form :deep(.el-upload),
.import-form :deep(.el-upload-dragger) {
  width: 100%;
}

.form-help {
  width: 100%;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.upload-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #606266;
}

.upload-copy span {
  color: #909399;
  font-size: 12px;
}

.preview-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.token-tip {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
}

.preview-table {
  margin-top: 14px;
}

.import-complete {
  min-height: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .preview-summary {
    flex-wrap: wrap;
  }

  .token-tip {
    width: 100%;
    margin-left: 0;
  }
}
</style>
