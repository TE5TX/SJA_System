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

export function updateProfilePhoto(photo) {
  return request({
    url: '/v1/user/profile/photo',
    method: 'put',
    data: { photo }
  })
}

export function deleteProfilePhoto() {
  return request({
    url: '/v1/user/profile/photo',
    method: 'delete'
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

export function createUser(data) {
  return request({
    url: '/v1/users',
    method: 'post',
    data
  })
}

export function exportUsers(data) {
  return request({
    url: '/v1/users/export',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 60000
  })
}

export function batchUpdateUsers(data) {
  return request({
    url: '/v1/users/batch',
    method: 'put',
    data
  })
}

// 更新用户信息（管理员）
export function updateUser(id, data) {
  return request({
    url: `/v1/users/${id}`,
    method: 'put',
    data
  })
}

export function updateUserPhoto(id, photo) {
  return request({
    url: `/v1/users/${id}/photo`,
    method: 'put',
    data: { photo }
  })
}

export function deleteUserPhoto(id) {
  return request({
    url: `/v1/users/${id}/photo`,
    method: 'delete'
  })
}

// 启用/禁用用户（管理员）
export function updateUserStatus(id, status) {
  return request({
    url: `/v1/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}
