import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    user: uni.getStorageSync('user') || null
  }),
  getters: {
    isLogined: (state) => !!state.token,
    displayName: (state) => (state.user ? state.user.realName : ''),
    role: (state) => (state.user ? state.user.role : ''),
    isAdmin: (state) => state.user && state.user.role === 'ADMIN',
    isManager: (state) => state.user && ['ADMIN', 'PARTNER'].includes(state.user.role)
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      uni.setStorageSync('token', token)
      uni.setStorageSync('user', user)
    },
    logout() {
      this.token = ''
      this.user = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('user')
    }
  }
})
