<template>
  <div class="match-manage">
    <h1>赛事管理</h1>
    <div class="toolbar">
      <el-input v-model="searchQuery" placeholder="搜索对战双方" style="width: 220px" />
      <el-button type="primary" @click="searchMatches">搜索</el-button>
      <el-button type="primary" @click="openCreateDialog">创建赛事</el-button>
    </div>
    <el-table :data="matches" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="leagueName" label="所属联赛" width="160" />
      <el-table-column prop="title" label="对战双方" />
      <el-table-column prop="location" label="地点" />
      <el-table-column prop="startTime" label="开始时间" width="180">
        <template #default="scope">
          {{ formatTime(scope.row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="matchResult" label="赛果" width="180">
        <template #default="scope">
          {{ scope.row.matchResult || '待比赛' }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteMatch(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px"
      @size-change="(size) => handleSizeChange(size, getMatches)"
      @current-change="(page) => handlePageChange(page, getMatches)"
    />

    <el-dialog :title="isEdit ? '编辑赛事' : '创建赛事'" v-model="dialogVisible" width="500px">
      <el-form :model="matchForm" :rules="rules" ref="matchFormRef" label-width="80px">
        <el-form-item label="联赛" prop="leagueId">
          <el-select v-model="matchForm.leagueId" placeholder="请选择联赛" style="width: 100%" @change="onLeagueChange">
            <el-option v-for="l in leagues" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主队" prop="homeTeamId">
          <el-select v-model="matchForm.homeTeamId" placeholder="选择主队俱乐部" style="width: 100%">
            <el-option v-for="c in availableClubs" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="客队" prop="awayTeamId">
          <el-select v-model="matchForm.awayTeamId" placeholder="选择客队俱乐部" style="width: 100%">
            <el-option v-for="c in availableClubs" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="matchForm.location" placeholder="请输入地点" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="matchForm.startTime" type="datetime" placeholder="选择日期时间" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="matchForm.status" placeholder="请选择状态">
            <el-option label="待比赛" value="REGISTERING" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="ENDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="比分" v-if="matchForm.status !== 'REGISTERING'">
          <div style="display: flex; align-items: center; gap: 8px; width: 100%;">
            <el-input-number v-model="matchForm.homeScore" :min="0" :max="99" />
            <span>:</span>
            <el-input-number v-model="matchForm.awayScore" :min="0" :max="99" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveMatch">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { useAdminPagination } from '@/composables/useAdminPagination'

const dialogVisible = ref(false)
const matchFormRef = ref(null)
const matches = ref([])
const clubs = ref([])
const leagues = ref([])
const leagueClubMap = ref({})
const isEdit = ref(false)
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

const matchForm = ref({
  id: null,
  title: '',
  leagueId: null,
  location: '',
  startTime: '',
  status: 'REGISTERING',
  homeTeamId: null,
  awayTeamId: null,
  homeScore: null,
  awayScore: null
})

const rules = {
  leagueId: [{ required: true, message: '请选择联赛', trigger: 'change' }],
  homeTeamId: [{ required: true, message: '请选择主队俱乐部', trigger: 'change' }],
  awayTeamId: [{ required: true, message: '请选择客队俱乐部', trigger: 'change' }],
  location: [{ required: true, message: '请输入地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const availableClubs = computed(() => {
  if (!matchForm.value.leagueId) return clubs.value
  return leagueClubMap.value[matchForm.value.leagueId] || []
})

const onLeagueChange = () => {
  matchForm.value.homeTeamId = null
  matchForm.value.awayTeamId = null
}

const openCreateDialog = () => {
  isEdit.value = false
  matchForm.value = {
    id: null,
    title: '',
    leagueId: null,
    location: '',
    startTime: '',
    status: 'REGISTERING',
    homeTeamId: null,
    awayTeamId: null,
    homeScore: null,
    awayScore: null
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  matchForm.value = {
    id: row.id,
    title: row.title,
    leagueId: row.leagueId || null,
    location: row.location,
    startTime: row.startTime,
    status: row.status || 'REGISTERING',
    homeTeamId: row.homeTeamId || null,
    awayTeamId: row.awayTeamId || null,
    homeScore: row.homeScore ?? null,
    awayScore: row.awayScore ?? null
  }
  dialogVisible.value = true
}

const saveMatch = async () => {
  if (!matchFormRef.value) return
  await matchFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const token = localStorage.getItem('token')

        // 统一由主队/客队拼出标题（后端也会兜底）
        const homeClub = clubs.value.find(c => c.id === matchForm.value.homeTeamId)
        const awayClub = clubs.value.find(c => c.id === matchForm.value.awayTeamId)
        if (!homeClub || !awayClub) {
          ElMessage.error('请选择有效的主队/客队')
          return
        }
        if (homeClub.id === awayClub.id) {
          ElMessage.error('主队和客队不能相同')
          return
        }
        matchForm.value.title = `${homeClub.name}VS${awayClub.name}`

        const req = isEdit.value
          ? axios.put(`/api/admin/match/${matchForm.value.id}`, matchForm.value, {
              headers: { Authorization: `Bearer ${token}` }
            })
          : axios.post('/api/admin/match', matchForm.value, {
              headers: { Authorization: `Bearer ${token}` }
            })
        const response = await req
        if (response.data.code === 200 && response.data.data) {
          ElMessage.success(isEdit.value ? '编辑赛事成功' : '创建赛事成功')
          dialogVisible.value = false
          getMatches()
        } else {
          ElMessage.error(response.data.message || (isEdit.value ? '编辑赛事失败' : '创建赛事失败'))
        }
      } catch (error) {
        console.error(isEdit.value ? '编辑赛事失败:' : '创建赛事失败:', error)
        ElMessage.error(error.response?.data?.message || (isEdit.value ? '编辑赛事失败' : '创建赛事失败'))
      }
    }
  })
}

const deleteMatch = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/admin/match/${id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    if (response.data.code === 200 && response.data.data) {
      ElMessage.success('删除赛事成功')
      await afterDeleteRefresh(getMatches)
    } else {
      ElMessage.error(response.data.message || '删除赛事失败')
    }
  } catch (error) {
    console.error('删除赛事失败:', error)
    ElMessage.error(error.response?.data?.message || '删除赛事失败')
  }
}

const getMatches = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/match/list', {
          headers: {
            Authorization: `Bearer ${token}`
      },
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        search: searchQuery.value
          }
        })
        if (response.data.code === 200) {
      const result = applyPageResult(response.data.data)
      matches.value = result.list
        }
      } catch (error) {
    console.error('获取赛事列表失败:', error)
    ElMessage.error('获取赛事列表失败')
      }
}

