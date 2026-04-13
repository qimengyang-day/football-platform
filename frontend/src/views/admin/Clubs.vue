<template>
  <div class="clubs">
    <h1>俱乐部管理</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>俱乐部列表</span>
          <el-button type="primary" @click="addClub">添加俱乐部</el-button>
        </div>
      </template>
      <div class="search-bar">
        <el-input v-model="searchQuery" placeholder="搜索俱乐部名称" style="width: 200px; margin-right: 10px" />
        <el-button type="primary" @click="searchClubs">搜索</el-button>
      </div>
      <el-table :data="clubs" style="width: 100%">
        <el-table-column prop="name" label="俱乐部名称"></el-table-column>
        <el-table-column prop="headCoach" label="主教练" width="150"></el-table-column>
        <el-table-column prop="translator" label="翻译" width="120"></el-table-column>
        <el-table-column prop="sponsor" label="赞助商" width="150"></el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="editClub(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteClub(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="(size) => handleSizeChange(size, getClubs)"
        @current-change="(page) => handlePageChange(page, getClubs)"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 添加/编辑俱乐部对话框 -->
    <el-dialog v-model="addClubDialogVisible" :title="isEdit ? '编辑俱乐部' : '添加俱乐部'" width="600px">
      <el-form :model="clubForm" :rules="rules" ref="clubFormRef" label-width="100px">
        <el-form-item label="俱乐部名称" prop="name">
          <el-input v-model="clubForm.name" />
        </el-form-item>
        <el-form-item label="主教练" prop="headCoach">
          <el-input v-model="clubForm.headCoach" />
        </el-form-item>
        <el-form-item label="翻译" prop="translator">
          <el-input v-model="clubForm.translator" />
        </el-form-item>
        <el-form-item label="赞助商" prop="sponsor">
          <el-input v-model="clubForm.sponsor" />
        </el-form-item>
        <el-form-item label="所属联赛" prop="leagueIds">
          <el-select v-model="clubForm.leagueIds" multiple placeholder="请选择联赛">
            <el-option v-for="league in leagues" :key="league.id" :label="league.name" :value="league.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="clubForm.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addClubDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveClub">保存</el-button>
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

const clubs = ref([])
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

const addClubDialogVisible = ref(false)
const isEdit = ref(false)
const clubFormRef = ref(null)
const clubForm = ref({
  name: '',
  headCoach: '',
  translator: '',
  sponsor: '',
  leagueIds: [],
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入俱乐部名称', trigger: 'blur' }]
}

const getLeagues = async () => {
  try {
    const response = await axios.get('/api/public/league/list')
    if (response.data.code === 200) {
      leagues.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取联赛列表失败:', error)
  }
}

const getClubs = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/clubs', {
      headers: { Authorization: `Bearer ${token}` },
      params: {
        search: searchQuery.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (response.data.code === 200) {
      const result = applyPageResult(response.data.data)
      clubs.value = result.list
    }
  } catch (error) {
    console.error('获取俱乐部列表失败:', error)
    ElMessage.error('获取俱乐部列表失败')
  }
}

const searchClubs = () => {
  resetToFirstPage()
  getClubs()
}

const addClub = () => {
  isEdit.value = false
  clubForm.value = {
    name: '',
    headCoach: '',
    translator: '',
    sponsor: '',
    leagueIds: [],
    description: ''
  }
  addClubDialogVisible.value = true
}

const editClub = (club) => {
  isEdit.value = true
  clubForm.value = {
    id: club.id,
    name: club.name,
    headCoach: club.headCoach,
    translator: club.translator,
    sponsor: club.sponsor,
    leagueIds: club.leagueIds || [],
    description: club.description
  }
  addClubDialogVisible.value = true
}

const saveClub = async () => {
  if (!clubFormRef.value) return
  await clubFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const token = localStorage.getItem('token')
        if (!token) {
          ElMessage.error('登录状态已失效，请重新登录后再试')
          return
        }
        const payload = {
          name: clubForm.value.name,
          headCoach: clubForm.value.headCoach,
          translator: clubForm.value.translator,
          sponsor: clubForm.value.sponsor,
          description: clubForm.value.description
        }
        // 只有当 managerId 有值时才添加到 payload 中
        if (clubForm.value.managerId) {
          payload.managerId = clubForm.value.managerId
        }
        let response
        if (isEdit.value) {
          // 更新俱乐部基本信息时需要传递 id
          payload.id = clubForm.value.id
          // 更新俱乐部基本信息
          response = await axios.put('/api/admin/club/update', payload, {
            headers: { Authorization: `Bearer ${token}` }
          })
          // 更新联赛关联：先删除旧的，再添加新的
          if (response.data.code === 200) {
            // 删除所有旧的联赛关联
            await axios.delete(`/api/admin/club/leagues/${clubForm.value.id}`, {
              headers: { Authorization: `Bearer ${token}` }
            })
            // 添加新的联赛关联
            if (clubForm.value.leagueIds && clubForm.value.leagueIds.length > 0) {
              for (const leagueId of clubForm.value.leagueIds) {
                await axios.post('/api/admin/league/club/add', null, {
                  headers: { Authorization: `Bearer ${token}` },
                  params: { clubId: clubForm.value.id, leagueId: leagueId }
                })
              }
            }
          }
        } else {
          response = await axios.post('/api/admin/club/add', payload, {
            headers: { Authorization: `Bearer ${token}` }
          })
          if (response.data.code !== 200) {
            ElMessage.error(response.data.message || '添加失败')
            return
          }

          // 添加联赛关联（仅在拿到有效 clubId 后执行）
          let clubId = response.data.data
          if (!clubId) {
            // 兜底：若后端未回传 id，则根据名称回查最新列表
            const queryResp = await axios.get('/api/admin/clubs', {
              headers: { Authorization: `Bearer ${token}` },
              params: { search: clubForm.value.name, pageNum: 1, pageSize: 1 }
            })
            const first = queryResp?.data?.data?.list?.[0]
            clubId = first?.id
          }

          if (!clubId) {
            ElMessage.error('俱乐部已创建，但未获取到俱乐部ID，联赛绑定未执行')
            addClubDialogVisible.value = false
            getClubs()
            return
          }

          if (clubForm.value.leagueIds && clubForm.value.leagueIds.length > 0) {
            for (const leagueId of clubForm.value.leagueIds) {
              const bindResp = await axios.post('/api/admin/league/club/add', null, {
                headers: { Authorization: `Bearer ${token}` },
                params: { clubId, leagueId }
              })
              if (bindResp?.data?.code !== 200 || bindResp?.data?.data === false) {
                throw new Error(bindResp?.data?.message || `俱乐部绑定联赛失败（leagueId=${leagueId}）`)
              }
            }
          }
        }
        if (response.data.code === 200 && response.data.data !== false) {
          ElMessage.success(isEdit.value ? '编辑成功' : '添加成功')
          addClubDialogVisible.value = false
          getClubs()
        } else {
          ElMessage.error(response.data.message || (isEdit.value ? '编辑失败' : '添加失败'))
        }
      } catch (error) {
        console.error(isEdit.value ? '编辑失败:' : '添加失败:', error)
        ElMessage.error(error.response?.data?.message || (isEdit.value ? '编辑失败' : '添加失败'))
      }
    }
  })
}

const deleteClub = async (id) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.delete(`/api/admin/club/delete/${id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('删除成功')
      await afterDeleteRefresh(getClubs)
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  getLeagues()
  getClubs()
})
</script>

<style scoped>
.clubs {
  padding: 20px;
}

.clubs h1 {
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
  .clubs {
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