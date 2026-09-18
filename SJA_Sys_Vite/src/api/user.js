import request from '@/utils/request'
import { createIdempotencyKey } from '@/api/asyncTask'

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
    data,
    // 202608192148 - 修复了网络重试可能重复创建写入任务的问题。
    headers: { 'X-Idempotency-Key': createIdempotencyKey('profile') }
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

export function getUserDetail(id) {
  return request({
    url: `/v1/users/${id}`,
    method: 'get'
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

// 202609011700 - 完成了人员管理密码确认后的永久删除请求。
export function deleteUser(id, currentPassword) {
  return request({
    url: `/v1/users/${id}`,
    method: 'delete',
    data: { currentPassword }
  })
}

// 202608210907 - 增加了XLSX批量注册预览和一次性令牌确认请求。
export function previewUserImport(formData) {
  return request({
    url: '/v1/users/import/preview',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}

export function downloadUserImportTemplate() {
  return request({
    url: '/v1/users/import/template',
    method: 'get',
    responseType: 'blob',
    timeout: 30000
  })
}

// 202609181028 - 增加十列人员名单模板下载，保留现有两列批量注册模板接口。
export function downloadUserRosterImportTemplate() {
  return request({
    url: '/v1/users/import/roster/template',
    method: 'get',
    responseType: 'blob',
    timeout: 30000
  })
}

// 202609181104 - 新增十列名单的独立预览请求，不复用两列批量注册接口。
export function previewUserRosterImport(formData) {
  return request({
    url: '/v1/users/import/roster/preview',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}

// 202609181147 - 增加人员名单一次性确认导入请求，保留两列注册的原有接口。
export function confirmUserRosterImport(importToken, currentPassword) {
  return request({
    url: '/v1/users/import/roster/confirm',
    method: 'post',
    data: { importToken, currentPassword },
    timeout: 120000
  })
}

export function confirmUserImport(importToken, currentPassword) {
  return request({
    url: '/v1/users/import/confirm',
    method: 'post',
    data: { importToken, currentPassword },
    timeout: 120000
  })
}

export function resetUserPassword(id) {
  return request({
    url: `/v1/users/${id}/password/reset`,
    method: 'put'
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
