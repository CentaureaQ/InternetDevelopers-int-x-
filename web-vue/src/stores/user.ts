import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  token?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const storedUser = localStorage.getItem('userInfo')
  let initialUserInfo: UserInfo | null = null
  if (storedUser) {
    try {
      initialUserInfo = JSON.parse(storedUser)
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  const userInfo = ref<UserInfo | null>(initialUserInfo)

  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)
  const nickname = computed(() => userInfo.value?.nickname)

  function setToken(newToken: string) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)
    } else {
      localStorage.removeItem('token')
    }
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function login(loginData: UserInfo) {
    setToken(loginData.token || '')
    setUserInfo(loginData)
  }

  function logout() {
    setToken('')
    userInfo.value = null
    localStorage.clear()
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    userId,
    username,
    nickname,
    setToken,
    setUserInfo,
    login,
    logout
  }
})



