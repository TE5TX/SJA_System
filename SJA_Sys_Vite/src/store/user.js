import { defineStore } from 'pinia'
import { ref } from 'vue'
import { prepareCsrfToken } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const authenticated = ref(false)
  const sessionInitialized = ref(false)
  const mustChangePassword = ref(localStorage.getItem('mustChangePassword') === 'true')

  // 安全地解析 userInfo
  let userInfoValue = null
  try {
    const stored = localStorage.getItem('sessionUser')
    if (stored && stored !== 'null' && stored !== 'undefined') {
      userInfoValue = JSON.parse(stored)
    }
  } catch (e) {
    console.error('Failed to parse userInfo:', e)
    localStorage.removeItem('sessionUser')
    userInfoValue = null
  }
  const userInfo = ref(userInfoValue)

  // Remove legacy values that may contain ID card, bank card, phone or dorm data.
  localStorage.removeItem('userInfo')
  localStorage.removeItem('token')

  const toSessionUser = info => {
    if (!info) return null
    const keys = [
      'id', 'username', 'realName', 'photo', 'isValid',
      'roleNames', 'roleCodes', 'deptNames', 'deptCodes'
    ]
    return Object.fromEntries(keys.filter(key => key in info).map(key => [key, info[key]]))
  }

  function setAuthenticated(value) {
    authenticated.value = Boolean(value)
  }

  /**
   * 在受保护路由首次加载时验证HttpOnly Cookie，并初始化CSRF令牌。
   */
  async function initializeSession() {
    if (sessionInitialized.value) return authenticated.value
    try {
      await prepareCsrfToken()
      authenticated.value = true
    } catch (error) {
      authenticated.value = false
    } finally {
      // 202609201535 - 完成了页面刷新后通过安全Cookie恢复内存认证状态的任务。
      sessionInitialized.value = true
    }
    return authenticated.value
  }

  function setUserInfo(info) {
    userInfo.value = info
    const sessionUser = toSessionUser(info)
    if (sessionUser) localStorage.setItem('sessionUser', JSON.stringify(sessionUser))
    else localStorage.removeItem('sessionUser')
  }

  function setMustChangePassword(value) {
    mustChangePassword.value = Boolean(value)
    localStorage.setItem('mustChangePassword', String(Boolean(value)))
  }

  function logout() {
    authenticated.value = false
    sessionInitialized.value = true
    userInfo.value = null
    sessionStorage.removeItem('token')
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('sessionUser')
    localStorage.removeItem('mustChangePassword')
  }

  return {
    authenticated,
    sessionInitialized,
    userInfo,
    mustChangePassword,
    setAuthenticated,
    initializeSession,
    setUserInfo,
    setMustChangePassword,
    logout
  }
})
