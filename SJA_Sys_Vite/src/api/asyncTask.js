import request from '@/utils/request'

export function getAsyncTaskStatus(taskId, statusToken) {
  return request({
    url: `/v1/async-tasks/${taskId}`,
    method: 'get',
    headers: { 'X-Task-Token': statusToken },
    // 状态令牌已限制为单个 taskId；不携带 JWT 可避免每次轮询都查询用户权限。
    skipAuth: true,
    // 任务失败通过 data.status 表达，不应触发全局 HTTP 错误提示。
    skipGlobalError: true
  })
}

const sleep = delay => new Promise(resolve => window.setTimeout(resolve, delay))

/**
 * 等待后台写入真正完成。轮询间隔逐步增加并加入随机抖动，避免并发用户整齐地同时请求。
 */
export async function waitForAsyncTask(taskId, statusToken, options = {}) {
  const timeout = options.timeout ?? 120000
  const startedAt = Date.now()
  let pollCount = 0

  while (Date.now() - startedAt < timeout) {
    // 202609081000 - 修复了稿费提交完成后前端固定等待2.5秒才查询任务状态的问题。
    // 首次立即确认，后续渐进退避并保留少量随机抖动，兼顾提交反馈速度和服务端压力。
    if (pollCount > 0) {
      const baseDelay = Math.min(3000, 500 + (pollCount - 1) * 350)
      const jitter = Math.floor(Math.random() * 200)
      await sleep(baseDelay + jitter)
    }

    const response = await getAsyncTaskStatus(taskId, statusToken)
    const task = response.data || {}
    options.onProgress?.(task)

    if (task.status === 'SUCCESS') return task
    if (task.status === 'FAILED') {
      const error = new Error(task.message || '后台任务处理失败')
      error.isAsyncTaskFailure = true
      error.task = task
      throw error
    }
    pollCount += 1
  }

  const error = new Error('任务仍在后台排队处理，请稍后刷新页面查看结果')
  error.isAsyncTaskFailure = true
  throw error
}

export function createIdempotencyKey(prefix = 'task') {
  const randomPart = globalThis.crypto?.randomUUID?.()
    || `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `${prefix}:${randomPart}`
}
