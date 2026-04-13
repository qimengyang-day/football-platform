<template>
  <div class="auth-page">
    <div class="bg" aria-hidden="true"></div>
    <div class="content">
      <div class="brand">
        <div class="logo">
          <span class="ball"></span>
          <span class="stripe"></span>
        </div>
        <div class="title">绿茵链知</div>
        <div class="subtitle">足球一体化智慧建设服务平台</div>
        <div class="tagline">赛事 · 球员 · 俱乐部 · 生态</div>
      </div>

      <el-card class="card" shadow="always">
        <template #header>
          <div class="card-header">
            <div class="card-title">欢迎回来</div>
            <div class="card-desc">用户名/手机号登录，系统自动识别角色</div>
          </div>
        </template>

        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-position="top">
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名或手机号（球迷/球员）"
              autocomplete="username"
              size="large"
              @keyup.enter="login"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              size="large"
              @keyup.enter="login"
            />
          </el-form-item>

          <div class="actions">
            <el-button type="primary" size="large" :loading="loading" @click="login" class="btn-primary">
              登录
            </el-button>
            <el-button size="large" @click="goToRegister" class="btn-ghost">去注册</el-button>
          </div>

          <div class="helper">
            <span>仅支持：球迷 / 球员 / 俱乐部</span>
            <span class="dot">·</span>
            <span>无验证码</span>
          </div>
        </el-form>
      </el-card>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="changePasswordDialogVisible" title="修改密码" width="400px">
      <div class="password-warning">
        <el-icon><WarningFilled /></el-icon>
        <span>检测到您当前使用的是初始密码，为了账户安全，请立即修改密码</span>
      </div>
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="skipChangePassword">暂不修改</el-button>
          <el-button type="primary" @click="confirmChangePassword" :loading="changingPassword">立即修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from '@/utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref(null)
const loading = ref(false)
const changingPassword = ref(false)
const changePasswordDialogVisible = ref(false)
const passwordFormRef = ref(null)

const loginForm = ref({
  username: '',
  password: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const login = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/auth/login', loginForm.value)
        if (response.data.code === 200) {
          const userInfo = response.data.data
          userStore.setUserInfo(userInfo)
          
          // 检测是否是初始密码（假设初始密码是 123456）
          if (userInfo.isDefaultPassword) {
            changePasswordDialogVisible.value = true
          } else {
            // 根据角色跳转到对应页面
            navigateByRole(userInfo.role)
          }
        } else {
          // 后端返回业务错误（HTTP 200 但 code != 200）
          const errorMsg = response.data?.message || '登录失败'
          ElMessage.error(errorMsg)
        }
      } catch (error) {
        console.error('登录失败:', error)
        // 优先显示后端返回的错误信息
        const errorMsg = error.response?.data?.message || error.message || '登录失败，请检查网络连接'
        ElMessage.error(errorMsg)
      } finally {
        loading.value = false
      }
    }
  })
}

const navigateByRole = (role) => {
  switch (role) {
    case 'ADMIN':
      router.push('/admin/green-field')
      break
    case 'PLAYER':
      router.push('/player/profile')
      break
    case 'FAN':
      router.push('/fan/home')
      break
    case 'CLUB':
      router.push('/club/dashboard')
      break
    default:
      router.push('/login')
  }
}

const confirmChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      changingPassword.value = true
      try {
        const token = localStorage.getItem('token')
        const response = await axios.put('/api/auth/change-password', {
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword
        }, {
          headers: { Authorization: `Bearer ${token}` }
        })
        
        if (response.data.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          changePasswordDialogVisible.value = false
          // 清空登录信息，跳转到登录页
          userStore.logout()
          router.push('/login')
        } else {
          ElMessage.error(response.data?.message || '密码修改失败')
        }
      } catch (error) {
        console.error('密码修改失败:', error)
        ElMessage.error(error.response?.data?.message || '密码修改失败')
      } finally {
        changingPassword.value = false
      }
    }
  })
}

const skipChangePassword = () => {
  changePasswordDialogVisible.value = false
  // 根据角色跳转到对应页面
  navigateByRole(userStore.role)
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: radial-gradient(1200px 600px at 20% 10%, rgba(34, 197, 94, 0.25), transparent 55%),
    radial-gradient(900px 500px at 85% 25%, rgba(16, 185, 129, 0.18), transparent 60%),
    linear-gradient(180deg, #07130c 0%, #06150d 50%, #07130c 100%);
}

.bg {
  position: absolute;
  inset: -40%;
  background:
    radial-gradient(circle at 30% 30%, rgba(34,197,94,0.18) 0 35%, transparent 36%),
    radial-gradient(circle at 70% 60%, rgba(16,185,129,0.14) 0 30%, transparent 31%),
    repeating-linear-gradient(90deg, rgba(255,255,255,0.03) 0 1px, transparent 1px 22px);
  transform: rotate(-8deg);
  filter: blur(0.2px);
  opacity: 0.9;
}

.content {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 28px;
  position: relative;
  z-index: 1;
  align-items: center;
}

.brand {
  color: rgba(255, 255, 255, 0.92);
  padding: 8px 8px 8px 6px;
  animation: floatIn 520ms ease-out both;
}

.logo {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  position: relative;
  background: linear-gradient(135deg, rgba(34,197,94,0.26), rgba(16,185,129,0.12));
  border: 1px solid rgba(255,255,255,0.12);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.35);
  margin-bottom: 14px;
  overflow: hidden;
}

.logo .ball {
  position: absolute;
  inset: 12px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 35% 35%, rgba(255,255,255,0.95), rgba(255,255,255,0.25) 55%, rgba(0,0,0,0.12) 100%),
    repeating-linear-gradient(45deg, rgba(0,0,0,0.18) 0 2px, transparent 2px 8px);
  opacity: 0.95;
}

.logo .stripe {
  position: absolute;
  left: -30%;
  top: 10%;
  width: 160%;
  height: 60%;
  background: linear-gradient(90deg, transparent, rgba(34,197,94,0.5), transparent);
  transform: rotate(-18deg);
  animation: sweep 2.8s ease-in-out infinite;
}

.title {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.6px;
}

.subtitle {
  margin-top: 6px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.6;
}

.tagline {
  margin-top: 14px;
  display: inline-flex;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.12);
  color: rgba(255,255,255,0.84);
  font-size: 13px;
}

.card {
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(10px);
  animation: popIn 520ms cubic-bezier(.2,.8,.2,1) both;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-weight: 800;
  font-size: 18px;
  color: #0f172a;
}

.card-desc {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.62);
}

.actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-top: 4px;
}

.btn-primary {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(135deg, #16a34a, #10b981);
  border: none;
}

.btn-primary:hover {
  filter: brightness(1.03);
}

.btn-ghost {
  width: 100%;
  border-radius: 12px;
}

.helper {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.58);
  user-select: none;
}

.dot {
  margin: 0 8px;
}

@media (max-width: 960px) {
  .content {
    grid-template-columns: 1fr;
  }
  .brand {
    text-align: center;
  }
  .logo {
    margin-left: auto;
    margin-right: auto;
  }
  .tagline {
    justify-content: center;
  }
}

@keyframes popIn {
  from { transform: translateY(10px) scale(0.98); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}

@keyframes floatIn {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@keyframes sweep {
  0%, 100% { transform: translateX(-12%) rotate(-18deg); opacity: 0.55; }
  50% { transform: translateX(12%) rotate(-18deg); opacity: 0.9; }
}

.password-warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
  margin-bottom: 16px;
  color: #fa8c16;
}

.password-warning .el-icon {
  font-size: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>