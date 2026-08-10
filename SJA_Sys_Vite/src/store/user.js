import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const mustChangePassword = ref(localStorage.getItem('mustChangePassword') === 'true')

  // 安全地解析 userInfo
  let userInfoValue = null
  try {
    const stored = localStorage.getItem('userInfo')
    if (stored && stored !== 'null' && stored !== 'undefined') {
      userInfoValue = JSON.parse(stored)
    }
  } catch (e) {
    console.error('Failed to parse userInfo:', e)
    localStorage.removeItem('userInfo')
    userInfoValue = null
  }
  const userInfo = ref(userInfoValue)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function setMustChangePassword(value) {
    mustChangePassword.value = Boolean(value)
    localStorage.setItem('mustChangePassword', String(Boolean(value)))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
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
