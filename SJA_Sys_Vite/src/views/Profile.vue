<template>
  <div class="profile">
    <div class="page-title">
      <h1>个人管理</h1>
    </div>

    <el-row :gutter="20">
      <!-- 左侧个人信息卡片 -->
      <el-col :xl="4" :lg="5" :md="6" :sm="24" :xs="24">
        <el-card class="profile-card">
          <div class="profile-card-body">
            <el-avatar :size="120" class="profile-avatar">
              {{ userInfo.realName?.charAt(0) || 'U' }}
            </el-avatar>
            <h2 class="profile-name">{{ userInfo.realName || '未设置' }}</h2>
            <div class="profile-badges">
              <el-tag type="primary" size="large">{{ deptDisplay }}</el-tag>
              <el-tag type="info" size="large">{{ roleDisplay }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧详细信息卡片 -->
      <el-col :xl="20" :lg="19" :md="18" :sm="24" :xs="24">
        <el-card>
          <template #header>
            <el-tabs v-model="activeTab" class="profile-tabs">
              <el-tab-pane label="个人资料" name="overview"></el-tab-pane>
              <el-tab-pane label="编辑信息" name="edit"></el-tab-pane>
              <el-tab-pane label="修改密码" name="password"></el-tab-pane>
            </el-tabs>
          </template>

          <!-- 个人资料（只读） -->
          <div v-show="activeTab === 'overview'" class="tab-content">
            <el-descriptions :column="2" border class="profile-descriptions">
              <el-descriptions-item label="姓名">{{ userInfo.realName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="性别">{{ genderText }}</el-descriptions-item>
              <el-descriptions-item label="学号（账号）">{{ userInfo.studentNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
              <el-descriptions-item label="银行卡号">{{ userInfo.bankCard || '-' }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">{{ idCardMasked }}</el-descriptions-item>
              <el-descriptions-item label="照片">{{ userInfo.photo ? '已上传' : '未上传' }}</el-descriptions-item>
              <el-descriptions-item label="二级学院">{{ userInfo.college || '-' }}</el-descriptions-item>
              <el-descriptions-item label="班级">{{ userInfo.className || '-' }}</el-descriptions-item>
              <el-descriptions-item label="宿舍地址">{{ userInfo.dormAddress || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属部门">{{ deptDisplay }}</el-descriptions-item>
              <el-descriptions-item label="团内职务">{{ roleDisplay }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ formatTime(userInfo.createTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 编辑信息 -->
          <div v-show="activeTab === 'edit'" class="tab-content">
            <el-alert
              title="提示：修改学号后，登录账号将自动同步为新的学号"
              type="info"
              :closable="true"
              show-icon
              style="margin-bottom: 20px"
            />

            <el-form
              :model="editForm"
              :rules="rules"
              ref="formRef"
              label-width="120px"
              class="edit-form"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="姓名" prop="realName">
                    <el-input v-model="editForm.realName" placeholder="请输入姓名" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="性别">
                    <el-radio-group v-model="editForm.gender">
                      <el-radio :value="1">男</el-radio>
                      <el-radio :value="2">女</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="学号（账号）" prop="studentNo">
                    <el-input v-model="editForm.studentNo" placeholder="修改后账号将同步更新" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="身份证号" prop="idCard">
                    <el-input v-model="editForm.idCard" placeholder="请输入身份证号" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="银行卡号" prop="bankCard">
                    <el-input v-model="editForm.bankCard" placeholder="用于稿费发放" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="手机号" prop="phone">
                    <el-input v-model="editForm.phone" placeholder="请输入手机号" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="二级学院" prop="college">
                    <el-select v-model="editForm.college" filterable placeholder="请选择二级学院">
                      <el-option v-for="college in collegeOptions" :key="college" :label="college" :value="college" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="班级" prop="className">
                    <el-input v-model="editForm.className" placeholder="请输入班级" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="宿舍地址" prop="dormAddress">
                    <el-input v-model="editForm.dormAddress" placeholder="例如：3栋404" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="照片">
                    <el-upload
                      class="avatar-upload"
                      :show-file-list="false"
                      :before-upload="beforeAvatarUpload"
                      :http-request="handleAvatarUpload"
                    >
                      <el-avatar v-if="editForm.photo" :src="editForm.photo" :size="56" />
                      <el-button v-else type="primary">上传照片</el-button>
                      <el-button v-if="editForm.photo" class="edit-btn" type="primary" link>重新上传</el-button>
                    </el-upload>
                    <el-button v-if="editForm.photo" type="danger" link @click="handleDeleteAvatar">删除照片</el-button>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="所属部门" prop="deptNames">
                    <el-input :model-value="deptDisplay" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="团内职务" prop="roleNames">
                    <el-input :model-value="roleDisplay" disabled />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item>
                <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
                  <el-icon><Check /></el-icon>
                  保存修改
                </el-button>
                <el-button @click="handleReset">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 修改密码 -->
          <div v-show="activeTab === 'password'" class="tab-content">
            <el-form
              :model="passwordForm"
              :rules="passwordRules"
              ref="passwordFormRef"
              label-width="120px"
              class="password-form"
            >
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入当前密码"
                  show-password
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入8到64位英文字母和数字组合"
                  show-password
                />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-button>
                <el-button @click="handleResetPassword">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh, Lock } from '@element-plus/icons-vue'
import { deleteProfilePhoto, getUserProfile, updateProfilePhoto, updateUserProfile } from '@/api/user'
import { changePassword } from '@/api/auth'
import { uploadAvatar } from '@/api/upload'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const activeTab = ref('overview')
const submitLoading = ref(false)
const passwordLoading = ref(false)
const formRef = ref(null)
const passwordFormRef = ref(null)
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

// 用户信息（优先用 store 中的，后续可从接口刷新）
const userInfo = computed(() => userStore.userInfo || {})

// 部门显示
const deptDisplay = computed(() => {
  const names = userInfo.value?.deptNames
  return (names && names.length > 0) ? names.join('、') : '未分配部门'
})

// 角色显示
const roleDisplay = computed(() => {
  const names = userInfo.value?.roleNames
  return (names && names.length > 0) ? names.join('、') : '-'
})

// 性别文本
const genderText = computed(() => {
  const g = userInfo.value?.gender
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '-'
})

const idCardMasked = computed(() => userInfo.value?.idCard || '-')

// 编辑表单
const editForm = reactive({
  realName: '',
  gender: 1,
  photo: '',
  studentNo: '',
  idCard: '',
  bankCard: '',
  phone: '',
  college: '',
  className: '',
  dormAddress: ''
})

// 编辑表单验证
const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
  ],
  bankCard: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  college: [{ required: true, message: '请输入二级学院', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  dormAddress: [{ required: true, message: '请输入宿舍地址', trigger: 'blur' }]
}

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 确认密码校验
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度必须为8到64位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/, message: '必须同时包含英文字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 加载用户信息到编辑表单
const loadEditForm = () => {
  const data = userInfo.value
  Object.assign(editForm, {
    realName: data.realName || '',
    gender: data.gender ?? 1,
    photo: data.photo || '',
    studentNo: data.studentNo || '',
    idCard: data.idCard || '',
    bankCard: data.bankCard || '',
    phone: data.phone || '',
    college: data.college || '',
    className: data.className || '',
    dormAddress: data.dormAddress || ''
  })
}

// 从后端刷新个人资料
const refreshProfile = async () => {
  try {
    const res = await getUserProfile()
    userStore.setUserInfo(res.data)
    loadEditForm()
  } catch (e) {
    console.error('刷新个人资料失败:', e)
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
    const url = res.data?.url || ''
    await updateProfilePhoto(url)
    editForm.photo = url
    userStore.setUserInfo({ ...userInfo.value, photo: url })
    ElMessage.success('照片已上传并保存')
    options.onSuccess?.(res)
  } catch (error) {
    options.onError?.(error)
  }
}

const handleDeleteAvatar = async () => {
  try {
    await deleteProfilePhoto()
    editForm.photo = ''
    userStore.setUserInfo({ ...userInfo.value, photo: '' })
    ElMessage.success('照片已删除')
  } catch (error) {
    console.error(error)
  }
}

// 提交编辑
const handleSubmit = async () => {
  if (!formRef.value) return
  if (!Array.isArray(userInfo.value?.deptNames) || userInfo.value.deptNames.length === 0) {
    ElMessage.warning('所属部门不能为空，请先由管理员分配部门')
    return
  }
  if (!Array.isArray(userInfo.value?.roleNames) || userInfo.value.roleNames.length === 0) {
    ElMessage.warning('团内职务不能为空，请先由管理员分配角色')
    return
  }

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await updateUserProfile({ ...editForm })
        ElMessage.success('保存成功')

        // 刷新个人资料（学号修改后账号会同步更新）
        await refreshProfile()

        // 如果学号变了，提醒用户账号已变更
        if (editForm.studentNo !== userInfo.value?.studentNo) {
          ElMessageBox.alert(
            `您的登录账号已更新为：${editForm.studentNo}，下次登录请使用新账号。`,
            '账号变更提醒',
            { confirmButtonText: '我知道了', type: 'info' }
          )
        }
      } catch (error) {
        console.error(error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 重置表单
const handleReset = () => {
  loadEditForm()
  ElMessage.info('已重置')
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordLoading.value = true
      try {
        await changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功，请重新登录')

        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''

        setTimeout(() => {
          ElMessageBox.confirm('密码已修改，需要重新登录', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            userStore.logout()
            window.location.href = '/login'
          }).catch(() => {})
        }, 1000)
      } catch (error) {
        console.error(error)
      } finally {
        passwordLoading.value = false
      }
    }
  })
}

// 重置密码表单
const handleResetPassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  if (passwordFormRef.value) {
    passwordFormRef.value.clearValidate()
  }
  ElMessage.info('已重置')
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  try {
    const d = new Date(time)
    return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return time
  }
}

// 初始化
onMounted(() => {
  loadEditForm()
  // 从后端刷新最新个人资料
  refreshProfile()
})
</script>

<style scoped>
.profile {
  padding: 0;
}

.page-title {
  margin-bottom: 20px;
}

.page-title h1 {
  font-size: 24px;
  font-weight: 600;
  color: var(--secondary-color);
  margin-bottom: 10px;
}

/* 左侧个人信息卡片 */
.profile-card {
  border: none !important;
  border-radius: 8px !important;
  box-shadow: var(--card-shadow) !important;
}

.profile-card-body {
  text-align: center;
  padding: 20px 0;
}

.profile-avatar {
  margin-bottom: 15px;
  background: linear-gradient(45deg, var(--primary-color), var(--primary-hover));
  font-size: 36px;
  font-weight: 600;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--secondary-color);
  margin: 10px 0;
}

.profile-badges {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  margin-top: 15px;
}

.profile-badges .el-tag {
  width: 100%;
  justify-content: center;
  font-size: 14px;
  padding: 6px 12px;
}

/* 选项卡 */
.profile-tabs :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: 2px solid #ebeef4;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: #6c757d;
  padding: 0 20px;
  height: 50px;
  line-height: 50px;
}

.profile-tabs :deep(.el-tabs__item:hover) {
  color: var(--primary-color);
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
  font-weight: 600;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary-color);
  height: 3px;
}

/* 内容区域 */
.tab-content {
  padding: 20px 0;
}

.profile-descriptions :deep(.el-descriptions__label) {
  font-weight: 500;
  color: var(--secondary-color);
  width: 120px;
}

.profile-descriptions :deep(.el-descriptions__content) {
  color: #606266;
}

/* 编辑表单 */
.edit-form,
.password-form {
  max-width: 800px;
}

.edit-form :deep(.el-form-item__label),
.password-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

.edit-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #f8f9fa;
}

.avatar-upload :deep(.el-upload) {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-button {
  min-width: 100px;
}

:deep(.edit-btn) {
  background-color: transparent !important;
  border-color: transparent !important;
  color: rgb(88, 88, 205) !important;
  margin-left: 0;
}

@media (max-width: 768px) {
  .profile-card-body {
    padding: 15px 0;
  }

  .profile-name {
    font-size: 18px;
  }

  .profile-tabs :deep(.el-tabs__item) {
    padding: 0 10px;
    font-size: 14px;
  }

  .edit-form,
  .password-form {
    max-width: 100%;
  }
}
</style>
