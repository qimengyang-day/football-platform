<template>
  <div class="dashboard">
    <div class="header">
      <h1>绿荫平台</h1>
    </div>

    <div class="stats">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>平台概览</span>
          </div>
        </template>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ dashboardData.clubCount || 0 }}</div>
            <div class="stat-label">俱乐部</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ dashboardData.playerCount || 0 }}</div>
            <div class="stat-label">球员</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ dashboardData.fanCount || 0 }}</div>
            <div class="stat-label">球迷</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ dashboardData.adminCount || 0 }}</div>
            <div class="stat-label">管理员</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 添加/编辑用户 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="editingUser ? '编辑用户' : '添加用户'"
      width="620px"
    >
      <el-form :model="userForm" label-width="100px">
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload/avatar"
            :headers="{ Authorization: `Bearer ${getToken()}` }"
            :show-file-list="false"
            :on-success="handleUserAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <el-avatar :size="72" :src="userForm.avatar || '/src/assets/images/default-avatar.svg'" />
          </el-upload>
          <div class="upload-tip">支持 JPG/PNG，最大 2MB</div>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="userForm.username" :disabled="editingUser" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="userForm.password"
            type="password"
            :disabled="editingUser"
            placeholder="编辑时禁用，重置请用重置密码按钮"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="俱乐部" value="CLUB" />
            <el-option label="球员" value="PLAYER" />
            <el-option label="球迷" value="FAN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userForm.role === 'CLUB'" label="所属俱乐部">
          <el-select v-model="userForm.mainTeamId" placeholder="请选择所属俱乐部（主队ID）" clearable style="width: 100%">
            <el-option
              v-for="c in availableClubsForRole"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 添加/编辑俱乐部 -->
    <el-dialog
      v-model="clubDialogVisible"
      :title="editingClub ? '编辑俱乐部' : '添加俱乐部'"
      width="720px"
    >
      <el-form :model="clubForm" label-width="120px">
        <el-form-item label="俱乐部名称">
          <el-input v-model="clubForm.name" />
        </el-form-item>

        <el-form-item label="俱乐部头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload/avatar"
            :headers="{ Authorization: `Bearer ${getToken()}` }"
            :show-file-list="false"
            :on-success="handleClubLogoSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <el-avatar :size="72" :src="clubForm.logo || '/src/assets/images/default-avatar.svg'" />
          </el-upload>
          <div class="upload-tip">支持 JPG/PNG，最大 2MB</div>
        </el-form-item>

        <el-form-item label="俱乐部管理员">
          <el-select v-model="clubForm.managerId" placeholder="可选：请选择 role=CLUB 的用户" clearable>
            <el-option v-for="m in clubManagerOptions" :key="m.id" :label="`${m.username}(${m.id})`" :value="m.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="主教练">
          <el-input v-model="clubForm.headCoach" />
        </el-form-item>

        <el-form-item label="翻译(可用逗号分隔)">
          <el-input v-model="clubForm.translator" />
        </el-form-item>

        <el-form-item label="赞助商(可用逗号分隔)">
          <el-input v-model="clubForm.sponsor" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="clubForm.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="clubDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveClub">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('users')
const currentPage = ref(1)
const pageSize = ref(10)
const totalUsers = ref(0)
const totalClubs = ref(0)

const dashboardData = ref({ clubCount: 0, playerCount: 0, fanCount: 0, adminCount: 0 })
const users = ref([])
const clubs = ref([])

const userDialogVisible = ref(false)
const editingUser = ref(false)
const userForm = ref({
  id: null,
  username: '',
  nickname: '',
  password: '',
  role: 'FAN',
  phone: '',
  status: 1,
  avatar: '',
  mainTeamId: null
})

const clubDialogVisible = ref(false)
const editingClub = ref(false)
const clubForm = ref({
  id: null,
  name: '',
  logo: '',
  managerId: null,
  description: '',
  headCoach: '',
  translator: '',
  sponsor: ''
})

const clubManagerOptions = ref([])

const getToken = () => localStorage.getItem('token') || ''

const getRoleName = (role) => {
  const roleMap = { ADMIN: '管理员', CLUB: '俱乐部', PLAYER: '球员', FAN: '球迷' }
  return roleMap[role] || role
}

const getRoleType = (role) => {
  const typeMap = { ADMIN: 'primary', CLUB: 'warning', PLAYER: 'success', FAN: 'info' }
  return typeMap[role] || 'default'
}

const availableClubsForRole = computed(() => {
  // 只展示未绑定俱乐部角色（managerId 为空）的俱乐部，确保 1:1 关系更稳
  return (clubs.value || []).filter(c => !c.managerId)
})

const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJPG && !isPNG) {
    ElMessage.error('头像图片只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像图片大小不能超过 2MB!')
    return false
  }
  return true
}

const handleUserAvatarSuccess = (res) => {
  if (res.code === 200) {
    userForm.value.avatar = res.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(res.message || '头像上传失败')
  }
}

