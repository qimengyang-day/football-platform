<template>
  <div class="profile">
    <h1>个人中心</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
          <el-button type="primary" @click="editMode = !editMode">{{ editMode ? '取消' : '编辑' }}</el-button>
        </div>
      </template>
      <div class="profile-content">
        <div class="avatar-section">
          <el-avatar :size="120" :src="userInfo.avatar || '/src/assets/images/default-avatar.svg'" />
          <el-button v-if="editMode" type="primary" size="small" @click="avatarDialogVisible = true">更换头像</el-button>
        </div>
        <el-form :model="userInfo" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userInfo.username" disabled />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="userInfo.nickname" :disabled="!editMode" />
          </el-form-item>
          <el-form-item v-if="editMode">
            <el-button type="primary" @click="updateProfile">保存</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 头像修改对话框（简化：输入URL；后续可接文件上传） -->
    <el-dialog v-model="avatarDialogVisible" title="更换头像" width="420px">
      <el-upload
        action="/api/upload/avatar"
        :headers="getAuthHeaders"
        :show-file-list="false"
        :on-success="onAvatarUploaded"
        :before-upload="beforeAvatarUpload"
      >
        <el-button type="primary">上传图片</el-button>
      </el-upload>
      <div style="margin-top: 10px; color: #666">支持 JPG/PNG，最大 2MB</div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="avatarDialogVisible = false">取消</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const editMode = ref(false)
const formRef = ref(null)
const avatarDialogVisible = ref(false)
const userInfo = ref({
  username: '',
  nickname: '',
  avatar: ''
})

const getAuthHeaders = () => ({ Authorization: `Bearer ${userStore.token}` })

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度应在 2-20 个字符之间', trigger: 'blur' }
  ]
}

const getProfile = async () => {
  try {
    const response = await axios.get('/api/auth/info')
    if (response.data.code === 200) {
      userInfo.value = response.data.data
    }
  } catch (error) {
    console.error('获取个人资料失败:', error)
    ElMessage.error('获取个人资料失败')
  }
}

const updateProfile = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const response = await axios.put('/api/user/profile', {
          nickname: userInfo.value.nickname,
          avatar: userInfo.value.avatar
        })
        if (response.data.code === 200) {
          ElMessage.success('更新成功')
          editMode.value = false
          // 更新用户存储中的信息
          userStore.setUserInfo({
            ...userStore.$state,
            nickname: userInfo.value.nickname,
            avatar: userInfo.value.avatar
          })
        }
      } catch (error) {
        console.error('更新失败:', error)
        ElMessage.error(error.response?.data?.message || '更新失败')
      }
    }
  })
}

const onAvatarUploaded = (response) => {
  if (response.code === 200) {
    userInfo.value.avatar = response.data
    avatarDialogVisible.value = false
    ElMessage.success('头像上传成功，请点击保存')
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

onMounted(() => {
  getProfile()
})
</script>

<style scoped>
.profile {
  padding: 20px;
}

.profile h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 30px;
  padding: 20px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

@media (max-width: 768px) {
  .profile {
    padding: 10px;
  }
  
  .profile-content {
    gap: 20px;
  }
}
</style>