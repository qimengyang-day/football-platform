<template>
  <div class="gf-layout role-player">
    <header class="gf-header">
      <div class="gf-brand">
        <div class="gf-brand-badge" aria-hidden="true"></div>
        <div>
          <div class="gf-brand-title">绿茵链知</div>
          <div class="gf-brand-sub">球员 · 档案与归属</div>
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
              <el-dropdown-item @click="goToProfile">个人中心</el-dropdown-item>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="gf-main">
      <aside class="gf-sidebar" :class="{ 'is-collapsed': isSidebarCollapsed }">
        <el-menu :default-active="activeMenu" :collapse="isSidebarCollapsed" router>
          <el-menu-item index="/player/profile">
            <el-icon><UserFilled /></el-icon>
            <span>个人档案</span>
          </el-menu-item>
          <el-menu-item v-if="showClubMenu" index="/player/club">
            <el-icon><OfficeBuilding /></el-icon>
            <span>我的俱乐部</span>
          </el-menu-item>
        </el-menu>
        <div class="gf-sidebar-toggle" @click="toggleSidebar">
          <el-icon>{{ isSidebarCollapsed ? 'ArrowRight' : 'ArrowLeft' }}</el-icon>
        </div>
      </aside>
      <!-- 移动端遮罩层 -->
      <div class="sidebar-overlay" v-if="!isSidebarCollapsed && windowWidth < 768" @click="toggleSidebar"></div>
      <main class="gf-content">
        <div class="gf-surface" style="padding: 16px;">
          <router-view />
        </div>
        <!-- 返回顶部按钮 -->
        <div class="back-to-top" @click="scrollToTop" v-if="showBackToTop">
          <el-icon><ArrowUp /></el-icon>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { UserFilled, OfficeBuilding, ArrowLeft, ArrowRight, ArrowUp } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isSidebarCollapsed = ref(false)
const showBackToTop = ref(false)
const windowWidth = ref(window.innerWidth)
const showClubMenu = ref(true)

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

const safeAvatar = computed(() => {
  const avatar = userStore.avatar
  if (!avatar || avatar === 'undefined' || avatar === 'null' || avatar === 'nan') {
    return '/uploads/default/player.jpg'
  }
  return avatar
})

const safeNickname = computed(() => {
  const nickname = userStore.nickname
  const username = userStore.username
  if (nickname && nickname !== 'undefined' && nickname !== 'null' && nickname !== 'nan') {
    return nickname
  }
  if (username && username !== 'undefined' && username !== 'null' && username !== 'nan') {
    return username
  }
  return '用户'
})

const activeMenu = computed(() => {
  return router.currentRoute.value.fullPath
})

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}

const goToProfile = () => {
  router.push('/player/profile')
}

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

const handleScroll = () => {
  showBackToTop.value = window.scrollY > 300
}

const handleResize = () => {
  windowWidth.value = window.innerWidth
  // 在小屏幕上默认收起侧边栏
  if (windowWidth.value < 768) {
    isSidebarCollapsed.value = true
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
  // 初始化时检查窗口大小
  handleResize()
  // 自由身不显示“我的俱乐部”
  axios
    .get('/api/player/me')
    .then((res) => {
      if (res.data.code === 200) {
        const me = res.data.data
        showClubMenu.value = !!me?.teamId
        // 若没有俱乐部且当前在club页，自动跳回档案页
        if (!showClubMenu.value && router.currentRoute.value.fullPath.startsWith('/player/club')) {
          router.push('/player/profile')
        }
      }
    })
    .catch(() => {})
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
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

.nickname {
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .gf-sidebar {
    position: fixed;
    left: 0;
    top: 60px;
    height: calc(100vh - 60px);
    z-index: 1000;
    transform: translateX(0);
    transition: transform 0.3s;
  }

  .gf-sidebar.is-collapsed {
    transform: translateX(-100%);
    width: var(--gf-sidebar-w);
  }

  .nickname {
    display: none;
  }
}

/* 遮罩层样式 */
.sidebar-overlay {
  position: fixed;
  top: 60px;
  left: 200px;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  transition: opacity 0.3s;
}

@media (max-width: 768px) {
  .sidebar-overlay {
    left: 0;
  }
}

@media (max-width: 480px) {
  .gf-header { padding: 0 12px; }
}

/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #16a34a, #10b981);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  z-index: 1000;
}

.back-to-top:hover {
  filter: brightness(1.05);
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.back-to-top:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .back-to-top {
    bottom: 20px;
    right: 20px;
    width: 40px;
    height: 40px;
  }
}
</style>