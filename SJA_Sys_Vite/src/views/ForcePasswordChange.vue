<!-- 202608101042 - 修复了首次登录缺少强制修改初始密码页面的问题。 -->
<template>
  <div class="password-page">
    <el-card class="password-panel">
      <template #header>
        <div>
          <h1>设置新密码</h1>
          <p>首次登录需要修改初始密码，新密码由英文字母和数字组成。</p>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit-button" @click="submit">确认修改</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule, value, callback) => {
  if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度必须为8到64位', trigger: 'blur' },
    // 202608121431 - 放宽了首次登录密码格式，不再要求同时包含大小写字母。
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/, message: '必须同时包含英文字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    userStore.setMustChangePassword(false)
    ElMessage.success('密码修改成功')
    await router.replace('/app')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--background-color);
}

.password-panel {
  width: min(440px, 100%);
  border-radius: 8px;
}

h1 {
  margin: 0;
  font-size: 22px;
  letter-spacing: 0;
}

p {
  margin: 6px 0 0;
  color: #6b7280;
}

.submit-button {
  width: 100%;
}
</style>
