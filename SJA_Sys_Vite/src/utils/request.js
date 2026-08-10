import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      const isLoginRequest = error.config?.url?.includes('/v1/auth/login')
      if (!isLoginRequest) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
    } else if (error.response?.status === 403 && message === '首次登录请先修改密码') {
      localStorage.setItem('mustChangePassword', 'true')
      if (window.location.pathname !== '/change-password') window.location.href = '/change-password'
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
