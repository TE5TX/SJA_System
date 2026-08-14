<template>
  <div class="user-management">
    <div class="page-title">
      <h1>人员信息</h1>
      <p>按姓名、学号、学院、部门和角色筛选成员，维护基础资料与账号状态。</p>
    </div>

    <el-card>
      <template #header>
        <div class="filters">
          <el-input v-model="query.realName" clearable placeholder="姓名" />
          <el-input v-model="query.studentNo" clearable placeholder="学号" />
          <el-input v-model="query.college" clearable placeholder="学院" />
          <el-select v-model="query.deptCode" clearable placeholder="部门">
            <el-option v-for="dept in depts" :key="dept.deptCode" :label="dept.deptName" :value="dept.deptCode" />
          </el-select>
          <el-select v-model="query.roleCode" clearable placeholder="角色">
            <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
          <el-select v-model="query.isValid" clearable placeholder="账号状态">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增人员</el-button>
          <el-button :icon="Edit" :disabled="selectedUsers.length === 0" @click="openBatchEdit">
            批量修改{{ selectedUsers.length ? `（${selectedUsers.length}）` : '' }}
          </el-button>
          <el-button type="success" :icon="Download" :loading="exportLoading" @click="openExport">导出Excel</el-button>
        </div>
      </template>

      <el-table
        ref="userTableRef"
        class="centered-data-table"
        :data="users"
        v-loading="loading"
        border
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" fixed reserve-selection />
        <!-- 202608131617 - 为人员信息管理列表增加了跨分页连续序号。 -->
        <el-table-column label="序号" width="76" align="center" fixed>
          <template #default="{ $index }">{{ rowNumber($index) }}</template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" width="110" fixed />
        <el-table-column prop="studentNo" label="学号" min-width="130" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="college" label="学院" min-width="170" show-overflow-tooltip />
        <el-table-column prop="className" label="班级" min-width="130" show-overflow-tooltip />
        <el-table-column label="部门" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ join(row.deptNames) }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ join(row.roleNames) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isValid === 1 ? 'success' : 'danger'">{{ row.isValid === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)" class="edit-btn">编辑</el-button>
            <el-button
              link
              :type="row.isValid === 1 ? 'danger' : 'success'"
              :icon="SwitchButton"
              @click="toggleStatus(row)"
            >
              {{ row.isValid === 1 ? '禁用' : '启用' }}
            </el-button>
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" :title="isCreating ? '新增人员' : '编辑人员信息'" width="720px" :close-on-click-modal="false" @closed="clearAvatarPreview">
      <el-form ref="userFormRef" :model="editForm" :rules="userFormRules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="editForm.realName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="editForm.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
                <el-radio :value="0">未知</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="editForm.studentNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="editForm.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="isCreating" :gutter="16">
          <el-col :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="editForm.password" type="password" show-password autocomplete="new-password" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="editForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学院" prop="college">
              <el-select v-model="editForm.college" filterable placeholder="请选择学院">
                <el-option v-for="college in collegeOptions" :key="college" :label="college" :value="college" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="editForm.className" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="宿舍地址" prop="dormAddress">
          <el-input v-model="editForm.dormAddress" />
        </el-form-item>
        <el-form-item label="照片">
          <el-upload
            class="avatar-upload"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
          >
            <AuthenticatedAvatar v-if="editForm.photo" :src="avatarPreviewUrl || editForm.photo" :size="52" />
            <el-button v-else type="primary">上传证件照</el-button>
            <el-button v-if="editForm.photo" class="edit-btn" type="primary" link>重新上传</el-button>
          </el-upload>
          <el-button v-if="editForm.photo" type="danger" link @click="handleDeleteAvatar">删除照片</el-button>
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="editForm.idCard" />
        </el-form-item>
        <el-form-item label="银行卡号" prop="bankCard">
          <el-input v-model="editForm.bankCard" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptIds">
              <el-select v-model="editForm.deptIds" multiple filterable placeholder="请选择部门">
                <el-option v-for="dept in depts" :key="dept.id" :label="dept.deptName" :value="dept.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="editForm.roleIds" multiple filterable placeholder="请选择角色">
                <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="账号状态" prop="isValid">
          <el-switch v-model="editForm.isValid" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="!isCreating" type="warning" :loading="resetPasswordSaving" @click="handleResetPassword">
          重置密码
        </el-button>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">{{ isCreating ? '确认新增' : '保存修改' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量修改人员" width="520px">
      <div class="batch-summary">已选择 {{ selectedUsers.length }} 名人员</div>
      <div class="batch-option">
        <el-checkbox v-model="batchForm.updateRoles">修改身份角色</el-checkbox>
        <el-select
          v-model="batchForm.roleIds"
          multiple
          filterable
          :disabled="!batchForm.updateRoles"
          placeholder="请选择修改后的角色"
        >
          <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </div>
      <div class="batch-option">
        <el-checkbox v-model="batchForm.updateStatus">修改账号状态</el-checkbox>
        <el-radio-group v-model="batchForm.isValid" :disabled="!batchForm.updateStatus">
          <el-radio-button :value="1">启用</el-radio-button>
          <el-radio-button :value="0">禁用</el-radio-button>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="saveBatchEdit">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="exportVisible" title="导出人员信息" width="560px">
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
        <el-button type="primary" :loading="exportLoading" @click="handleExport">导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Download, Edit, Plus, Refresh, Search, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchUpdateUsers, createUser, deleteUserPhoto, exportUsers, getUserDetail, getUserList, resetUserPassword, updateUser, updateUserPhoto, updateUserStatus } from '@/api/user'
