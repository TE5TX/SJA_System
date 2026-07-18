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
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" border row-key="id">
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑人员信息" width="720px">
      <el-form :model="editForm" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="editForm.realName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
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
            <el-form-item label="学号">
              <el-input v-model="editForm.studentNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学院">
              <el-input v-model="editForm.college" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级">
              <el-input v-model="editForm.className" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="宿舍地址">
          <el-input v-model="editForm.dormAddress" />
        </el-form-item>
        <el-form-item label="照片">
          <el-upload
            class="avatar-upload"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
          >
            <el-avatar v-if="editForm.photo" :src="editForm.photo" :size="52" />
            <el-button v-else type="primary">上传照片</el-button>
            <el-button v-if="editForm.photo" class="change-avatar" type="primary" link>重新上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="editForm.idCard" />
        </el-form-item>
        <el-form-item label="银行卡号">
          <el-input v-model="editForm.bankCard" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" required>
              <el-select v-model="editForm.deptIds" multiple filterable placeholder="请选择部门">
                <el-option v-for="dept in depts" :key="dept.id" :label="dept.deptName" :value="dept.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" required>
              <el-select v-model="editForm.roleIds" multiple filterable placeholder="请选择角色">
                <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-switch v-model="editForm.isValid" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Edit, Refresh, Search, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUser, updateUserStatus } from '@/api/user'
import { getDeptList, getRoleList } from '@/api/system'
import { uploadAvatar } from '@/api/upload'

const query = reactive({ realName: '', studentNo: '', college: '', deptCode: '', roleCode: '', pageNum: 1, pageSize: 10 })
const users = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const editingId = ref(null)
const roles = ref([])
const depts = ref([])
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
  remark: ''
})

const join = value => Array.isArray(value) && value.length ? value.join('、') : '-'

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await getUserList({ ...query })
    users.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNum = 1
  loadUsers()
}

const reset = () => {
  Object.assign(query, { realName: '', studentNo: '', college: '', deptCode: '', roleCode: '', pageNum: 1, pageSize: 10 })
  loadUsers()
}

const openEdit = row => {
  editingId.value = row.id
  Object.assign(editForm, {
    realName: row.realName || '',
    gender: row.gender ?? 0,
    studentNo: row.studentNo || '',
    phone: row.phone || '',
    photo: row.photo || '',
    idCard: row.idCard || '',
    college: row.college || '',
    className: row.className || '',
    dormAddress: row.dormAddress || '',
    bankCard: row.bankCard || '',
    deptIds: Array.isArray(row.deptIds) && row.deptIds.length ? row.deptIds : idsByNames(depts.value, row.deptNames, 'deptName'),
    roleIds: Array.isArray(row.roleIds) && row.roleIds.length ? row.roleIds : idsByNames(roles.value, row.roleNames, 'roleName'),
    isValid: row.isValid ?? 1,
    remark: row.remark || ''
  })
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.deptIds.length) {
    ElMessage.warning('请至少选择一个部门')
    return
  }
  if (!editForm.roleIds.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }

  saving.value = true
  try {
    await updateUser(editingId.value, { ...editForm })
    ElMessage.success('人员信息已保存')
    editVisible.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

const beforeAvatarUpload = file => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('只支持 jpg、png、gif、webp 格式')
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
    const res = await uploadAvatar(options.file)
    editForm.photo = res.data?.url || ''
    ElMessage.success('照片上传成功')
    options.onSuccess?.(res)
  } catch (error) {
    options.onError?.(error)
  }
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

:deep(.el-dialog .el-select) {
  width: 100%;
}

.avatar-upload :deep(.el-upload) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.change-avatar {
  margin-left: 0;
}

@media (max-width: 768px) {
  .filters :deep(.el-input),
  .filters :deep(.el-select),
  .filters .el-button {
    width: 100%;
  }
}

:deep(.edit-btn) {
  background-color: transparent !important;
  border-color: transparent !important;
  color: rgb(88, 88, 205) !important;
}
</style>
