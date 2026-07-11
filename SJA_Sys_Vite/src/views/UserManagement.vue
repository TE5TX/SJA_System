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
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
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
        <el-form-item label="银行卡号">
          <el-input v-model="editForm.bankCard" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
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
  college: '',
  className: '',
  dormAddress: '',
  bankCard: '',
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
    college: row.college || '',
    className: row.className || '',
    dormAddress: row.dormAddress || '',
    bankCard: row.bankCard || '',
    remark: row.remark || ''
  })
  editVisible.value = true
}

const saveEdit = async () => {
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

@media (max-width: 768px) {
  .filters :deep(.el-input),
  .filters :deep(.el-select),
  .filters .el-button {
    width: 100%;
  }
}
</style>
