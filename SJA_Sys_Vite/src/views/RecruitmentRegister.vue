<template>
  <div class="recruitment-page">
    <header class="public-header">
      <div class="brand">校记者团管理系统</div>
      <router-link to="/login">返回登录</router-link>
    </header>

    <main class="public-content">
      <div class="public-title">
        <h1>校记者团成员注册</h1>
        <p>请如实填写个人信息并设置登录密码，提交成功后即可使用学号和密码登录系统。</p>
      </div>

      <el-card v-if="loading" class="registration-panel">
        <el-skeleton :rows="8" animated />
      </el-card>

      <el-card v-else-if="!status?.acceptingRegistration" class="registration-panel paused-panel">
        <el-result icon="warning" :title="registrationStatusTitle" :sub-title="registrationStatusMessage" />
      </el-card>

      <el-card v-else-if="completed" class="registration-panel completed-panel">
        <el-result icon="success" title="注册成功">
          <template #sub-title>
            <p>登录账号为你填写的学号，登录密码为你注册时设置的密码。</p>
          </template>
          <template #extra>
            <el-button type="primary" @click="$router.push('/login')">前往登录</el-button>
          </template>
        </el-result>
      </el-card>

      <el-card v-else class="registration-panel">
        <template #header>
          <div class="form-header">
            <span>个人信息</span>
          </div>
        </template>

        <el-alert
          title="请妥善保管你设置的登录密码。银行卡号和照片可后续补充。"
          type="info"
          :closable="false"
          show-icon
        />

        <el-form
          ref="formRef"
          class="registration-form"
          :model="form"
          :rules="rules"
          label-position="top"
          :disabled="submitting"
        >
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="form.realName" maxlength="50" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="学号（登录账号）" prop="studentNo">
                <el-input v-model="form.studentNo" maxlength="20" placeholder="请输入学号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="登录密码" prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  maxlength="50"
                  autocomplete="new-password"
                  show-password
                  placeholder="8至50位，须同时包含字母和数字"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="确认登录密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  maxlength="50"
                  autocomplete="new-password"
                  show-password
                  placeholder="请再次输入登录密码"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="性别" prop="gender">
                <el-select v-model="form.gender" placeholder="请选择性别">
                  <el-option label="男" :value="1" />
                  <el-option label="女" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" maxlength="11" inputmode="numeric" placeholder="请输入11位手机号" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="身份证号" prop="idCard">
                <el-input v-model="form.idCard" maxlength="18" placeholder="请输入18位身份证号" @blur="normalizeIdCard" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="二级学院" prop="college">
                <el-select v-model="form.college" filterable placeholder="请选择二级学院">
                  <el-option v-for="college in collegeOptions" :key="college" :label="college" :value="college" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="班级" prop="className">
                <el-input v-model="form.className" maxlength="50" placeholder="请输入班级" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="宿舍地址" prop="dormAddress">
                <el-input v-model="form.dormAddress" maxlength="100" placeholder="例如：凤翔3栋301" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="银行卡号（可选）" prop="bankCard">
                <el-input v-model="form.bankCard" maxlength="30" inputmode="numeric" placeholder="可后续在个人管理中补充" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="所属部门" prop="deptCode">
                <el-select v-model="form.deptCode" placeholder="请选择所属部门">
                  <el-option v-for="dept in departmentOptions" :key="dept.value" :label="dept.label" :value="dept.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="照片（可选）">
                <el-upload
                  ref="uploadRef"
                  class="photo-upload"
                  accept="image/jpeg,image/png,image/gif"
                  list-type="picture"
                  :auto-upload="false"
                  :limit="1"
                  :on-change="handlePhotoChange"
                  :on-remove="handlePhotoRemove"
                  :on-exceed="handlePhotoExceed"
                >
                  <el-button :icon="Upload">选择照片</el-button>
                  <template #tip>
                    <div class="el-upload__tip">支持 JPG、PNG、GIF，文件不超过 5MB；可后续在个人管理中补充。</div>
                  </template>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>

          <div class="submit-bar">
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              提交注册信息
            </el-button>
          </div>
        </el-form>
      </el-card>
    </main>

    <div v-show="captchaVisible" class="captcha-overlay" @click.self="closeCaptcha">
      <div id="recruitment-captcha"></div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getRecruitmentStatus, submitRecruitmentRegistration } from '@/api/recruitment'
import { collegeOptions } from '@/constants/colleges'

const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const completed = ref(false)
const status = ref(null)
const photoFile = ref(null)
const captchaVisible = ref(false)
let captchaInstance = null

const departmentOptions = [
  { label: '采编部', value: 'EDITORIAL' },
  { label: '摄影部', value: 'PHOTOGRAPHY' },
  { label: '视觉部', value: 'VISUAL' },
  { label: '视频部', value: 'VIDEO' }
]

