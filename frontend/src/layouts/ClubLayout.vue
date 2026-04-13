<template>
  <div class="gf-layout role-club">
    <header class="gf-header">
      <div class="gf-brand">
        <div class="gf-brand-badge" aria-hidden="true"></div>
        <div>
          <div class="gf-brand-title">绿茵链知</div>
          <div class="gf-brand-sub">俱乐部 · {{ safeClubName }}</div>
        </div>
      </div>
      <div class="user-info">
        <el-dropdown>
          <div class="user-dropdown">
            <el-upload
              action="/api/upload/avatar"
              :headers="getAuthHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
            >
              <el-avatar :size="40" :src="safeAvatar" style="cursor: pointer" />
            </el-upload>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="gf-main">
      <aside class="gf-sidebar">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/club/dashboard">
            <span>{{ safeClubName }}</span>
          </el-menu-item>
        </el-menu>
      </aside>
      <main class="gf-content">
        <div class="gf-surface" style="padding: 16px;">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const clubName = ref('俱乐部')

const getAuthHeaders = () => ({ Authorization: `Bearer ${userStore.token}` })

const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    userStore.avatar = response.data
    localStorage.setItem('avatar', response.data)
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

const beforeAvatarUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJpgOrPng) ElMessage.error('只能上传 JPG/PNG 格式')
  if (!isLt2M) ElMessage.error('图片大小不能超过 2MB')
  return isJpgOrPng && isLt2M
}

const normalizeText = (v) => {
  if (v === undefined || v === null) return ''
  const s = String(v).trim()
  if (!s) return ''
  const lower = s.toLowerCase()
  if (lower === 'undefined' || lower === 'null' || lower === 'nan') return ''
  return s
}

const safeClubName = computed(() => {
  return normalizeText(clubName.value) || '俱乐部'
})

const safeNickname = computed(() => {
  return normalizeText(userStore.nickname) || normalizeText(userStore.username) || ''
})

const safeAvatar = computed(() => {
  // 俱乐部角色显示俱乐部 Logo
  if (userStore.role === 'CLUB' && userStore.clubLogo) {
    const logo = userStore.clubLogo
    if (logo && logo !== 'undefined' && logo !== 'null' && logo !== 'nan') {
      return logo
    }
  }
  // 其他角色或无 Logo 时显示用户头像
  const avatar = userStore.avatar
  if (avatar && avatar !== 'undefined' && avatar !== 'null' && avatar !== 'nan') {
    return avatar
  }
  return '/uploads/default/club.jpg'
})

const activeMenu = computed(() => {
  return router.currentRoute.value.fullPath
})

const getClubInfo = async () => {
  try {
    const response = await axios.get('/api/club/info', {
      headers: getAuthHeaders()
    })
    if (response.data.code === 200) {
      const data = response.data.data
      clubName.value = normalizeText(data?.name) || '俱乐部'
      // 同步俱乐部 Logo 到 userStore
      if (data?.logo) {
        userStore.clubLogo = data.logo
        localStorage.setItem('clubLogo', data.logo)
      }
    }
  } catch (error) {
    console.error('获取俱乐部信息失败:', error)
  }
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  getClubInfo()
})
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

@media (max-width: 480px) {
  .gf-header { padding: 0 12px; }
}
</style>