import { getDeptList, getRoleList } from '@/api/system'
import { uploadAvatar } from '@/api/upload'
import AuthenticatedAvatar from '@/components/AuthenticatedAvatar.vue'

const query = reactive({ realName: '', studentNo: '', college: '', deptCode: '', roleCode: '', isValid: '', pageNum: 1, pageSize: 10 })
const users = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const resetPasswordSaving = ref(false)
const exportLoading = ref(false)
const batchSaving = ref(false)
const editVisible = ref(false)
const exportVisible = ref(false)
const batchVisible = ref(false)
const editingId = ref(null)
const dialogMode = ref('edit')
const userFormRef = ref(null)
const userTableRef = ref(null)
const selectedUsers = ref([])
const roles = ref([])
const depts = ref([])
const collegeOptions = [
  '电子信息学院',
  '机电工程学院',
  '计算机学院',
  '经贸学院',
  '人文社会科学学院',
  '材料与食品学院',
  '外国语学院',
  '艺术设计学院',
  '管理学院'
]
const editForm = reactive({
  realName: '',
  gender: 0,
  studentNo: '',
  phone: '',
  photo: '',
  idCard: '',
  college: '',
  className: '',
  dormAddress: '',
  bankCard: '',
  deptIds: [],
  roleIds: [],
  isValid: 1,
  remark: '',
  password: '',
  confirmPassword: ''
})
const avatarPreviewUrl = ref('')
// 202608140858 - 新增人员的未绑定头像使用本地 Blob 预览，避免绕过后端绑定校验。
const clearAvatarPreview = () => {
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value)
  avatarPreviewUrl.value = ''
}
const isCreating = computed(() => dialogMode.value === 'create')
const requiredText = label => [{ required: true, whitespace: true, message: `请输入${label}`, trigger: 'blur' }]
// 202608131153 - 放宽了有人员编辑权限的管理人员修改人员信息时的格式校验。
// 202608131454 - 新增人员时仅要求填写学号，其余资料允许后续补充。
const userFormRules = computed(() => ({
  realName: [],
  gender: [],
  studentNo: requiredText('学号'),
  phone: [],
  password: [
    { validator: (_rule, value, callback) => !value || (value.length >= 6 && value.length <= 50) ? callback() : callback(new Error('密码长度应为 6 到 50 个字符')), trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: (_rule, value, callback) => value === editForm.password ? callback() : callback(new Error('两次输入的密码不一致')), trigger: 'blur' }
  ],
  idCard: [],
  college: [],
  className: [],
  dormAddress: [],
  bankCard: [],
  deptIds: [],
  roleIds: [],
  isValid: [],
  remark: []
}))
const batchForm = reactive({
  updateRoles: true,
  roleIds: [],
  updateStatus: false,
  isValid: 1
})

