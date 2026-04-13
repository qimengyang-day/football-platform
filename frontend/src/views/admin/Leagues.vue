<template>
  <div class="leagues">
    <h1>联赛管理</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>联赛列表</span>
          <el-button type="primary" @click="addLeague">添加联赛</el-button>
        </div>
      </template>
      <div class="search-bar">
        <el-input v-model="searchQuery" placeholder="搜索联赛名称" style="width: 200px; margin-right: 10px" />
        <el-button type="primary" @click="searchLeagues">搜索</el-button>
      </div>
      <el-table :data="leagues" style="width: 100%">
        <el-table-column prop="name" label="联赛名称"></el-table-column>
        <el-table-column prop="clubCount" label="俱乐部数量" width="120"></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="viewLeagueClubs(scope.row)">查看俱乐部</el-button>
            <el-button size="small" @click="editLeague(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteLeague(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="(size) => handleSizeChange(size, getLeagues)"
        @current-change="(page) => handlePageChange(page, getLeagues)"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 添加/编辑联赛对话框 -->
    <el-dialog v-model="addLeagueDialogVisible" :title="isEdit ? '编辑联赛' : '添加联赛'" width="500px">
      <el-form :model="leagueForm" :rules="rules" ref="leagueFormRef" label-width="100px">
        <el-form-item label="联赛名称" prop="name">
          <el-input v-model="leagueForm.name" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="leagueForm.description" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="封面">
          <el-upload
            class="avatar-uploader"
            action="/api/upload/image"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :before-upload="beforeCoverUpload"
          >
            <img v-if="leagueForm.cover" :src="leagueForm.cover" class="cover-image" />
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <div class="upload-text">点击上传封面</div>
            </div>
          </el-upload>
          <template #tip>
            <div class="el-upload__tip">
              请上传 JPG、PNG 格式的图片，大小不超过 2MB
            </div>
          </template>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addLeagueDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveLeague">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 联赛俱乐部列表对话框 -->
    <el-dialog v-model="leagueClubsDialogVisible" :title="currentLeagueName + ' - 俱乐部列表'" width="600px">
      <div style="display: flex; gap: 8px; margin-bottom: 12px;">
        <el-select v-model="selectedClubIdToAdd" placeholder="选择已有俱乐部添加到联赛" style="flex: 1;">
          <el-option
            v-for="club in addableClubs"
            :key="club.id"
            :label="club.name"
            :value="club.id"
          />
        </el-select>
        <el-button type="primary" @click="addClubToCurrentLeague">添加俱乐部</el-button>
      </div>
      <el-table :data="leagueClubs" style="width: 100%">
        <el-table-column prop="name" label="俱乐部名称"></el-table-column>
        <el-table-column prop="headCoach" label="主教练" width="120"></el-table-column>
        <el-table-column prop="sponsor" label="赞助商" width="150"></el-table-column>
      </el-table>
      <div style="margin-top: 16px; margin-bottom: 8px; font-weight: 600;">联赛积分榜</div>
      <el-table :data="leagueStandings" style="width: 100%">
        <el-table-column prop="rank" label="排名" width="70"></el-table-column>
        <el-table-column prop="teamName" label="球队"></el-table-column>
        <el-table-column prop="played" label="场次" width="70"></el-table-column>
        <el-table-column prop="wins" label="胜" width="60"></el-table-column>
        <el-table-column prop="draws" label="平" width="60"></el-table-column>
        <el-table-column prop="losses" label="负" width="60"></el-table-column>
        <el-table-column prop="goalDiff" label="净胜球" width="80"></el-table-column>
        <el-table-column prop="points" label="积分" width="70"></el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="leagueClubsDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAdminPagination } from '@/composables/useAdminPagination'

const leagues = ref([])
const searchQuery = ref('')
const {
  pageNum,
  pageSize,
  total,
  resetToFirstPage,
  applyPageResult,
  handlePageChange,
  handleSizeChange,
  afterDeleteRefresh
} = useAdminPagination()

const addLeagueDialogVisible = ref(false)
const isEdit = ref(false)
const leagueFormRef = ref(null)
const leagueForm = ref({
  name: '',
  description: '',
  cover: ''
})

const leagueClubsDialogVisible = ref(false)
const currentLeagueName = ref('')
const currentLeagueId = ref(null)
const leagueClubs = ref([])
const leagueStandings = ref([])
const selectedClubIdToAdd = ref(null)
const allClubs = ref([])

const rules = {
  name: [{ required: true, message: '请输入联赛名称', trigger: 'blur' }]
}

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return { Authorization: `Bearer ${token}` }
})

const getLeagues = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/leagues', {
      headers: { Authorization: `Bearer ${token}` },
      params: {
        search: searchQuery.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (response.data.code === 200) {
      const result = applyPageResult(response.data.data)
      leagues.value = result.list
    }
  } catch (error) {
    console.error('获取联赛列表失败:', error)
    ElMessage.error('获取联赛列表失败')
  }
}

