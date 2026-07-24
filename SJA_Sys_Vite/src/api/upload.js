import request from '@/utils/request'

export function uploadAvatar(file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/v1/upload/avatar',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
