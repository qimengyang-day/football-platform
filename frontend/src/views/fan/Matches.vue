<template>
  <div class="fan-matches">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>赛事列表</span>
          <div class="season-selector">
            <el-button @click="previousSeason">上赛季</el-button>
            <el-select v-model="currentSeason" @change="fetchMatches" style="width: 100px">
              <el-option label="2026" value="2026" />
              <el-option label="2025" value="2025" />
              <el-option label="2024" value="2024" />
            </el-select>
            <el-button @click="nextSeason">下赛季</el-button>
          </div>
        </div>
      </template>
      <div class="match-schedule">
        <div v-for="(monthMatches, month) in groupedMatches" :key="month" class="month-section">
          <h3 class="month-title">{{ month }}</h3>
          <div v-for="match in monthMatches" :key="match.id" class="match-item" @click="viewMatch(match.id)">
            <div class="match-time">{{ formatMatchTime(match.matchTime) }}</div>
            <div class="match-teams">
              <div class="team home-team">
                <div class="team-name">{{ match.homeTeamName }}</div>
                <div class="team-logo" v-if="match.homeTeamLogo">
                  <img :src="match.homeTeamLogo" alt="{{ match.homeTeamName }}">
                </div>
              </div>
              <div class="match-score" v-if="match.status === 'ENDED'">
                <span class="score">{{ match.homeScore }}-{{ match.awayScore }}</span>
              </div>
              <div class="match-score" v-else>
                <span class="status">{{ match.status === 'REGISTERING' ? '未开始' : '进行中' }}</span>
              </div>
              <div class="team away-team">
                <div class="team-logo" v-if="match.awayTeamLogo">
                  <img :src="match.awayTeamLogo" alt="{{ match.awayTeamName }}">
                </div>
                <div class="team-name">{{ match.awayTeamName }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from '@/utils/axios'

const matches = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const currentSeason = ref('2026')

const parseTitleTeams = (title) => {
  if (!title) return { homeTeamName: '', awayTeamName: '' }
  // 与后端 title 格式一致：支持 "A VS B" / "AvsB" / "A vs B"
  const parts = String(title).split(/\s*vs\s*/i)
  if (parts.length >= 2) {
    return { homeTeamName: parts[0].trim(), awayTeamName: parts.slice(1).join(' vs ').trim() }
  }
  return { homeTeamName: String(title), awayTeamName: '' }
}

const groupedMatches = computed(() => {
  const grouped = {}
  matches.value.forEach(match => {
    const date = new Date(match.matchTime)
    const month = `${date.getFullYear()}年${date.getMonth() + 1}月`
    if (!grouped[month]) {
      grouped[month] = []
    }
    grouped[month].push(match)
  })
  return grouped
})

const fetchMatches = async () => {
  try {
    // 后端当前已提供统一的公开赛事列表接口：/api/match/list
    // 这里用现有接口完成前端分页与赛季过滤，避免调用不存在的 /api/fan/matches。
    const response = await axios.get('/api/match/list')
    if (response?.data?.code !== 200) {
      ElMessage.error(response?.data?.message || '获取赛事列表失败')
      return
    }

    const all = response.data.data || []
    const seasonYear = Number(currentSeason.value)

    // 过滤当前赛季（使用 startTime 的年份）
    const filtered = all.filter(m => {
      if (!m?.startTime) return false
      const y = new Date(m.startTime).getFullYear()
      return y === seasonYear
    })

    // 显式补齐前端渲染需要的字段（matchTime / homeTeamName / awayTeamName / 分数）
    const normalized = filtered.map(m => {
      const { homeTeamName, awayTeamName } =
        m.homeTeamName && m.awayTeamName
          ? { homeTeamName: m.homeTeamName, awayTeamName: m.awayTeamName }
          : parseTitleTeams(m.title)

      return {
        ...m,
        matchTime: m.startTime,
        homeTeamName,
        awayTeamName,
        homeScore: m.homeScore ?? 0,
        awayScore: m.awayScore ?? 0,
        // 后端已统一输出 REGISTERING/ONGOING/ENDED，这里兜底兼容历史中文
        status: m.status === '已结束' ? 'ENDED' : m.status === '进行中' ? 'ONGOING' : m.status === '报名中' ? 'REGISTERING' : (m.status || 'REGISTERING')
      }
    })

    total.value = normalized.length
    const start = (currentPage.value - 1) * pageSize.value
    matches.value = normalized.slice(start, start + pageSize.value)
  } catch (error) {
    ElMessage.error('获取赛事列表失败')
    console.error('获取赛事列表失败:', error)
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchMatches()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  fetchMatches()
}

const viewMatch = (id) => {
  window.location.href = `#/fan/match/${id}`
}

const previousSeason = () => {
  const year = parseInt(currentSeason.value)
  if (year > 2024) {
    currentSeason.value = (year - 1).toString()
    fetchMatches()
  }
}

const nextSeason = () => {
  const year = parseInt(currentSeason.value)
  if (year < 2026) {
    currentSeason.value = (year + 1).toString()
    fetchMatches()
  }
}

const formatMatchTime = (time) => {
  const date = new Date(time)
  const dayOfWeek = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes} ${dayOfWeek}`
}

onMounted(() => {
  fetchMatches()
})
</script>

<style scoped>
.fan-matches {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.season-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.match-schedule {
  margin-top: 20px;
}

.month-section {
  margin-bottom: 30px;
}

.month-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #333;
  border-left: 4px solid #faad14;
  padding-left: 10px;
}

.match-item {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.match-item:hover {
  background-color: #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.match-time {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.match-teams {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.team {
  display: flex;
  align-items: center;
  gap: 10px;
}

.team-name {
  font-size: 16px;
  font-weight: 500;
}

.team-logo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
}

.team-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-team {
  flex: 1;
  justify-content: flex-start;
}

.away-team {
  flex: 1;
  justify-content: flex-end;
}

.match-score {
  font-size: 20px;
  font-weight: bold;
  padding: 0 20px;
  text-align: center;
}

.score {
  color: #faad14;
}

.status {
  color: #1890ff;
  font-size: 14px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .fan-matches {
    padding: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .season-selector {
    width: 100%;
    justify-content: space-between;
  }
  
  .season-selector .el-select {
    flex: 1;
  }
  
  .match-teams {
    flex-direction: column;
    gap: 10px;
  }
  
  .team {
    width: 100%;
    justify-content: space-between;
  }
  
  .match-score {
    padding: 10px 0;
  }
}
</style>