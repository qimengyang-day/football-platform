<template>
  <div class="players">
    <h1>球员管理</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>球员列表</span>
          <el-button type="primary" @click="addPlayer">添加球员</el-button>
        </div>
      </template>
      <div class="search-bar">
        <el-input v-model="searchQuery" placeholder="搜索球员姓名" style="width: 200px; margin-right: 10px" />
        <el-select v-model="clubFilter" placeholder="按俱乐部筛选">
          <el-option label="全部" value=""></el-option>
          <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id"></el-option>
        </el-select>
        <el-button type="primary" @click="searchPlayers">搜索</el-button>
      </div>
      <el-table :data="players" style="width: 100%">
        <el-table-column prop="realName" label="姓名"></el-table-column>
        <el-table-column prop="username" label="用户名" width="150"></el-table-column>
        <el-table-column prop="position" label="位置" width="100"></el-table-column>
        <el-table-column prop="teamName" label="所属俱乐部"></el-table-column>
        <el-table-column prop="marketValue" label="身价(万元)" width="120"></el-table-column>
        <el-table-column prop="goals" label="进球" width="80"></el-table-column>
        <el-table-column prop="assists" label="助攻" width="80"></el-table-column>
        <el-table-column prop="isFreeAgent" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.isFreeAgent === 1 ? 'warning' : 'success'">
              {{ scope.row.isFreeAgent === 1 ? '自由身' : '已签约' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="editPlayer(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deletePlayer(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="(size) => handleSizeChange(size, getPlayers)"
        @current-change="(page) => handlePageChange(page, getPlayers)"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 添加/编辑球员对话框 -->
    <el-dialog v-model="addPlayerDialogVisible" :title="isEdit ? '编辑球员' : '添加球员'" width="500px">
      <el-form :model="playerForm" :rules="rules" ref="playerFormRef" label-width="100px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="playerForm.realName" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="playerForm.username" placeholder="系统登录用户名" />
        </el-form-item>
        <el-form-item label="身高 (cm)" prop="height">
          <el-input v-model.number="playerForm.height" type="number" />
        </el-form-item>
        <el-form-item label="体重 (kg)" prop="weight">
          <el-input v-model.number="playerForm.weight" type="number" />
        </el-form-item>
        <el-form-item label="位置" prop="position">
          <el-select v-model="playerForm.position" placeholder="请选择位置">
            <el-option label="前锋" value="前锋" />
            <el-option label="中场" value="中场" />
            <el-option label="后卫" value="后卫" />
            <el-option label="门将" value="门将" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属俱乐部" prop="teamId">
          <el-select v-model="playerForm.teamId" placeholder="请选择俱乐部">
            <el-option label="自由身" value="0" />
            <el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="身价 (万元)" prop="marketValue">
          <el-input v-model.number="playerForm.marketValue" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addPlayerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePlayer">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { useAdminPagination } from '@/composables/useAdminPagination'

const players = ref([])
const clubs = ref([])
const searchQuery = ref('')
const clubFilter = ref('')
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

const addPlayerDialogVisible = ref(false)
const isEdit = ref(false)
const playerFormRef = ref(null)
const playerForm = ref({
  realName: '',
  username: '',
  height: '',
  weight: '',
  position: '',
  teamId: 0,
  marketValue: 0,
  userId: 0,
  isFreeAgent: 1
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  height: [{ required: true, message: '请输入身高', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入体重', trigger: 'blur' }],
  position: [{ required: true, message: '请选择位置', trigger: 'change' }],
  marketValue: [{ required: true, message: '请输入身价', trigger: 'blur' }]
}

const getClubs = async () => {
  try {
    const response = await axios.get('/api/public/club/list')
    if (response.data.code === 200) {
      clubs.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取俱乐部列表失败:', error)
  }
}

const mapFromPage = (page = {}) => {
  const records = page.records || []
  return {
    list: records.map((item) => {
      const teamId = item.teamId || 0
      const club = teamId ? clubs.value.find((c) => Number(c.id) === Number(teamId)) : null
      return {
        id: item.id,
        userId: item.userId,
        realName: item.realName || '',
        username: item.username || '',
        height: item.height,
        weight: item.weight,
        position: item.position,
        teamId: teamId,
        teamName: club ? club.name : '',
        marketValue: item.marketValue,
        goals: item.goals,
        assists: item.assists,
        isFreeAgent: item.isFreeAgent ?? (teamId ? 0 : 1)
      }
    }),
    total: Number(page.total || 0)
  }
}

const getPlayers = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/players', {
      headers: { Authorization: `Bearer ${token}` },
      params: {
        search: searchQuery.value || undefined,
        clubId: clubFilter.value ? Number(clubFilter.value) : undefined,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (response.data.code === 200) {
      const result = applyPageResult(response.data.data)
      players.value = result.list
      return
    }
    ElMessage.error(response.data.message || '获取球员列表失败')
    const fallback = await axios.get('/api/admin/player/list', {
      headers: { Authorization: `Bearer ${token}` },
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (fallback.data.code === 200) {
      const mapped = mapFromPage(fallback.data.data)
      players.value = mapped.list
      total.value = mapped.total
    }
  } catch (error) {
    console.error('获取球员列表失败:', error)
    ElMessage.error('获取球员列表失败')
  }
}

const searchPlayers = () => {
  resetToFirstPage()
  getPlayers()
}

const addPlayer = () => {
  isEdit.value = false
  playerForm.value = {
    realName: '',
    username: '',
    height: '',
    weight: '',
    position: '',
    teamId: 0,
    marketValue: 0,
    userId: 0, // 临时用户 ID，后续可以从登录信息中获取
    isFreeAgent: 1 // 默认为自由身
  }
  addPlayerDialogVisible.value = true
}

const editPlayer = (player) => {
  isEdit.value = true
  playerForm.value = {
    id: player.id,
    realName: player.realName,
    username: player.username || '',
    height: player.height,
    weight: player.weight,
    position: player.position,
    teamId: player.teamId || 0,
    marketValue: player.marketValue,
    userId: player.userId || 0,
    isFreeAgent: player.isFreeAgent || 1
  }
  addPlayerDialogVisible.value = true
}

const savePlayer = async () => {
  if (!playerFormRef.value) return
  await playerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const token = localStorage.getItem('token')
        let response
        if (isEdit.value) {
          response = await axios.put('/api/admin/player/update', playerForm.value, {
            headers: { Authorization: `Bearer ${token}` }
          })
        } else {
          response = await axios.post('/api/admin/player/add', playerForm.value, {
            headers: { Authorization: `Bearer ${token}` }
          })
        }
        if (response.data.code === 200) {
          ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
          addPlayerDialogVisible.value = false
          getPlayers()
        }
      } catch (error) {
        console.error(isEdit.value ? '编辑失败:' : '添加失败:', error)
        ElMessage.error(error.response?.data?.message || (isEdit.value ? '编辑失败' : '添加失败'))
      }
    }
  })
}

const deletePlayer = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/admin/player/delete/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      await afterDeleteRefresh(getPlayers)
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  getClubs()
  getPlayers()
})
</script>

<style scoped>
.players {
  padding: 20px;
}

.players h1 {
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

@media (max-width: 768px) {
  .players {
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
}
</style>