const searchLeagues = () => {
  resetToFirstPage()
  getLeagues()
}

const addLeague = () => {
  isEdit.value = false
  leagueForm.value = {
    name: '',
    description: '',
    cover: ''
  }
  addLeagueDialogVisible.value = true
}

const editLeague = (league) => {
  isEdit.value = true
  leagueForm.value = {
    id: league.id,
    name: league.name,
    description: league.description,
    cover: league.cover
  }
  addLeagueDialogVisible.value = true
}

const handleCoverSuccess = (response) => {
  if (response.code === 200) {
    leagueForm.value.cover = response.data
  }
}

const beforeCoverUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJpgOrPng) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
  }
  return isJpgOrPng && isLt2M
}

const saveLeague = async () => {
  if (!leagueFormRef.value) return
  await leagueFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const token = localStorage.getItem('token')
        let response
        if (isEdit.value) {
          response = await axios.put('/api/admin/league/update', leagueForm.value, {
            headers: { Authorization: `Bearer ${token}` }
          })
        } else {
          response = await axios.post('/api/admin/league/add', leagueForm.value, {
            headers: { Authorization: `Bearer ${token}` }
          })
        }
        if (response?.data?.code === 200) {
          // Result.success(successBoolean) => data 是 true/false
          if (response.data.data === true) {
            ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
            addLeagueDialogVisible.value = false
            getLeagues()
          } else {
            ElMessage.error(response.data?.message || (isEdit.value ? '编辑失败（未更新成功）' : '添加失败（未新增成功）'))
          }
        } else {
          ElMessage.error(response?.data?.message || (isEdit.value ? '编辑失败' : '添加失败'))
        }
      } catch (error) {
        console.error(isEdit.value ? '编辑失败:' : '添加失败:', error)
        ElMessage.error(error.response?.data?.message || (isEdit.value ? '编辑失败' : '添加失败'))
      }
    }
  })
}

const deleteLeague = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/admin/league/delete/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      await afterDeleteRefresh(getLeagues)
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

const viewLeagueClubs = async (league) => {
  try {
    currentLeagueId.value = league.id
    currentLeagueName.value = league.name
    selectedClubIdToAdd.value = null
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/admin/league/clubs/${league.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      leagueClubs.value = response.data.data || []
    }
    const standingsRes = await axios.get(`/api/league/${league.id}/standings`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (standingsRes.data.code === 200) {
      leagueStandings.value = standingsRes.data.data || []
    } else {
      leagueStandings.value = []
    }
    leagueClubsDialogVisible.value = true
  } catch (error) {
    console.error('获取联赛俱乐部失败:', error)
    ElMessage.error(error.response?.data?.message || '获取联赛俱乐部失败')
  }
}

const getAllClubs = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/clubs', {
      headers: { Authorization: `Bearer ${token}` },
      params: { search: '', pageNum: 1, pageSize: 500 }
    })
    if (response.data.code === 200) {
      allClubs.value = response.data.data.list || []
    }
  } catch (error) {
    console.error('获取全部俱乐部失败:', error)
    allClubs.value = []
  }
}

const addableClubs = computed(() => {
  const inLeague = new Set((leagueClubs.value || []).map(c => c.id))
  return (allClubs.value || []).filter(c => !inLeague.has(c.id))
})

const addClubToCurrentLeague = async () => {
  try {
    if (!currentLeagueId.value) {
      ElMessage.error('未选择联赛')
      return
    }
    if (!selectedClubIdToAdd.value) {
      ElMessage.error('请选择俱乐部')
      return
    }
    const token = localStorage.getItem('token')
    const response = await axios.post('/api/admin/league/club/add', null, {
      headers: { Authorization: `Bearer ${token}` },
      params: { clubId: selectedClubIdToAdd.value, leagueId: currentLeagueId.value }
    })
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success('添加俱乐部成功')
      await viewLeagueClubs({ id: currentLeagueId.value, name: currentLeagueName.value })
      await getLeagues()
    } else {
      ElMessage.error(response.data.message || '添加失败')
    }
  } catch (error) {
    console.error('添加俱乐部到联赛失败:', error)
    ElMessage.error(error.response?.data?.message || '添加失败')
  }
}

onMounted(() => {
  getLeagues()
  getAllClubs()
})
</script>

<style scoped>
.leagues {
  padding: 20px;
}

.leagues h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.cover-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.upload-placeholder {
  width: 100%;
  height: 200px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #1890ff;
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: #999;
}

@media (max-width: 768px) {
  .leagues {
    padding: 10px;
  }
  
  .search-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .search-bar .el-input {
    width: 100% !important;
    margin-right: 0 !important;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .cover-image {
    height: 150px;
  }
  
  .upload-placeholder {
    height: 150px;
  }
}
</style>