const exportFieldOptions = [
  { key: 'realName', label: '姓名' },
  { key: 'gender', label: '性别' },
  { key: 'studentNo', label: '学号' },
  { key: 'phone', label: '手机号' },
  { key: 'idCard', label: '身份证号' },
  { key: 'bankCard', label: '银行卡号' },
  { key: 'college', label: '学院' },
  { key: 'className', label: '班级' },
  { key: 'dormAddress', label: '宿舍地址' },
  { key: 'deptNames', label: '部门' },
  { key: 'roleNames', label: '角色' },
  { key: 'isValid', label: '状态' },
  { key: 'photo', label: '照片地址' },
  { key: 'remark', label: '备注' }
]
// 202608101042 - 修复了默认导出手机号、身份证、银行卡和宿舍地址导致敏感信息暴露的问题。
const defaultExportFields = ['realName', 'studentNo', 'college', 'className', 'deptNames', 'roleNames', 'isValid']
const exportFields = ref([...defaultExportFields])
const allExportFieldsSelected = computed(() => exportFields.value.length === exportFieldOptions.length)

const join = value => Array.isArray(value) && value.length ? value.join('、') : '-'
let loadSequence = 0
const loadUsers = async () => {
  const currentSequence = ++loadSequence
  loading.value = true
  try {
    const res = await getUserList({ ...query })
    if (currentSequence !== loadSequence) return
    users.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    if (currentSequence === loadSequence) loading.value = false
  }
}

const search = () => {
  clearTimeout(realtimeQueryTimer)
  clearSelection()
  query.pageNum = 1
  loadUsers()
}

// 202608131443 - 增加了人员管理筛选条件输入或选择后的实时查询。
let realtimeQueryTimer
watch(
  () => [query.realName, query.studentNo, query.college, query.deptCode, query.roleCode, query.isValid],
  () => {
    clearTimeout(realtimeQueryTimer)
    realtimeQueryTimer = setTimeout(search, 300)
  }
)

onBeforeUnmount(() => {
  clearTimeout(realtimeQueryTimer)
  clearAvatarPreview()
})

const reset = () => {
  clearSelection()
  // 202608111533 - 修复了人员管理分页不能选择100或200条并使用9999条加载全部数据的问题。
  Object.assign(query, { realName: '', studentNo: '', college: '', deptCode: '', roleCode: '', isValid: '', pageNum: 1, pageSize: 10 })
  nextTick(search)
}

const handleSelectionChange = selection => {
  selectedUsers.value = selection
}

const clearSelection = () => {
  userTableRef.value?.clearSelection()
  selectedUsers.value = []
}

const openBatchEdit = () => {
  if (!selectedUsers.value.length) return
  Object.assign(batchForm, {
    updateRoles: true,
    roleIds: [],
    updateStatus: false,
    isValid: 1
  })
  batchVisible.value = true
}

const rowNumber = index => (query.pageNum - 1) * query.pageSize + index + 1

const saveBatchEdit = async () => {
  if (!batchForm.updateRoles && !batchForm.updateStatus) {
    ElMessage.warning('请至少选择一项需要修改的内容')
    return
  }
  if (batchForm.updateRoles && !batchForm.roleIds.length) {
    ElMessage.warning('请选择修改后的角色')
    return
  }

  let currentPassword = ''
  try {
    const result = await ElMessageBox.prompt(
      `请输入当前用户密码，确认批量修改选中的 ${selectedUsers.value.length} 名人员`,
      '批量修改身份校验',
      {
        type: 'warning',
        confirmButtonText: '确认修改',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '当前用户密码',
        inputValidator: value => String(value || '').trim() ? true : '请输入当前用户密码',
        closeOnClickModal: false,
        showClose: false
      }
    )
    currentPassword = result.value
  } catch {
    return
  }

  batchSaving.value = true
  try {
    const data = { userIds: selectedUsers.value.map(user => user.id), currentPassword }
    if (batchForm.updateRoles) data.roleIds = [...batchForm.roleIds]
    if (batchForm.updateStatus) data.isValid = batchForm.isValid
    await batchUpdateUsers(data)
    ElMessage.success(`已修改 ${selectedUsers.value.length} 名人员`)
    batchVisible.value = false
    clearSelection()
    await loadUsers()
  } finally {
    batchSaving.value = false
  }
}

