<template>
  <div class="comments">
    <h1>评论区</h1>
    <el-card>
      <div style="margin-bottom: 12px;">
        当前系统仅支持“每场比赛”评论交流，请先进入赛事详情页查看并发表评论。
      </div>
      <el-table :data="matches" style="width: 100%">
        <el-table-column prop="title" label="对战双方" />
        <el-table-column prop="startTime" label="开始时间" width="200">
          <template #default="scope">
            {{ formatDate(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button size="small" type="primary" @click="goMatch(scope.row.id)">去评论</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const matches = ref([])

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const getComments = async () => {
  try {
    const response = await axios.get('/api/match/list')
    if (response.data.code === 200) matches.value = response.data.data || []
  } catch (error) {
    console.error('获取赛事列表失败:', error)
    ElMessage.error('获取赛事列表失败')
  }
}

const goMatch = (id) => {
  router.push(`/fan/match/${id}`)
}

onMounted(() => {
  getComments()
})
</script>

<style scoped>
.comments {
  padding: 20px;
}

.comments h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 20px 0;
}

.comment-card {
  transition: transform 0.3s ease;
}

.comment-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: bold;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.match-info-small {
  font-size: 12px;
  color: #666;
  background-color: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
}

.comment-content {
  margin-bottom: 15px;
  line-height: 1.5;
}

.comment-footer {
  display: flex;
  gap: 20px;
}

@media (max-width: 768px) {
  .comments {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .comment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>