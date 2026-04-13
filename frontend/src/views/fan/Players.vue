<template>
  <div class="fan-players">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>球员列表</span>
          <el-input
            v-model="searchQuery"
            placeholder="搜索球员"
            style="width: 200px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>
      <el-table :data="players" style="width: 100%">
        <el-table-column prop="id" label="球员ID" width="80" />
        <el-table-column prop="name" label="球员姓名" />
        <el-table-column prop="clubName" label="所属俱乐部" />
        <el-table-column prop="position" label="位置" width="100" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="nationality" label="国籍" />
        <el-table-column prop="marketValue" label="身价" width="120">
          <template #default="scope">
            {{ scope.row.marketValue }} 万欧元
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

const players = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')

const fetchPlayers = async () => {
  try {
    const response = await axios.get('/api/fan/players', {
      params: {
        page: currentPage.value,
        pageSize: pageSize.value,
        search: searchQuery.value
      }
    })
    if (response.data.code === 200) {
      players.value = response.data.data.records || []
      total.value = response.data.data.total || 0
    } else {
      ElMessage.error(response.data.message || '获取球员列表失败')
    }
  } catch (error) {
    ElMessage.error('获取球员列表失败')
    console.error('获取球员列表失败:', error)
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  fetchPlayers()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  fetchPlayers()
}

onMounted(() => {
  fetchPlayers()
})
</script>

<style scoped>
.fan-players {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .fan-players {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .card-header .el-input {
    width: 100%;
  }
  
  .pagination-container {
    justify-content: center;
  }
}
</style>