const handleClubLogoSuccess = (res) => {
  if (res.code === 200) {
    clubForm.value.logo = res.data
    ElMessage.success('俱乐部头像上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const openAddUser = () => {
  editingUser.value = false
  userForm.value = {
    id: null,
    username: '',
    nickname: '',
    password: '',
    role: 'FAN',
    phone: '',
    status: 1,
    avatar: '',
    mainTeamId: null
  }
  userDialogVisible.value = true
}

const openEditUser = (u) => {
  editingUser.value = true
  userForm.value = {
    ...u,
    // 编辑时禁用密码，避免把无效/空密码带到后端
    password: '',
    mainTeamId: u.mainTeamId ?? null
  }
  userDialogVisible.value = true
}

const saveUser = async () => {
  try {
    const token = getToken()
    if (userForm.value.role === 'CLUB' && !userForm.value.mainTeamId) {
      ElMessage.error('新增俱乐部角色时必须选择“所属俱乐部”')
      return
    }
    if (!editingUser.value && (!userForm.value.password || !userForm.value.password.trim())) {
      ElMessage.error('请填写密码')
      return
    }
    const req = editingUser.value
      ? axios.put('/api/admin/user/update', userForm.value, { headers: { Authorization: `Bearer ${token}` } })
      : axios.post('/api/admin/user/add', userForm.value, { headers: { Authorization: `Bearer ${token}` } })
    const res = await req
    if (res.data.code === 200) {
      userDialogVisible.value = false
      ElMessage.success(editingUser.value ? '更新用户成功' : '添加用户成功')
      await refreshUsers()
      await refreshClubManagers()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

const deleteUser = async (id) => {
  try {
    const token = getToken()
    const res = await axios.delete(`/api/admin/user/delete/${id}`, { headers: { Authorization: `Bearer ${token}` } })
    if (res.data.code === 200) {
      ElMessage.success('删除用户成功')
      await refreshUsers()
      await refreshClubManagers()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

const resetUserPassword = async (id) => {
  try {
    const token = getToken()
    const res = await axios.put(`/api/admin/user/reset-password/${id}`, {}, { headers: { Authorization: `Bearer ${token}` } })
    if (res.data.code === 200) {
      ElMessage.success('密码重置成功（默认 123456）')
    } else {
      ElMessage.error(res.data.message || '重置失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '重置失败')
  }
}

const refreshUsers = async () => {
  try {
    const token = getToken()
    const res = await axios.get('/api/admin/users', {
      params: { pageNum: currentPage.value, pageSize: pageSize.value },
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 200) {
      users.value = res.data.data.list || []
      totalUsers.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('刷新用户失败')
  }
}

const refreshClubManagers = async () => {
  // 为避免 CLUB 管理员数量超过单页，按分页持续拉取直到拿满 total（total 是所有角色总数）
  try {
    const token = getToken()
    const pageSizeLocal = 100
    let pageNumLocal = 1
    let totalAll = 0
    const allClubManagers = []

    while (true) {
      const res = await axios.get('/api/admin/users', {
        params: { pageNum: pageNumLocal, pageSize: pageSizeLocal },
        headers: { Authorization: `Bearer ${token}` }
      })

      if (res.data.code !== 200) break

      const list = res.data.data?.list || []
      const totalFromServer = res.data.data?.total
      if (pageNumLocal === 1) totalAll = totalFromServer || list.length

      allClubManagers.push(...list.filter((u) => u.role === 'CLUB'))

      // 结束条件：已拉满所有用户页，或这一页没数据
      if (list.length === 0) break
      if (pageNumLocal * pageSizeLocal >= totalAll) break

      pageNumLocal += 1
    }

    clubManagerOptions.value = allClubManagers
  } catch (e) {
    clubManagerOptions.value = []
  }
}

const openAddClub = () => {
  editingClub.value = false
  clubForm.value = {
    id: null,
    name: '',
    logo: '',
    managerId: null,
    description: '',
    headCoach: '',
    translator: '',
    sponsor: ''
  }
  clubDialogVisible.value = true
}

const openEditClub = (c) => {
  editingClub.value = true
  clubForm.value = {
    id: c.id,
    name: c.name || '',
    logo: c.logo || '',
    managerId: c.managerId,
    description: c.description || '',
    headCoach: c.headCoach || '',
    translator: c.translator || '',
    sponsor: c.sponsor || ''
  }
  clubDialogVisible.value = true
}

const saveClub = async () => {
  try {
    const token = getToken()
    const req = editingClub.value
      ? axios.put('/api/admin/club/update', clubForm.value, { headers: { Authorization: `Bearer ${token}` } })
      : axios.post('/api/admin/club/add', clubForm.value, { headers: { Authorization: `Bearer ${token}` } })
    const res = await req
    if (res.data.code === 200) {
      clubDialogVisible.value = false
      ElMessage.success(editingClub.value ? '更新俱乐部成功' : '添加俱乐部成功')
      await refreshClubs()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

const deleteClub = async (id) => {
  try {
    const token = getToken()
    const res = await axios.delete(`/api/admin/club/delete/${id}`, { headers: { Authorization: `Bearer ${token}` } })
    if (res.data.code === 200) {
      ElMessage.success('删除俱乐部成功')
      await refreshClubs()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

const refreshClubs = async () => {
  try {
    const token = getToken()
    const res = await axios.get('/api/admin/clubs', {
      params: { pageNum: currentPage.value, pageSize: pageSize.value },
      headers: { Authorization: `Bearer ${token}` }
    })
    if (res.data.code === 200) {
      clubs.value = res.data.data.list || []
      totalClubs.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('刷新俱乐部失败')
  }
}

const handleSizeChange = async (size) => {
  pageSize.value = size
  if (activeTab.value === 'users') await refreshUsers()
  if (activeTab.value === 'clubs') await refreshClubs()
}

const handleCurrentChange = async (current) => {
  currentPage.value = current
  if (activeTab.value === 'users') await refreshUsers()
  if (activeTab.value === 'clubs') await refreshClubs()
}

const getDashboardData = async () => {
  try {
    const token = getToken()
    const res = await axios.get('/api/admin/data/dashboard', { headers: { Authorization: `Bearer ${token}` } })
    if (res.data.code === 200) dashboardData.value = res.data.data || {}
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  await getDashboardData()
  await refreshUsers()
  await refreshClubManagers()
  await refreshClubs()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  color: #333;
  margin: 0;
}

.stats {
  margin-bottom: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.content {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

.tab-content {
  margin-top: 20px;
}

.actions {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>