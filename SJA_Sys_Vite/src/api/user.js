import request from '@/utils/request'

// 获取当前用户个人资料
export function getUserProfile() {
  return request({
    url: '/v1/user/profile',
    method: 'get'
  })
}

// 更新个人资料
export function updateUserProfile(data) {
  return request({
    url: '/v1/user/profile',
    method: 'put',
    data
  })
}

// 查询用户列表（管理员）
export function getUserList(params) {
  return request({
    url: '/v1/users',
    method: 'get',
    params
  })
}