const searchMatches = () => {
  resetToFirstPage()
  getMatches()
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 获取状态类型（用于标签颜色）
const getStatusType = (status) => {
  const typeMap = {
    'REGISTERING': 'warning',
    'ONGOING': 'success',
    'ENDED': 'info'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    'REGISTERING': '待比赛',
    'ONGOING': '进行中',
    'ENDED': '已结束'
  }
  return textMap[status] || '未知'
}

// 初始化获取赛事列表
const loadLeagues = async () => {
  try {
    const response = await axios.get('/api/league/list')
    if (response.data.code === 200) {
      leagues.value = response.data.data || []
      for (const l of leagues.value) {
        const clubsRes = await axios.get(`/api/league/${l.id}/clubs`)
        if (clubsRes.data.code === 200) {
          leagueClubMap.value[l.id] = clubsRes.data.data || []
        } else {
          leagueClubMap.value[l.id] = []
        }
      }
    }
  } catch (e) {
    console.error('获取联赛列表失败:', e)
  }
}

const loadClubs = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/public/club/list', {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      clubs.value = response.data.data || []
    }
  } catch (e) {
    console.error('获取俱乐部列表失败:', e)
  }
}

loadLeagues().then(() => loadClubs()).then(() => getMatches())
</script>

<style scoped>
.match-manage {
  padding: 20px;
}

.match-manage h1 {
  margin-bottom: 20px;
  color: #333;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>