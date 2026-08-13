import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
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

  function setToken(newToken) {
    token.value = newToken
    if (newToken) sessionStorage.setItem('token', newToken)
    else sessionStorage.removeItem('token')
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
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('sessionUser')
    localStorage.removeItem('mustChangePassword')
  }

  return {
    token,
    userInfo,
    mustChangePassword,
    setToken,
    setUserInfo,
    setMustChangePassword,
    logout
  }
})
