<template>
  <div class="profile-center">
    <h1>个人资料</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>基础信息</span>
        </div>
      </template>
      <el-form :model="form" label-width="90px">
        <el-form-item label="头像">
          <el-upload
            action="/api/upload/avatar"
            :headers="{ Authorization: `Bearer ${localStorage.getItem('token')}` }"
            :show-file-list="false"
            :on-success="onAvatarUpload"
          >
            <el-avatar :size="72" :src="form.avatar || '/src/assets/images/default-avatar.svg'" />
          </el-upload>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>
      <el-form :model="pwd" label-width="90px">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="savePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const form = ref({ username: '', nickname: '', avatar: '' })
const pwd = ref({ oldPassword: '', newPassword: '' })

const load = async () => {
  const res = await axios.get('/api/auth/info')
  if (res.data.code === 200) {
    form.value = {
      username: res.data.data.username,
      nickname: res.data.data.nickname || '',
      avatar: res.data.data.avatar || ''
    }
  }
}

const onAvatarUpload = (res) => {
  if (res.code === 200) {
    form.value.avatar = res.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(res.message || '头像上传失败')
  }
}

const saveProfile = async () => {
  const res = await axios.put('/api/user/profile', {
    nickname: form.value.nickname,
    avatar: form.value.avatar
  })
  if (res.data.code === 200) {
    ElMessage.success('资料已保存')
    await userStore.getCurrentUserInfo()
  } else {
    ElMessage.error(res.data.message || '保存失败')
  }
}

const savePassword = async () => {
  const res = await axios.put('/api/user/password', pwd.value)
  if (res.data.code === 200) {
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    location.href = '/login'
  } else {
    ElMessage.error(res.data.message || '密码修改失败')
  }
}

onMounted(load)
</script>

<style scoped>
.profile-center { padding: 20px; }
.profile-center h1 { margin-bottom: 16px; color:#333; }
.card-header { display:flex; justify-content:space-between; align-items:center; }
</style>

