import request from '@/utils/request'

export function getRoleList() {
  return request({
    url: '/v1/system/roles',
    method: 'get'
  })
}

export function getDeptList() {
  return request({
    url: '/v1/system/depts',
    method: 'get'
  })
}

export function getMenuTree() {
  return request({
    url: '/v1/system/menus',
    method: 'get'
  })
}

export function getOperationLogs(params) {
  return request({
    url: '/v1/system/logs',
    method: 'get',
    params
  })
}

// 202609291708 - 后台目录设置只提交相对路径，真实根目录由运行环境控制。
export function getStorageSettings() {
  return request({ url: '/v1/system/storage', method: 'get' })
}

export function checkStorageSetting(category, relativeDir) {
  return request({ url: '/v1/system/storage/check', method: 'post', data: { category, relativeDir } })
}

export function updateStorageSetting(category, relativeDir, currentPassword) {
  return request({ url: '/v1/system/storage', method: 'put', data: { category, relativeDir, currentPassword } })
}
