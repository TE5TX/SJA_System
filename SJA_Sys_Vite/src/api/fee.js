import request from '@/utils/request'
import { createIdempotencyKey } from '@/api/asyncTask'

export function submitFee(data) {
  return request({
    url: '/v1/fee/submit',
    method: 'post',
    data,
    // 202608192148 - 修复了网络重试可能重复生成稿费任务的问题。
    headers: { 'X-Idempotency-Key': createIdempotencyKey('fee') }
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

export function calculateFee(data) {
  return request({
    url: '/v1/fee/calculate',
    method: 'post',
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

export function batchAuditFee(data) {
  return request({
    url: '/v1/fee/audit/batch',
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

export function exportMonthlyFeeSummary(data) {
  return request({
    url: '/v1/fee/monthly-summary/export',
    method: 'post',
    data,
    responseType: 'blob',
    skipGlobalError: true
  })
}

// 202609281642 - 完成了月份稿费导出前的同源清单预览请求。
export function previewMonthlyFeeSummary(data) {
  return request({
    url: '/v1/fee/monthly-summary/preview',
    method: 'post',
    data
  })
}

export function getFeeStandards() {
  return request({
    url: '/v1/fee/standards',
    method: 'get'
  })
}

export function updateFeeStandard(id, data) {
  return request({
    url: `/v1/fee/standards/${id}`,
    method: 'put',
    data
  })
}
