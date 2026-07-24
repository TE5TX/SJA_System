import request from '@/utils/request'

export function submitFee(data) {
  return request({
    url: '/v1/fee/submit',
    method: 'post',
    data
  })
}

export function getFeeContributors() {
  return request({
    url: '/v1/fee/contributors',
    method: 'get'
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

export function updateFeeDetail(workType, id, data) {
  return request({
    url: `/v1/fee/detail/${workType}/${id}`,
    method: 'put',
    data
  })
}

export function invalidateRejectedFee(workType, id) {
  return request({
    url: `/v1/fee/detail/${workType}/${id}`,
    method: 'delete'
  })
}

export function auditFee(workType, id, data) {
  return request({
    url: `/v1/fee/audit/${workType}/${id}`,
    method: 'put',
    data
  })
}

export function resetFeeAudit(workType, id) {
  return request({
    url: `/v1/fee/audit/${workType}/${id}/reset`,
    method: 'put'
  })
}

export function exportFee(data) {
  return request({
    url: '/v1/fee/export',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
