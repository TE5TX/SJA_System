import request from '@/utils/request'

// 登录
export function login(data) {
  return request({
    url: '/v1/auth/login',
    method: 'post',
    data
  })
}

// 登出
export function logout() {
  return request({
    url: '/v1/auth/logout',
    method: 'post'
  })
}

// 刷新令牌
export function refreshToken() {
  return request({
    url: '/v1/auth/refresh',
    method: 'post',
    headers: { 'X-SJA-Refresh': '1' }
  })
}

// 202609201535 - 获取双重提交Cookie模式使用的CSRF令牌。
export function prepareCsrfToken() {
  return request({
    url: '/v1/auth/csrf',
    method: 'get',
    skipAuthRedirect: true,
    skipGlobalError: true
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/v1/user/profile/password',
    method: 'put',
    data
  })
}
