import { defineStore } from 'pinia'
import axios from '@/utils/axios'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    avatar: localStorage.getItem('avatar') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    clubLogo: localStorage.getItem('clubLogo') || ''
  }),
  actions: {
    setUserInfo(data) {
      this.token = data.token
      this.role = data.role
      this.avatar = data.avatar
      this.username = data.username
      this.nickname = data.nickname || ''
      this.clubLogo = data.clubLogo || ''
      localStorage.setItem('token', data.token)
      localStorage.setItem('role', data.role)
      localStorage.setItem('avatar', data.avatar)
      localStorage.setItem('username', data.username)
      localStorage.setItem('nickname', data.nickname || '')
      localStorage.setItem('clubLogo', data.clubLogo || '')
    },
    async logout() {
      try {
        await axios.post('/api/auth/logout')
      } catch (error) {
        console.error('退出登录失败:', error)
      } finally {
        this.$reset()
        localStorage.clear()
      }
    },
    async getCurrentUserInfo() {
      try {
        const response = await axios.get('/api/auth/info')
        if (response.data.code === 200) {
          this.avatar = response.data.data.avatar
          this.username = response.data.data.username
          this.nickname = response.data.data.nickname || ''
          if (response.data.data.clubLogo) {
            this.clubLogo = response.data.data.clubLogo
            localStorage.setItem('clubLogo', response.data.data.clubLogo)
          }
          localStorage.setItem('avatar', response.data.data.avatar)
          localStorage.setItem('username', response.data.data.username)
          localStorage.setItem('nickname', response.data.data.nickname || '')
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
      }
    }
  }
})