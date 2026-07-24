import request from '@/utils/request'

export function resolveSujiaNames(names) {
  return request({
    url: '/v1/tools/sujia/resolve',
    method: 'post',
    data: { names }
  })
}

export function exportSujiaNames(names) {
  return request({
    url: '/v1/tools/sujia/export',
    method: 'post',
    data: { names },
    responseType: 'blob'
  })
}
