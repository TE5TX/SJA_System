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