const openExport = () => {
  exportVisible.value = true
}

const toggleAllExportFields = () => {
  exportFields.value = allExportFieldsSelected.value
    ? []
    : exportFieldOptions.map(field => field.key)
}

const restoreDefaultExportFields = () => {
  exportFields.value = [...defaultExportFields]
}

const handleExport = async () => {
  if (!exportFields.value.length) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }

  exportLoading.value = true
  try {
    const exportParams = {
      realName: query.realName,
      studentNo: query.studentNo,
      college: query.college,
      deptCode: query.deptCode,
      roleCode: query.roleCode,
      fields: exportFields.value
    }
    if (query.isValid !== '') {
      exportParams.isValid = query.isValid
    }

    const response = await exportUsers(exportParams)
    if (response.status === 204 || !response.data?.size) {
      ElMessage.warning('没有可导出的人员信息')
      return
    }

    downloadExportFile(response)
    exportVisible.value = false
    ElMessage.success('人员信息已导出')
  } finally {
    exportLoading.value = false
  }
}

const downloadExportFile = response => {
  const disposition = response.headers['content-disposition'] || ''
  const filenameMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const filename = filenameMatch
    ? decodeURIComponent(filenameMatch[1])
    : `人员信息_${formatDateForFilename(new Date())}.xlsx`
  const blob = new Blob([response.data], {
    type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const formatDateForFilename = date => {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

const resetUserForm = () => {
  clearAvatarPreview()
  Object.assign(editForm, {
    realName: '',
    gender: 0,
    studentNo: '',
    phone: '',
    photo: '',
    idCard: '',
    college: '',
    className: '',
    dormAddress: '',
    bankCard: '',
    deptIds: [],
    roleIds: [],
    isValid: 1,
    remark: '',
    password: '',
    confirmPassword: ''
  })
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  resetUserForm()
  editVisible.value = true
  nextTick(() => userFormRef.value?.clearValidate())
}

const openEdit = async row => {
  clearAvatarPreview()
  const detail = (await getUserDetail(row.id)).data
  dialogMode.value = 'edit'
  editingId.value = detail.id
  Object.assign(editForm, {
    realName: detail.realName || '',
    gender: detail.gender ?? 0,
    studentNo: detail.studentNo || '',
    phone: detail.phone || '',
    photo: detail.photo || '',
    idCard: detail.idCard || '',
    college: detail.college || '',
    className: detail.className || '',
    dormAddress: detail.dormAddress || '',
    bankCard: detail.bankCard || '',
    deptIds: Array.isArray(detail.deptIds) && detail.deptIds.length ? detail.deptIds : idsByNames(depts.value, detail.deptNames, 'deptName'),
    roleIds: Array.isArray(detail.roleIds) && detail.roleIds.length ? detail.roleIds : idsByNames(roles.value, detail.roleNames, 'roleName'),
    isValid: detail.isValid ?? 1,
    remark: detail.remark || '',
    password: '',
    confirmPassword: ''
  })
  editVisible.value = true
  nextTick(() => userFormRef.value?.clearValidate())
}

const saveEdit = async () => {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const { confirmPassword, ...formData } = editForm
    if (isCreating.value) {
      await createUser(formData)
      ElMessage.success('人员新增成功，登录账号为学号')
    } else {
      const { password, ...updateData } = formData
      await updateUser(editingId.value, updateData)
      ElMessage.success('人员信息已保存')
    }
    editVisible.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

// 202608131456 - 增加了管理人员重置他人密码为 asdf123456 的功能。
const handleResetPassword = async () => {
  if (!editingId.value) return
  try {
    await ElMessageBox.confirm(
      '确认将该人员密码重置为 asdf123456？重置后该人员需要重新登录并修改密码。',
      '重置密码确认',
      { type: 'warning', confirmButtonText: '确认重置' }
    )
  } catch {
    return
  }

  resetPasswordSaving.value = true
  try {
    await resetUserPassword(editingId.value)
    ElMessage.success('密码已重置为 asdf123456')
  } finally {
    resetPasswordSaving.value = false
  }
}

const beforeAvatarUpload = file => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('只支持 JPEG、PNG、GIF 格式')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('照片大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async options => {
  try {
    clearAvatarPreview()
    avatarPreviewUrl.value = URL.createObjectURL(options.file)
    const res = await uploadAvatar(options.file)
    const url = res.data?.url || ''
    editForm.photo = url
    if (isCreating.value) {
      ElMessage.success('照片已上传，提交新增人员后保存')
    } else {
      await updateUserPhoto(editingId.value, url)
      ElMessage.success('照片已上传并保存')
      await loadUsers()
    }
    options.onSuccess?.(res)
  } catch (error) {
    clearAvatarPreview()
    options.onError?.(error)
  }
}

const handleDeleteAvatar = async () => {
  clearAvatarPreview()
  if (isCreating.value) {
    editForm.photo = ''
    return
  }
  if (!editingId.value) return
  await deleteUserPhoto(editingId.value)
  editForm.photo = ''
  ElMessage.success('照片已删除')
  await loadUsers()
}

const idsByNames = (options, names, nameField) => {
  if (!Array.isArray(names)) return []
  return options
    .filter(item => names.includes(item[nameField]))
    .map(item => item.id)
}

const toggleStatus = row => {
  const nextStatus = row.isValid === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确认${action} ${row.realName} 的账号？`, action, { type: 'warning' }).then(async () => {
    await updateUserStatus(row.id, nextStatus)
    ElMessage.success(`${action}成功`)
    await loadUsers()
  }).catch(() => {})
}

const loadOptions = async () => {
  try {
    roles.value = (await getRoleList()).data || []
  } catch {
    roles.value = []
  }
  try {
    depts.value = (await getDeptList()).data || []
  } catch {
    depts.value = []
  }
}

onMounted(() => {
  loadOptions()
  loadUsers()
})
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filters :deep(.el-input),
.filters :deep(.el-select) {
  width: 150px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

/* 202608131646 - 统一了人员信息管理主表各列的居中对齐样式。 */
.centered-data-table :deep(.el-table__cell .cell) {
  text-align: center;
}

:deep(.el-dialog .el-select) {
  width: 100%;
}

.avatar-upload :deep(.el-upload) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-summary {
  margin-bottom: 18px;
  color: #606266;
}

.batch-option {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  min-height: 48px;
  margin-bottom: 12px;
}

.batch-option :deep(.el-checkbox) {
  margin-right: 0;
}

.export-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-bottom: 12px;
}

.export-actions :deep(.export-action-btn) {
  min-width: auto;
  height: 32px;
  margin: 0;
  padding: 0 10px;
  color: #606266;
  background-color: transparent !important;
  border-color: transparent !important;
}

.export-actions :deep(.export-action-btn:hover) {
  color: #3f51f5;
  background-color: #f0f2ff !important;
}

.export-field-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
}

.export-field-list :deep(.el-checkbox) {
  margin-right: 0;
}

@media (max-width: 768px) {
  .filters :deep(.el-input),
  .filters :deep(.el-select),
  .filters .el-button {
    width: 100%;
  }

  .export-field-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .batch-option {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

:deep(.edit-btn) {
  background-color: transparent !important;
  border-color: transparent !important;
  color: rgb(88, 88, 205) !important;
  margin-left: 0;
}
</style>
