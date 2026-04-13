<template>
  <div class="fans">
    <h1>球迷管理</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>球迷列表</span>
        </div>
      </template>
      <div class="search-bar">
        <el-input v-model="searchQuery" placeholder="搜索球迷昵称或用户名" style="width: 300px; margin-right: 10px" />
        <el-button type="primary" @click="searchFans">搜索</el-button>
      </div>
      <el-table :data="fans" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150"></el-table-column>
        <el-table-column prop="nickname" label="昵称"></el-table-column>
        <el-table-column prop="mainTeamName" label="主队" width="150"></el-table-column>
        <el-table-column prop="starLevel" label="星级" width="100">
          <template #default="scope">
            <el-rate v-model="scope.row.starLevel" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '活跃' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button size="small" type="primary" @click="viewFanDetail(scope.row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="(size) => handleSizeChange(size, getFans)"
        @current-change="(page) => handlePageChange(page, getFans)"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 球迷详情对话框 -->
    <el-dialog v-model="fanDetailDialogVisible" title="球迷详情" width="600px">
      <div v-if="fanDetail" class="fan-detail">
        <div class="detail-item">
          <span class="label">用户名:</span>
          <span class="value">{{ fanDetail.username }}</span>
        </div>
        <div class="detail-item">
          <span class="label">昵称:</span>
          <span class="value">{{ fanDetail.nickname }}</span>
        </div>
        <div class="detail-item">
          <span class="label">头像:</span>
          <el-avatar :size="60" :src="fanDetail.avatar || '/src/assets/images/default-avatar.svg'" />
        </div>
        <div class="detail-item">
          <span class="label">主队:</span>
          <span class="value">{{ fanDetail.mainTeamName || '未设置' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">星级:</span>
          <el-rate v-model="fanDetail.starLevel" disabled />
        </div>
        <div class="detail-item">
          <span class="label">注册时间:</span>
          <span class="value">{{ formatDate(fanDetail.createTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">最近登录:</span>
          <span class="value">{{ formatDate(fanDetail.lastLoginTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">评论数:</span>
          <span class="value">{{ fanDetail.commentCount || 0 }}</span>
        </div>
        <div class="detail-item">
          <span class="label">评分数:</span>
          <span class="value">{{ fanDetail.scoreCount || 0 }}</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="fanDetailDialogVisible = false">关闭</el-button>
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

const fans = ref([])
const searchQuery = ref('')
const {
  pageNum,
  pageSize,
  total,
  resetToFirstPage,
  applyPageResult,
  handlePageChange,
  handleSizeChange
} = useAdminPagination()

const fanDetailDialogVisible = ref(false)
const fanDetail = ref(null)

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const getFans = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/admin/fans', {
      headers: { Authorization: `Bearer ${token}` },
      params: {
        search: searchQuery.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (response.data.code === 200) {
      const result = applyPageResult(response.data.data)
      fans.value = result.list
    }
  } catch (error) {
    console.error('获取球迷列表失败:', error)
    ElMessage.error('获取球迷列表失败')
  }
}

const searchFans = () => {
  resetToFirstPage()
  getFans()
}

const toggleStatus = async (fan) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.put(`/api/admin/fan/status/${fan.id}`, {
      status: fan.status === 1 ? 0 : 1
    }, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success(fan.status === 1 ? '禁用成功' : '启用成功')
      fan.status = fan.status === 1 ? 0 : 1
    }
  } catch (error) {
    console.error('切换状态失败:', error)
    ElMessage.error(error.response?.data?.message || '切换状态失败')
  }
}

const viewFanDetail = async (fanId) => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/admin/fan/detail/${fanId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (response.data.code === 200) {
      fanDetail.value = response.data.data
      fanDetailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取球迷详情失败:', error)
    ElMessage.error('获取球迷详情失败')
  }
}

onMounted(() => {
  getFans()
})
</script>

<style scoped>
.fans {
  padding: 20px;
}

.fans h1 {
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

.fan-detail {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 20px 0;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 20px;
}

.label {
  width: 100px;
  font-weight: bold;
  color: #666;
}

.value {
  flex: 1;
  color: #333;
}

@media (max-width: 768px) {
  .fans {
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
  
  .detail-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .label {
    width: auto;
  }
}
</style>