import http from 'k6/http'
import { check } from 'k6'
import { SharedArray } from 'k6/data'
import execution from 'k6/execution'
import { sleep } from 'k6'

const source = new SharedArray('sja-load-users', () => [
  JSON.parse(open('./generated/users.generated.json'))
])[0]

const baseUrl = (__ENV.BASE_URL || 'https://sja.org.cn').replace(/\/$/, '')
const runId = `${Date.now()}`

export const options = {
  scenarios: {
    // Each effective SQL user updates their own row exactly once.
    profile_updates: {
      executor: 'per-vu-iterations',
      exec: 'updateProfile',
      vus: source.profiles.length,
      iterations: 1,
      maxDuration: '3m'
    },
    // All users whose SQL role grants fee:submit submit one department-valid task.
    fee_submissions: {
      executor: 'per-vu-iterations',
      exec: 'submitFee',
      vus: source.fees.length,
      iterations: 1,
      maxDuration: '3m'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    checks: ['rate>0.99']
  }
}

function params(token, endpoint, idempotencyKey) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...(idempotencyKey ? { 'X-Idempotency-Key': idempotencyKey } : {})
    },
    tags: { endpoint },
    timeout: '30s'
  }
}

function businessSuccess(response) {
  if (response.status !== 200 && response.status !== 202) return false
  try {
    return JSON.parse(response.body).code === 200
  } catch (_) {
    return false
  }
}

function acceptedTaskId(response) {
  if (response.status !== 202) return null
  try {
    const body = JSON.parse(response.body)
    return body.code === 200 ? body.data?.taskId : null
  } catch (_) {
    return null
  }
}

function waitForTask(taskId, statusToken, endpoint) {
  const deadline = Date.now() + 120000
  let pollCount = 0
  while (Date.now() < deadline) {
    // 202608192148 - 修复了压测脚本只测试入队响应、没有验证数据库最终写入的问题。
    sleep(Math.min(6, 2.5 + pollCount * 0.5) + Math.random() * 0.5)
    const response = http.get(
      `${baseUrl}/api/v1/async-tasks/${taskId}`,
      {
        headers: { 'X-Task-Token': statusToken },
        tags: { endpoint: `${endpoint}-status` },
        timeout: '30s'
      }
    )
    if (!businessSuccess(response)) return false
    const task = JSON.parse(response.body).data
    if (task?.status === 'SUCCESS') return true
    if (task?.status === 'FAILED') return false
    pollCount += 1
  }
  return false
}

export function updateProfile() {
  // iterationInTest is scoped to this scenario; __VU may be globally numbered
  // across both concurrent scenarios and is therefore unsafe as an array index.
  const user = source.profiles[execution.scenario.iterationInTest]
  const body = {
    ...user.profile,
    // Force a real UPDATE on every run without changing the login username.
    dormAddress: `压测宿舍-${runId}-${user.userId}`
  }
  const response = http.put(
    `${baseUrl}/api/v1/user/profile`,
    JSON.stringify(body),
    params(user.token, 'profile-update', `k6-profile-${runId}-${user.userId}`)
  )
  const taskId = acceptedTaskId(response)
  check(response, { 'profile update accepted': () => Boolean(taskId) })
  if (taskId) {
    const statusToken = JSON.parse(response.body).data.statusToken
    check(waitForTask(taskId, statusToken, 'profile-update'), {
      'profile update completed': completed => completed
    })
  }
}

export function submitFee() {
  const item = source.fees[execution.scenario.iterationInTest]
  const workInfo = { ...item.workInfo }
  const titleKey = item.workType === 'VIDEO' ? 'videoTitle' : 'title'
  workInfo[titleKey] = `k6-${item.workType}-${runId}-${item.userId}`

  const body = {
    workType: item.workType,
    workInfo,
    contributors: [{
      userId: item.userId,
      roleInWork: item.roleInWork,
      feeAmount: null,
      workCount: item.workType === 'PHOTO' ? 1 : null,
      remark: 'k6并发压力测试'
    }]
  }
  const response = http.post(
    `${baseUrl}/api/v1/fee/submit`,
    JSON.stringify(body),
    params(item.token, 'fee-submit', `k6-fee-${runId}-${item.userId}`)
  )
  const taskId = acceptedTaskId(response)
  check(response, { 'fee submission accepted': () => Boolean(taskId) })
  if (taskId) {
    const statusToken = JSON.parse(response.body).data.statusToken
    check(waitForTask(taskId, statusToken, 'fee-submit'), {
      'fee submission completed': completed => completed
    })
  }
}