const form = reactive({
  realName: '',
  studentNo: '',
  password: '',
  confirmPassword: '',
  gender: null,
  phone: '',
  idCard: '',
  college: '',
  className: '',
  dormAddress: '',
  bankCard: '',
  deptCode: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  // 202609011534 - 公开注册改为由注册人自行设置并确认登录密码。
  password: [
    { required: true, message: '请设置登录密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,50}$/, message: '密码须为8至50位且同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入登录密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === form.password) callback()
        else callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入正确的18位身份证号', trigger: 'blur' }
  ],
  college: [{ required: true, message: '请选择二级学院', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  dormAddress: [{ required: true, message: '请输入宿舍地址', trigger: 'blur' }],
  bankCard: [{ pattern: /^$|^\d{10,30}$/, message: '银行卡号应为10到30位数字', trigger: 'blur' }],
  deptCode: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
}

const loadStatus = async () => {
  loading.value = true
  try {
    const response = await getRecruitmentStatus()
    status.value = response.data
  } finally {
    loading.value = false
  }
}

const registrationStatusTitle = computed(() => {
  if (!status.value?.published) return '注册已暂停'
  if (status.value?.publishStartAt && new Date() < new Date(status.value.publishStartAt)) return '注册尚未开始'
  return '注册已结束'
})

const registrationStatusMessage = computed(() => {
  if (!status.value?.published) return '当前暂不接收新的注册信息，请等待管理员重新发布。'
  if (status.value?.publishStartAt && new Date() < new Date(status.value.publishStartAt)) {
    return `注册将于 ${formatTime(status.value.publishStartAt)} 开放。`
  }
  return status.value?.publishEndAt
    ? `本次注册已于 ${formatTime(status.value.publishEndAt)} 结束。`
    : '当前暂不接收新的注册信息。'
})

const normalizeIdCard = () => {
  form.idCard = form.idCard.trim().toUpperCase()
}

const handlePhotoChange = uploadFile => {
  const file = uploadFile.raw
  if (!file?.type?.startsWith('image/')) {
    ElMessage.warning('请选择真实的图片文件')
    uploadRef.value?.clearFiles()
    photoFile.value = null
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('照片大小不能超过 5MB')
    uploadRef.value?.clearFiles()
    photoFile.value = null
    return
  }
  photoFile.value = file
}

const handlePhotoRemove = () => {
  photoFile.value = null
}

const handlePhotoExceed = () => ElMessage.warning('只能选择一张照片，请先移除当前照片')

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const latest = await getRecruitmentStatus()
  status.value = latest.data
  if (!status.value?.acceptingRegistration) {
    ElMessage.warning(registrationStatusTitle.value)
    return
  }
  await openCaptcha()
}

const closeCaptcha = () => {
  captchaInstance?.destroyWindow()
  captchaInstance = null
  captchaVisible.value = false
}

const openCaptcha = async () => {
  if (!window.TAC) {
    ElMessage.error('验证码组件加载失败，请刷新页面后重试')
    return
  }

  closeCaptcha()
  captchaVisible.value = true
  await nextTick()
  // 202609010036 - 公开招新提交前复用登录滑块验证码，降低自动化批量注册风险。
  captchaInstance = new window.TAC({
    requestCaptchaDataUrl: '/api/v1/auth/captcha/generate',
    validCaptchaUrl: '/api/v1/auth/captcha/verify',
    bindEl: '#recruitment-captcha',
    validSuccess: (response, captcha, tac) => {
      const captchaToken = response?.data?.token
      tac.destroyWindow()
      captchaInstance = null
      captchaVisible.value = false
      if (!captchaToken) {
        ElMessage.error('验证码结果无效，请重新验证')
        return
      }
      submitForm(captchaToken)
    },
    validFail: (response, captcha, tac) => tac.reloadCaptcha(),
    btnCloseFun: (element, tac) => {
      tac.destroyWindow()
      captchaInstance = null
      captchaVisible.value = false
    }
  }, {
    logoUrl: null,
    moveTrackMaskBgColor: '#dbeafe',
    moveTrackMaskBorderColor: '#409eff'
  })
  captchaInstance.init()
}

const submitForm = async captchaToken => {
  submitting.value = true
  try {
    await submitRecruitmentRegistration({ ...form, captchaToken }, photoFile.value)
    completed.value = true
    ElMessage.success('注册成功')
  } catch (error) {
    if (error.response?.status === 409) {
      const latest = await getRecruitmentStatus()
      status.value = latest.data
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadStatus)
onBeforeUnmount(closeCaptcha)
</script>

<style scoped>
.recruitment-page {
  min-height: 100dvh;
  background: var(--background-color);
}

.public-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 60px;
  padding: 0 max(24px, calc((100vw - 1040px) / 2));
  background: #fff;
  box-shadow: var(--header-shadow);
}

.brand {
  color: var(--secondary-color);
  font-size: 20px;
  font-weight: 700;
}

.public-content {
  width: min(960px, calc(100% - 32px));
  margin: 0 auto;
  padding: 32px 0 48px;
}

.public-title {
  margin-bottom: 22px;
}

.public-title h1 {
  margin-bottom: 8px;
  font-size: 28px;
}

.public-title p {
  color: #6b7280;
}

.registration-panel {
  width: 100%;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--secondary-color);
  font-weight: 700;
}

.registration-form {
  margin-top: 24px;
}

.registration-form :deep(.el-select),
.photo-upload {
  width: 100%;
}

.photo-upload :deep(.el-upload-list) {
  max-width: 520px;
}

.submit-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
}

.submit-bar .el-button {
  min-width: 160px;
}

.paused-panel,
.completed-panel {
  min-height: 360px;
}

/* 202609011625 - 修复了注册暂停与注册完成内容没有在状态卡片正中央显示的问题。 */
.paused-panel :deep(.el-card__body),
.completed-panel :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  box-sizing: border-box;
}

.paused-panel :deep(.el-result),
.completed-panel :deep(.el-result) {
  width: 100%;
}

.completed-panel p + p {
  margin-top: 6px;
}

.captcha-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.42);
}

#recruitment-captcha {
  width: min(318px, calc(100vw - 32px));
  min-height: 318px;
  background: #fff;
  border-radius: 5px;
}

@media (max-width: 767px) {
  .public-header {
    padding: 0 14px;
  }

  .brand {
    font-size: 17px;
  }

  .public-content {
    width: calc(100% - 24px);
    padding: 20px 0 calc(28px + env(safe-area-inset-bottom));
  }

  .public-title h1 {
    font-size: 22px;
  }

  .submit-bar .el-button {
    width: 100%;
  }

  .captcha-overlay {
    padding: 16px;
  }
}
</style>
