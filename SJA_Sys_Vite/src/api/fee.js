import request from '@/utils/request'

export function submitFee(data) {
  return request({
    url: '/v1/fee/submit',
    method: 'post',
    data
  })
}

export function getFeeList(params) {
  return request({
    url: '/v1/fee/list',
    method: 'get',
    params
  })
}

export function getFeeDetail(workType, id) {
  return request({
    url: `/v1/fee/detail/${workType}/${id}`,
    method: 'get'
  })
}

export function auditFee(workType, id, data) {
  return request({
    url: `/v1/fee/audit/${workType}/${id}`,
    method: 'put',
    data
  })
}

export function exportFee(params) {
  return request({
    url: '/v1/fee/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
