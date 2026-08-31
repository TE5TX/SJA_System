import request from '@/utils/request'

export function getRecruitmentStatus() {
  return request({
    url: '/v1/recruitment/status',
    method: 'get',
    skipAuth: true
  })
}

export function updateRecruitmentStatus(published) {
  return request({
    url: '/v1/recruitment/manage/status',
    method: 'put',
    data: { published }
  })
}

export function submitRecruitmentRegistration(data, photo) {
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
  formData.append('photo', photo)
  return request({
    url: '/v1/recruitment/register',
    method: 'post',
    data: formData,
    skipAuth: true,
    timeout: 30000
  })
}
