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
    method: 'post'
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
