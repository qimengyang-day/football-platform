<template>
  <div class="team-selection">
    <h1>我的主队</h1>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选择主队</span>
        </div>
      </template>
      <div class="team-selector">
        <el-form :model="form" ref="formRef">
          <el-form-item label="选择俱乐部" prop="clubId">
            <el-select v-model="form.clubId" placeholder="请选择您支持的球队" class="w-full">
              <el-option
                v-for="club in clubs"
                :key="club.id"
                :label="club.name"
                :value="club.id"
              >
                <div class="club-option">
                  <div class="club-logo" v-if="club.logo">
                    <img :src="club.logo" alt="{{ club.name }}">
                  </div>
                  <div class="club-logo" v-else></div>
                  <span>{{ club.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="setMainTeam" :loading="loading">设为主队</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <div v-if="currentTeam" class="team-detail">
      <el-card style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>球队详情</span>
            <el-button type="danger" @click="removeMainTeam" :loading="loading">移除主队</el-button>
          </div>
        </template>
        <div class="team-header">
          <div class="team-basic-info">
            <div class="club-logo" v-if="currentTeam.logo">
              <img :src="currentTeam.logo" alt="{{ currentTeam.name }}">
            </div>
            <div class="club-logo" v-else></div>
            <div class="team-info">
              <h2>{{ currentTeam.name }}</h2>
              <div class="team-performance">
                <span class="rank">第{{ teamDetails.leagueRank || 0 }}名</span>
              </div>
            </div>
          </div>
        </div>
        <el-tabs v-model="activeTab" class="team-tabs">
          <el-tab-pane label="动态" name="news"></el-tab-pane>
          <el-tab-pane label="赛程" name="schedule">
            <div class="match-schedule">
              <div v-for="(monthMatches, month) in teamMatches" :key="month" class="month-section">
                <h3 class="month-title">{{ month }}</h3>
                <div v-for="match in monthMatches" :key="match.id" class="match-item">
                  <div class="match-time">{{ formatMatchTime(match.matchTime) }}</div>
                  <div class="match-teams">
                    <div class="team home-team">
                      <div class="team-name">{{ match.homeTeamName }}</div>
                      <div class="team-logo" v-if="match.homeTeamLogo">
                        <img :src="match.homeTeamLogo" alt="{{ match.homeTeamName }}">
                      </div>
                    </div>
                    <div class="match-score" v-if="match.status === 'COMPLETED'">
                      <span class="score">{{ match.homeScore }}-{{ match.awayScore }}</span>
                    </div>
                    <div class="match-score" v-else>
                      <span class="status">{{ match.status === 'UPCOMING' ? '未开始' : '进行中' }}</span>
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
          </el-tab-pane>
          <el-tab-pane label="数据" name="data">
            <div class="team-stats-detail">
              <h3>当前参加联赛</h3>
              <el-table :data="joinedLeagues" style="width: 100%">
                <el-table-column prop="leagueName" label="联赛" />
                <el-table-column prop="rank" label="当前排名" width="120" />
                <el-table-column prop="totalTeams" label="联赛球队数" width="120" />
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="俱乐部人员" name="players">
            <div class="players-list">
              <h3>当前俱乐部所有人员信息</h3>
              <el-table :data="allPersonnel" style="width: 100%">
                <el-table-column prop="name" label="姓名" />
                <el-table-column prop="roleType" label="类型" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.roleType === '球员' ? 'success' : 'warning'" size="small">
                      {{ scope.row.roleType }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="position" label="岗位/位置" width="160" />
                <el-table-column prop="age" label="年龄" width="90">
                  <template #default="scope">
                    {{ scope.row.age !== null && scope.row.age !== '-' ? scope.row.age : '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="nationality" label="国籍" width="120" />
                <el-table-column prop="marketValue" label="身价/薪资 (万欧)" width="150">
                  <template #default="scope">
                    {{ scope.row.marketValue !== null && scope.row.marketValue !== '-' ? scope.row.marketValue : '-' }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="资料" name="info">
            <div class="team-info-detail">
              <p><strong>主教练:</strong> {{ currentTeam.headCoach || '暂无' }}</p>
              <p><strong>翻译:</strong> {{ currentTeam.translator || '暂无' }}</p>
              <p><strong>赞助商:</strong> {{ currentTeam.sponsor || '暂无' }}</p>
              <p><strong>描述:</strong> {{ currentTeam.description || '暂无' }}</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const form = ref({ clubId: '' })
const formRef = ref(null)
const clubs = ref([])
const currentTeam = ref(null)
const loading = ref(false)
const activeTab = ref('schedule')
const allMatches = ref([])
const joinedLeagues = ref([])
const playerGroups = ref({
  前锋: [],
  中场: [],
  后卫: [],
  门将: [],
  其他: []
})
const allPersonnel = ref([])
const teamDetails = ref({
  leagueRank: 0,
  coaches: []
})

const leagueStandings = ref([])

const normalizeText = (v) => String(v ?? '').trim()

const parseMatchTitle = (title) => {
  const parts = normalizeText(title).split(/\s*vs\s*/i)
  if (parts.length >= 2) {
    return { homeTeamName: parts[0], awayTeamName: parts.slice(1).join(' vs ') }
  }
  return { homeTeamName: normalizeText(title), awayTeamName: '' }
}

const getRankings = async () => {}

const syncTeamRank = () => {
  if (!currentTeam.value) {
    teamDetails.value.leagueRank = 0
    return
  }
  const teamName = normalizeText(currentTeam.value.name)
  const hit = leagueStandings.value.find(item => normalizeText(item.teamName) === teamName)
  teamDetails.value.leagueRank = hit?.rank || 0
}

const loadTeamLeaguesAndRank = async () => {
  if (!currentTeam.value) {
    joinedLeagues.value = []
    return
  }
  try {
    const leagueRes = await axios.get('/api/league/list')
    if (leagueRes?.data?.code !== 200 || !Array.isArray(leagueRes.data.data)) {
      joinedLeagues.value = []
      return
    }
    const rows = []
    for (const league of leagueRes.data.data) {
      const standingsRes = await axios.get(`/api/league/${league.id}/standings`)
      if (standingsRes?.data?.code !== 200 || !Array.isArray(standingsRes.data.data)) continue
      const list = standingsRes.data.data
      const idx = list.findIndex(r => r.clubId === currentTeam.value.id)
      if (idx !== -1) {
        rows.push({
          leagueName: league.name,
          rank: list[idx]?.rank || (idx + 1),
          totalTeams: list.length
        })
      }
    }
    joinedLeagues.value = rows
    if (rows.length > 0) {
      teamDetails.value.leagueRank = rows[0].rank
    }
  } catch (e) {
    console.error('加载主队联赛与排名失败:', e)
    joinedLeagues.value = []
  }
}

const loadClubPersonnel = async () => {
  if (!currentTeam.value?.id) {
    teamDetails.value.coaches = []
    playerGroups.value = { '前锋': [], '中场': [], '后卫': [], '门将': [], '其他': [] }
    allPersonnel.value = []
    console.log('没有主队，清空人员列表')
    return
  }
  try {
    console.log('开始加载俱乐部人员，clubId:', currentTeam.value.id)
    const res = await axios.get(`/api/public/club/${currentTeam.value.id}/personnel`)
    console.log('俱乐部人员响应:', res.data)
    if (res?.data?.code !== 200 || !res.data.data) {
      console.error('俱乐部人员响应异常:', res.data)
      teamDetails.value.coaches = []
      allPersonnel.value = []
      return
    }
    teamDetails.value.coaches = res.data.data.coaches || []
    console.log('教练组数据:', teamDetails.value.coaches)
    playerGroups.value = {
      '前锋': res.data.data.groups?.前锋 || [],
      '中场': res.data.data.groups?.中场 || [],
      '后卫': res.data.data.groups?.后卫 || [],
      '门将': res.data.data.groups?.门将 || [],
      '其他': res.data.data.groups?.其他 || []
    }
    console.log('球员分组数据:', playerGroups.value)
    const coachRows = (teamDetails.value.coaches || []).map(c => ({
      name: c.name || '',
      roleType: '教练',
      position: c.position || '',
      age: c.age ?? '-',
      nationality: c.nationality || '-',
      marketValue: c.salary ?? '-'
    }))
    const playerRows = Object.values(playerGroups.value).flat().map(p => ({
      name: p.name || '',
      roleType: '球员',
      position: p.position || '',
      age: p.age ?? '-',
      nationality: p.nationality || '-',
      marketValue: p.marketValue ?? '-'
    }))
    allPersonnel.value = [...coachRows, ...playerRows]
    console.log('合并后的人员列表:', allPersonnel.value)
    console.log('人员总数:', allPersonnel.value.length)
  } catch (e) {
    console.error('加载俱乐部人员结构失败:', e)
    console.error('错误详情:', e.response?.data)
    teamDetails.value.coaches = []
    allPersonnel.value = []
  }
}

const getMatches = async () => {
  try {
    const res = await axios.get('/api/match/list')
    if (res?.data?.code === 200) {
      const list = Array.isArray(res.data.data) ? res.data.data : []
      allMatches.value = list.map((m) => {
        const parsed = parseMatchTitle(m.title)
        return {
          ...m,
          matchTime: m.startTime,
          homeTeamName: m.homeTeamName || parsed.homeTeamName,
          awayTeamName: m.awayTeamName || parsed.awayTeamName
        }
      })
      return
    }
    allMatches.value = []
  } catch (e) {
    console.error('获取赛程失败:', e)
    allMatches.value = []
  }
}

const getClubs = async () => {
  try {
    const res = await axios.get('/api/club/list')
    if (res.data.code === 200) {
      clubs.value = res.data.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

const getMyMainTeam = async () => {
  try {
    const res = await axios.get('/api/auth/info')
    if (res.data.code === 200) {
      const mainTeamId = res.data.data.mainTeamId
      if (mainTeamId) {
        currentTeam.value = clubs.value.find(c => c.id === mainTeamId) || null
        form.value.clubId = mainTeamId
        syncTeamRank()
        await loadTeamLeaguesAndRank()
        await loadClubPersonnel()
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const setMainTeam = async () => {
  if (!form.value.clubId) return
  loading.value = true
  try {
    const res = await axios.put('/api/user/profile', { mainTeamId: form.value.clubId })
    if (res.data.code === 200) {
      currentTeam.value = clubs.value.find(c => c.id === form.value.clubId) || null
      syncTeamRank()
      await loadTeamLeaguesAndRank()
      await loadClubPersonnel()
      ElMessage.success('主队设置成功')
    } else {
      ElMessage.error(res.data.message || '设置失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  } finally {
    loading.value = false
  }
}

const removeMainTeam = async () => {
  loading.value = true
  try {
    const res = await axios.put('/api/user/profile', { mainTeamId: 0 })
    if (res.data.code === 200) {
      currentTeam.value = null
      form.value.clubId = ''
      joinedLeagues.value = []
      playerGroups.value = { '前锋': [], '中场': [], '后卫': [], '门将': [], '其他': [] }
      allPersonnel.value = []
      ElMessage.success('已移除主队')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '移除失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await getClubs()
  await getMatches()
  await getMyMainTeam()
  syncTeamRank()
  await loadTeamLeaguesAndRank()
  await loadClubPersonnel()
})
const teamMatches = computed(() => {
  if (!currentTeam.value) return {}
  const teamName = normalizeText(currentTeam.value.name)
  const teamId = currentTeam.value.id
  const matches = (allMatches.value || []).filter((match) => {
    const byId = (match.homeTeamId === teamId) || (match.awayTeamId === teamId)
    const byName = normalizeText(match.homeTeamName) === teamName || normalizeText(match.awayTeamName) === teamName
    // 只展示当前主队参与的比赛
    return byId || byName
  })

  const grouped = {}
  matches.forEach(match => {
    const date = new Date(match.matchTime)
    const month = `${date.getFullYear()}年${date.getMonth() + 1}月`
    if (!grouped[month]) {
      grouped[month] = []
    }
    grouped[month].push(match)
  })
  return grouped
})

const formatMatchTime = (time) => {
  const date = new Date(time)
  const dayOfWeek = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes} ${dayOfWeek}`
}
</script>

<style scoped>
.team-selection {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.team-selection h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.team-selector {
  padding: 20px 0;
}

.club-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.club-logo {
  width: 40px;
  height: 40px;
  background-color: #e8e8e8;
  border-radius: 50%;
  overflow: hidden;
}

.club-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.team-header {
  margin-bottom: 20px;
}

.team-basic-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.team-basic-info .club-logo {
  width: 80px;
  height: 80px;
}

.team-info h2 {
  margin-bottom: 10px;
  color: #333;
}

.team-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.stat-item {
  font-size: 14px;
  color: #666;
}

.team-performance {
  display: flex;
  gap: 20px;
  align-items: center;
}

.rank {
  font-size: 16px;
  font-weight: bold;
  color: #faad14;
}

.record {
  font-size: 14px;
  color: #666;
}

.recent-form {
  font-size: 14px;
  color: #666;
}

.team-tabs {
  margin-top: 20px;
}

.match-schedule {
  padding: 20px 0;
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

.team .team-logo {
  width: 32px;
  height: 32px;
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

.team-stats-detail {
  padding: 20px 0;
}

.team-stats-detail h3 {
  margin-bottom: 15px;
  color: #333;
}

.players-list {
  padding: 20px 0;
}

.players-list h3 {
  margin-bottom: 15px;
  color: #333;
}

.coaches {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
  margin-bottom: 30px;
}

.coach-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.coach-avatar {
  width: 50px;
  height: 50px;
  background-color: #e8e8e8;
  border-radius: 50%;
}

.coach-info {
  flex: 1;
}

.coach-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 5px;
}

.coach-position {
  font-size: 14px;
  color: #666;
}

.team-info-detail {
  padding: 20px 0;
}

.team-info-detail p {
  margin: 10px 0;
  color: #666;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .team-selection {
    padding: 10px;
  }
  
  .team-basic-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .team-stats {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .team-performance {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
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
  
  .coaches {
    grid-template-columns: 1fr;
  }
}
</style>