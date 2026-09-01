import request from '@/utils/request'

export function getRecruitmentStatus() {
  return request({
    url: '/v1/recruitment/status',
    method: 'get',
    skipAuth: true
  })
}

export function updateRecruitmentStatus(data) {
  return request({
    url: '/v1/recruitment/manage/status',
    method: 'put',
    data
  })
}

export function submitRecruitmentRegistration(data, photo) {
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
  // 202609011534 - 照片改为非必填时，只在用户选择文件后提交该分段。
  if (photo) {
    formData.append('photo', photo)
  }
  return request({
    url: '/v1/recruitment/register',
    method: 'post',
    data: formData,
    skipAuth: true,
    timeout: 30000
  })
}
