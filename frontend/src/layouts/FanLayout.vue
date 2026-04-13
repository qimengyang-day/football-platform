<template>
  <div class="gf-layout role-fan">
    <header class="gf-header">
      <div class="gf-brand">
        <div class="gf-brand-badge" aria-hidden="true"></div>
        <div>
          <div class="gf-brand-title">绿茵链知</div>
          <div class="gf-brand-sub">球迷 · 赛事与互动</div>
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
        <el-menu :default-active="activeMenu" :collapse="isSidebarCollapsed" @select="handleMenuSelect">
          <el-menu-item index="/fan/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/fan/team">
            <el-icon><StarFilled /></el-icon>
            <span>我的主队</span>
          </el-menu-item>
          <el-menu-item index="/fan/matches">
            <el-icon><Calendar /></el-icon>
            <span>赛事列表</span>
          </el-menu-item>
          <el-menu-item index="/fan/players">
            <el-icon><UserFilled /></el-icon>
            <span>球员榜单</span>
          </el-menu-item>
          <el-menu-item index="/fan/comments">
            <el-icon><ChatDotRound /></el-icon>
            <span>评论区</span>
          </el-menu-item>
          <el-menu-item index="/fan/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
        <div class="gf-sidebar-toggle" @click="toggleSidebar">
          <el-icon>{{ isSidebarCollapsed ? 'ArrowRight' : 'ArrowLeft' }}</el-icon>
        </div>
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { HomeFilled, StarFilled, Calendar, UserFilled, ChatDotRound, User, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isSidebarCollapsed = ref(false)

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

const activeMenu = computed(() => {
  return router.currentRoute.value.fullPath
})

const safeAvatar = computed(() => {
  const avatar = userStore.avatar
  if (!avatar || avatar === 'undefined' || avatar === 'null' || avatar === 'nan') {
    return '/uploads/default/fan.jpg'
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

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}

const goToProfile = () => {
  router.push('/fan/profile')
}

const handleMenuSelect = (key, keyPath) => {
  router.push(key)
}
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

  .gf-brand { min-width: auto; }

  .nickname {
    display: none;
  }
}

@media (max-width: 480px) {
  .gf-header { padding: 0 12px; }
}
</style>