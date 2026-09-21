import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

let refreshPromise = null

const clearAuth = () => {
  // 202609201535 - 清理历史版本遗留的Web Storage令牌，认证令牌仅允许存在于HttpOnly Cookie。
  sessionStorage.removeItem('token')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('sessionUser')
  localStorage.removeItem('mustChangePassword')
}

const refreshAccessToken = () => {
  if (!refreshPromise) {
    refreshPromise = axios.post('/api/v1/auth/refresh', null, {
      withCredentials: true,
      headers: { 'X-SJA-Refresh': '1' }
    }).then(response => response.data).finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

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
  async error => {
    const message = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      const original = error.config || {}
      const isAuthRequest = original.url?.includes('/v1/auth/login') || original.url?.includes('/v1/auth/refresh')
      if (!isAuthRequest && !original._retry) {
        original._retry = true
        try {
          // 202609201535 - Access Token由浏览器自动携带的HttpOnly Cookie提供，前端不再读取令牌。
          await refreshAccessToken()
          return request(original)
        } catch (refreshError) {
          clearAuth()
          if (!original.skipAuthRedirect) {
            ElMessage.error('登录已过期，请重新登录')
            window.location.href = '/login'
          }
          return Promise.reject(refreshError)
        }
      }
      if (!isAuthRequest) {
        clearAuth()
        if (!original.skipAuthRedirect) {
          ElMessage.error('登录已过期，请重新登录')
          window.location.href = '/login'
        }
      }
    } else if (error.response?.status === 403 && message === '首次登录请先修改密码') {
      localStorage.setItem('mustChangePassword', 'true')
      if (window.location.pathname !== '/change-password') window.location.href = '/change-password'
    // 202608140858 - 允许认证头像后台加载失败时静默使用文字头像。
    } else if (!error.config?.skipGlobalError) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
