<template>
  <div class="auth-page">
    <div class="bg" aria-hidden="true"></div>
    <div class="content">
      <div class="brand">
        <div class="title">创建你的账户</div>
        <div class="subtitle">选择你的身份后，我们会展示对应的注册信息</div>
        <div class="chips">
          <span class="chip">无短信</span>
          <span class="chip">无验证码</span>
          <span class="chip">角色自动分流</span>
        </div>
      </div>

      <el-card class="card" shadow="always">
        <template #header>
          <div class="card-header">
            <div class="card-title">注册</div>
            <div class="card-desc">仅允许注册：球迷 / 球员 / 俱乐部（管理员不可注册）</div>
          </div>
        </template>

        <el-form
          :model="registerForm"
          :rules="rules"
          ref="registerFormRef"
          label-position="top"
        >
          <div class="role-grid">
            <div
              class="role-card"
              :class="{ active: registerForm.role === 'FAN' }"
              @click="registerForm.role = 'FAN'"
            >
              <div class="role-title">球迷</div>
              <div class="role-desc">关注主队、赛况评论</div>
            </div>
            <div
              class="role-card"
              :class="{ active: registerForm.role === 'PLAYER' }"
              @click="registerForm.role = 'PLAYER'"
            >
              <div class="role-title">球员</div>
              <div class="role-desc">档案、身价、俱乐部归属</div>
            </div>
            <div
              class="role-card"
              :class="{ active: registerForm.role === 'CLUB' }"
              @click="registerForm.role = 'CLUB'"
            >
              <div class="role-title">俱乐部</div>
              <div class="role-desc">管理球队、赛程与人员</div>
            </div>
          </div>

          <el-form-item label="账号" prop="username">
            <el-input v-model="registerForm.username" placeholder="请输入用户名（全站唯一）" size="large" />
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="请输入昵称（全站唯一）" size="large" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" size="large" />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" size="large" />
          </el-form-item>

          <el-form-item
            v-if="registerForm.role === 'CLUB'"
            label="俱乐部名称"
            prop="clubName"
          >
            <el-input v-model="registerForm.clubName" placeholder="例如：上海海港" size="large" />
          </el-form-item>

          <el-form-item label="联系方式（可选）" prop="phone">
            <el-input v-model="registerForm.phone" placeholder="手机号/联系方式（不做短信验证）" size="large" />
          </el-form-item>

          <div class="actions">
            <el-button type="primary" size="large" :loading="loading" @click="register" class="btn-primary">
              注册并前往登录
            </el-button>
            <el-button size="large" @click="goToLogin" class="btn-ghost">已有账号，去登录</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = ref({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  role: 'FAN',
  phone: '',
  clubName: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      validator: async (rule, value, callback) => {
        // 检查用户名是否已存在
        try {
          const response = await axios.get('/api/auth/check-username', {
            params: { username: value }
          })
          if (response.data.code === 200 && response.data.data) {
            callback(new Error('该用户名已被注册，请更换其他用户名'))
          } else {
            callback()
          }
        } catch (error) {
          callback(new Error('检查用户名失败，请稍后重试'))
        }
      },
      trigger: 'blur'
    }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    {
      validator: async (rule, value, callback) => {
        // 检查昵称是否已存在
        try {
          const response = await axios.get('/api/auth/check-nickname', {
            params: { nickname: value }
          })
          if (response.data.code === 200 && response.data.data) {
            callback(new Error('该昵称已被使用，请更换其他昵称'))
          } else {
            callback()
          }
        } catch (error) {
          callback(new Error('检查昵称失败，请稍后重试'))
        }
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.value.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  clubName: [
    {
      validator: async (rule, value, callback) => {
        if (registerForm.value.role !== 'CLUB') return callback()
        const name = String(value || '').trim()
        if (!name) return callback(new Error('请输入俱乐部名称'))
        try {
          const res = await axios.get('/api/public/club/check-name', { params: { name } })
          if (res?.data?.code === 200 && res.data.data === true) {
            callback(new Error('该俱乐部名称已存在，请更换'))
          } else {
            callback()
          }
        } catch (e) {
          callback(new Error('校验俱乐部名称失败，请稍后重试'))
        }
      },
      trigger: 'blur'
    }
  ],
  phone: []
}

const register = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload = {
          username: registerForm.value.username,
          nickname: registerForm.value.nickname,
          password: registerForm.value.password,
          role: registerForm.value.role,
          phone: registerForm.value.phone,
          clubName: registerForm.value.role === 'CLUB' ? registerForm.value.clubName : undefined
        }
        const response = await axios.post('/api/auth/register', payload)
        if (response.data.code === 200) {
          ElMessage.success('注册成功，请登录')
          router.push('/login')
        } else {
          // 后端返回业务错误（HTTP 200 但 code != 200）
          const errorMsg = response.data?.message || '注册失败'
          ElMessage.error(errorMsg)
        }
      } catch (error) {
        console.error('注册失败:', error)
        // 优先显示后端返回的错误信息
        const errorMsg = error.response?.data?.message || error.message || '注册失败，请检查网络连接'
        ElMessage.error(errorMsg)
      } finally {
        loading.value = false
      }
    }
  })
}

const goToLogin = () => {
  router.push('/login')
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
  background: radial-gradient(1200px 600px at 20% 10%, rgba(34, 197, 94, 0.22), transparent 55%),
    radial-gradient(900px 500px at 85% 25%, rgba(16, 185, 129, 0.16), transparent 60%),
    linear-gradient(180deg, #07130c 0%, #06150d 50%, #07130c 100%);
}

.bg {
  position: absolute;
  inset: -40%;
  background:
    radial-gradient(circle at 25% 35%, rgba(34,197,94,0.16) 0 35%, transparent 36%),
    radial-gradient(circle at 75% 55%, rgba(16,185,129,0.12) 0 30%, transparent 31%),
    repeating-linear-gradient(90deg, rgba(255,255,255,0.03) 0 1px, transparent 1px 22px);
  transform: rotate(-8deg);
  opacity: 0.9;
}

.content {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  position: relative;
  z-index: 1;
  align-items: start;
}

.brand {
  color: rgba(255, 255, 255, 0.92);
  padding: 10px 8px;
  animation: floatIn 520ms ease-out both;
}

.brand .title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 6px;
}

.brand .subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.6;
}

.chips {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.12);
  color: rgba(255,255,255,0.86);
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

.role-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}

.role-card {
  border-radius: 14px;
  padding: 12px 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255,255,255,0.75);
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
  user-select: none;
}

.role-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(2, 44, 26, 0.10);
}

.role-card.active {
  border-color: rgba(16, 185, 129, 0.75);
  box-shadow: 0 14px 34px rgba(16, 185, 129, 0.18);
}

.role-title {
  font-weight: 800;
  color: #0f172a;
}

.role-desc {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.62);
}

.actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-top: 6px;
}

.btn-primary {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(135deg, #16a34a, #10b981);
  border: none;
}

.btn-ghost {
  width: 100%;
  border-radius: 12px;
}

@media (max-width: 960px) {
  .content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .role-grid {
    grid-template-columns: 1fr;
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
</style>