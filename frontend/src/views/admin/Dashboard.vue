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

    <div class="content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户信息" name="users">
          <div class="tab-content">
            <div class="actions">
              <el-button type="primary" @click="addUser">添加用户</el-button>
              <el-button @click="refreshUsers">刷新</el-button>
            </div>
            <el-table :data="users" style="width: 100%">
              <el-table-column prop="username" label="用户名"></el-table-column>
              <el-table-column prop="role" label="角色">
                <template #default="scope">
                  <el-tag :type="getRoleType(scope.row.role)">{{ getRoleName(scope.row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                    {{ scope.row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button size="small" @click="viewUser(scope.row.id)">查看</el-button>
                  <el-button size="small" type="primary" @click="editUser(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteUser(scope.row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="userPageNum"
              v-model:page-size="userPageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="userTotal"
              @size-change="(size) => userPg.handleSizeChange(size, refreshUsers)"
              @current-change="(page) => userPg.handlePageChange(page, refreshUsers)"
              style="margin-top: 20px;"
              prev-text="上一页"
              next-text="下一页"
              jumper-text="前往"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="俱乐部信息" name="clubs">
          <div class="tab-content">
            <div class="actions">
              <el-button type="primary" @click="addClub">添加俱乐部</el-button>
              <el-button @click="refreshClubs">刷新</el-button>
            </div>
            <el-table :data="clubs" style="width: 100%">
              <el-table-column prop="username" label="用户名"></el-table-column>
              <el-table-column prop="role" label="角色">
                <template #default="scope">
                  <el-tag :type="getRoleType(scope.row.role)">{{ getRoleName(scope.row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="俱乐部名称"></el-table-column>
              <el-table-column prop="foundDate" label="成立日期"></el-table-column>
              <el-table-column prop="status" label="状态">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                    {{ scope.row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="scope">
                  <el-button size="small" @click="viewClub(scope.row.id)">查看</el-button>
                  <el-button size="small" type="primary" @click="editClub(scope.row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="deleteClub(scope.row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="clubPageNum"
              v-model:page-size="clubPageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="clubTotal"
              @size-change="(size) => clubPg.handleSizeChange(size, refreshClubs)"
              @current-change="(page) => clubPg.handlePageChange(page, refreshClubs)"
              style="margin-top: 20px;"
              prev-text="上一页"
              next-text="下一页"
              jumper-text="前往"
            />
          </div>
        </el-tab-pane>

      </el-tabs>
    </div>

    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="editingUser ? '编辑用户' : '添加用户'"
      width="500px"
    >
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload/avatar"
            :headers="{ Authorization: `Bearer ${getToken()}` }"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="userForm.avatar" :src="userForm.avatar" class="avatar" />
            <div v-else class="avatar-placeholder">
              <el-icon><Plus /></el-icon>
              <div class="avatar-text">点击上传头像</div>
            </div>
          </el-upload>
          <div class="upload-tip">支持 JPG、PNG 格式，大小不超过 2MB</div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="userForm.username"></el-input>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="userForm.password" :disabled="editingUser"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role">
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="俱乐部" value="CLUB"></el-option>
            <el-option label="球员" value="PLAYER"></el-option>
            <el-option label="球迷" value="FAN"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="userForm.role === 'CLUB'" label="所属俱乐部">
          <el-select v-model="userForm.mainTeamId" placeholder="请选择所属俱乐部">
            <el-option
              v-for="c in availableClubsForRole"
              :key="c.id"
              :label="`${c.name}(${c.id})`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-input v-model="userForm.department"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 用户详情对话框 -->
    <el-dialog
      v-model="userDetailVisible"
      title="用户详情"
      width="600px"
    >
      <div class="user-detail">
        <div class="detail-section">
          <h3>基本信息</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户ID">{{ userDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ userDetail.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userDetail.nickname }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ getRoleName(userDetail.role) }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ userDetail.department }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ userDetail.status === 1 ? '启用' : '禁用' }}</el-descriptions-item>
            <el-descriptions-item label="头像">
              <img v-if="userDetail.avatar" :src="userDetail.avatar" class="detail-avatar" />
              <span v-else>无头像</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDetailVisible = false">关闭</el-button>
          <el-button type="primary" @click="editUserDetail">编辑</el-button>
          <el-button type="warning" @click="resetPassword">重置密码</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 俱乐部编辑对话框 -->
    <el-dialog
      v-model="clubDialogVisible"
      :title="editingClub ? '编辑俱乐部' : '添加俱乐部'"
      width="500px"
    >
      <el-form :model="clubForm" label-width="80px">
        <el-form-item label="俱乐部名称">
          <el-input v-model="clubForm.name"></el-input>
        </el-form-item>
        <el-form-item label="所在城市">
          <el-input v-model="clubForm.city"></el-input>
        </el-form-item>
        <el-form-item label="成立日期">
          <el-date-picker v-model="clubForm.foundDate" type="date" placeholder="选择日期"></el-date-picker>
        </el-form-item>
        <el-form-item label="用户名" v-if="!editingClub">
          <el-input v-model="clubForm.username" placeholder="俱乐部管理员用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" v-if="!editingClub">
          <el-input v-model="clubForm.password" type="password" placeholder="初始密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="clubForm.status"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="clubDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveClub">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 俱乐部详情（人员状况）对话框 -->
    <el-dialog
      v-model="clubDetailVisible"
      title="俱乐部人员状况"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="clubDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="俱乐部名称">{{ clubDetail.club?.name || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="主教练">{{ clubDetail.headCoach || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="翻译">{{ clubDetail.translators || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="赞助商">
            <div>
              <el-tag
                v-for="s in (clubDetail.sponsors || [])"
                :key="s"
                type="warning"
                style="margin-right: 8px; margin-bottom: 8px;"
              >
                {{ s }}
              </el-tag>
              <span v-if="(clubDetail.sponsors || []).length === 0">暂无</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="球员数量">{{ clubDetail.playerCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="总身价/价值">{{ clubDetail.totalValue || 0 }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 18px;">
          <div style="font-weight: 600; margin-bottom: 10px;">球员（前20名）</div>
          <el-table :data="clubDetail.players || []" style="width: 100%">
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="position" label="位置" width="120" />
            <el-table-column prop="age" label="年龄" width="80" />
            <el-table-column prop="nationality" label="国籍" width="140" />
            <el-table-column prop="marketValue" label="身价(万欧)" width="120" />
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAdminPagination } from '@/composables/useAdminPagination'

const router = useRouter()

const dashboardData = ref({
  clubCount: 0,
  playerCount: 0,
  fanCount: 0,
  adminCount: 0
})

const activeTab = ref('users')
const userPg = useAdminPagination()
const clubPg = useAdminPagination()
// 顶层 ref 绑定分页，避免 v-model 嵌套在普通对象属性上失效
const userPageNum = userPg.pageNum
const userPageSize = userPg.pageSize
const userTotal = userPg.total
const clubPageNum = clubPg.pageNum
const clubPageSize = clubPg.pageSize
const clubTotal = clubPg.total

// 用户列表
const users = ref([])

const clubs = ref([])

// 用户编辑相关
const userDialogVisible = ref(false)
const userDetailVisible = ref(false)
const editingUser = ref(null)
const userForm = ref({
  username: '',
  nickname: '',
  password: '',
  role: 'FAN',
  department: '',
  mainTeamId: null,
  status: 1,
  avatar: ''
})

// 获取token
const getToken = () => {
  return localStorage.getItem('token') || ''
}
const userDetail = ref({
  id: '',
  username: '',
  nickname: '',
  role: '',
  department: '',
  mainTeamId: null,
  status: 1,
  avatar: ''
})

// 俱乐部编辑相关
const clubDialogVisible = ref(false)
const editingClub = ref(null)
const clubForm = ref({
  name: '',
  city: '',
  foundDate: new Date(),
  username: '',
  password: '',
  status: 1
})

// 俱乐部详情（人员状况）
const clubDetailVisible = ref(false)
const clubDetail = ref(null)
const clubDetailLoading = ref(false)

const getRoleName = (role) => {
  const roleMap = {
    'ADMIN': '管理员',
    'PLAYER': '球员',
    'FAN': '球迷',
    'CLUB': '俱乐部'
  }
  return roleMap[role] || role
}

const getRoleType = (role) => {
  const typeMap = {
    'ADMIN': 'primary',
    'PLAYER': 'success',
    'FAN': 'info',
    'CLUB': 'warning'
  }
  return typeMap[role] || 'default'
}

const addUser = () => {
  editingUser.value = null
  userForm.value = {
    username: '',
    nickname: '',
    password: '',
    role: 'FAN',
    department: '',
    mainTeamId: null,
    status: 1,
    avatar: ''
  }
  userDialogVisible.value = true
}

// 头像上传成功回调
const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    userForm.value.avatar = response.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '头像上传失败')
  }
}

// 头像上传前校验
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

const editUser = (user) => {
  editingUser.value = user
  userForm.value = {
    ...user
  }
  userDialogVisible.value = true
}

const saveUser = async () => {
  try {
    const token = localStorage.getItem('token')
    if (userForm.value.role === 'CLUB' && !userForm.value.mainTeamId) {
      ElMessage.error('新增俱乐部角色时必须选择所属俱乐部')
      return
    }
    let response
    if (editingUser.value) {
      // 更新用户
      response = await axios.put('/api/admin/user/update', userForm.value, {
        headers: { Authorization: `Bearer ${token}` }
      })
    } else {
      // 添加用户
      response = await axios.post('/api/admin/user/add', userForm.value, {
        headers: { Authorization: `Bearer ${token}` }
      })
    }
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success(editingUser.value ? '更新用户成功' : '添加用户成功')
      userDialogVisible.value = false
      // 刷新用户列表
      await refreshUsers()
    } else {
      ElMessage.error(editingUser.value ? '更新用户失败' : '添加用户失败')
    }
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存用户失败: ' + (error.response?.data?.message || error.message))
  }
}

const deleteUser = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/admin/user/delete/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success('删除用户成功')
      await userPg.afterDeleteRefresh(refreshUsers)
    } else {
      ElMessage.error('删除用户失败')
    }
  } catch (error) {
    console.error('删除用户失败:', error)
    ElMessage.error('删除用户失败: ' + (error.response?.data?.message || error.message))
  }
}

const viewUser = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/auth/info`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      const user = users.value.find(u => u.id === id)
      if (user) {
        userDetail.value = { ...user }
        userDetailVisible.value = true
      }
    }
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ElMessage.error('获取用户详情失败')
  }
}

const editUserDetail = () => {
  editingUser.value = userDetail.value
  userForm.value = { ...userDetail.value }
  userDetailVisible.value = false
  userDialogVisible.value = true
}

const resetPassword = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.put(`/api/admin/user/reset-password/${userDetail.value.id}`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success('密码重置成功，初始密码为123456')
      userDetailVisible.value = false
    } else {
      ElMessage.error('密码重置失败')
    }
  } catch (error) {
    console.error('重置密码失败:', error)
    ElMessage.error('重置密码失败: ' + (error.response?.data?.message || error.message))
  }
}

const refreshUsers = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/users', {
      params: { pageNum: userPg.pageNum.value, pageSize: userPg.pageSize.value },
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      const result = userPg.applyPageResult(response.data.data)
      users.value = result.list
    }
  } catch (error) {
    console.error('刷新用户列表失败:', error)
    ElMessage.error('刷新用户列表失败')
  }
}

const addClub = () => {
  editingClub.value = null
  clubForm.value = {
    name: '',
    city: '',
    foundDate: new Date(),
    status: 1
  }
  clubDialogVisible.value = true
}

const editClub = (club) => {
  editingClub.value = club
  clubForm.value = {
    ...club
  }
  clubDialogVisible.value = true
}

const saveClub = async () => {
  try {
    const token = localStorage.getItem('token')
    let response
    if (editingClub.value) {
      // 更新俱乐部
      response = await axios.put('/api/admin/club/update', clubForm.value, {
        headers: { Authorization: `Bearer ${token}` }
      })
    } else {
      // 添加俱乐部（带用户名和密码）
      if (!clubForm.value.username || !clubForm.value.password) {
        ElMessage.error('请填写俱乐部管理员用户名和密码')
        return
      }
      response = await axios.post('/api/admin/club/add-with-user', clubForm.value, {
        headers: { Authorization: `Bearer ${token}` }
      })
    }
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success(editingClub.value ? '更新俱乐部成功' : '添加俱乐部成功')
      clubDialogVisible.value = false
      // 刷新俱乐部列表
      await refreshClubs()
    } else {
      ElMessage.error(response.data.message || (editingClub.value ? '更新俱乐部失败' : '添加俱乐部失败'))
    }
  } catch (error) {
    console.error('保存俱乐部失败:', error)
    ElMessage.error('保存俱乐部失败：' + (error.response?.data?.message || error.message))
  }
}

const deleteClub = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/admin/club/delete/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success('删除俱乐部成功')
      await clubPg.afterDeleteRefresh(refreshClubs)
    } else {
      ElMessage.error('删除俱乐部失败')
    }
  } catch (error) {
    console.error('删除俱乐部失败:', error)
    ElMessage.error('删除俱乐部失败: ' + (error.response?.data?.message || error.message))
  }
}

const viewClub = (id) => {
  // 查看俱乐部详情（人员状况）
  // 使用弹窗展示，避免跳转权限/角色限制导致“无反应”
  clubDetailVisible.value = true
  clubDetailLoading.value = true
  clubDetail.value = null

  const token = localStorage.getItem('token')
  axios.get(`/api/admin/club/${id}/summary`, {
    headers: { Authorization: `Bearer ${token}` }
  })
    .then((response) => {
      if (response.data.code === 200) {
        clubDetail.value = response.data.data
      } else {
        ElMessage.error(response.data.message || '获取俱乐部详情失败')
      }
    })
    .catch((error) => {
      console.error('获取俱乐部详情失败:', error)
      ElMessage.error(error.response?.data?.message || '获取俱乐部详情失败')
    })
    .finally(() => {
      clubDetailLoading.value = false
    })
}

const refreshClubs = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/clubs', {
      params: { pageNum: clubPg.pageNum.value, pageSize: clubPg.pageSize.value },
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      const result = clubPg.applyPageResult(response.data.data)
      clubs.value = result.list
    }
  } catch (error) {
    console.error('刷新俱乐部列表失败:', error)
    ElMessage.error('刷新俱乐部列表失败')
  }
}

const availableClubsForRole = computed(() => {
  // 只让 CLUB 角色绑定未被 manager_id 占用的俱乐部（严格 1:1）
  return (clubs.value || []).filter(c => !c.managerId)
})

const getDashboardData = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/data/dashboard', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      dashboardData.value = response.data.data
    }
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  }
}

onMounted(() => {
  getDashboardData()
  refreshUsers()
  refreshClubs()
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

.avatar-uploader {
  display: inline-block;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
}

.avatar-placeholder .el-icon {
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 8px;
}

.avatar-text {
  font-size: 12px;
  color: #8c939d;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.user-detail {
  padding: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h3 {
  margin-bottom: 15px;
  color: #333;
}

.detail-avatar {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  object-fit: cover;
}
</style>