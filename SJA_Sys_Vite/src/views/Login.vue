<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>校记者团管理系统</h2>
          <p>输入您的学号（账号）和密码</p>
        </div>
      </template>

      <el-form :model="loginForm" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="学号（账号）"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="credits">Designed by 校记者团技术部</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入学号（账号）', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login(loginForm)
        const { token, userInfo, menus } = res.data

        userStore.setToken(token)
        userStore.setUserInfo(userInfo)

        if (menus) {
          localStorage.setItem('menus', JSON.stringify(menus))
        }

        ElMessage.success('登录成功')

        setTimeout(() => {
          router.push('/app')
        }, 100)
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--background-color);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 450px;
  border: none !important;
  border-radius: 8px !important;
  box-shadow: 0px 0 30px rgba(1, 41, 112, 0.1) !important;
}

.login-card :deep(.el-card__header) {
  padding: 20px !important;
  border-bottom: 1px solid #ebeef4 !important;
}

.login-card :deep(.el-card__body) {
  padding: 30px !important;
}

.login-header {
  text-align: center;
  margin-bottom: 10px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--secondary-color);
  margin: 0;
  font-family: 'Nunito', sans-serif;
}

.login-header p {
  font-size: 14px;
  color: #899bbd;
  margin: 5px 0 0;
}

.login-form {
  margin-top: 20px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 4px;
  padding: 8px 12px;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 10px 0 rgba(65, 84, 241, 0.15);
}

.login-button {
  width: 100%;
  margin-top: 10px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 4px;
}

.credits {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #899bbd;
}
</style>
