<template>
  <div class="club">
    <h1>我的俱乐部</h1>
    
    <!-- 只有当球员已加入俱乐部时才显示俱乐部信息（支持"已签约"、"俱乐部成员"等状态） -->
    <el-card v-if="hasJoinedClub && currentClub">
      <template #header>
        <div class="card-header">
          <span>当前所属俱乐部</span>
        </div>
      </template>
      <div class="club-info">
        <div class="club-logo"></div>
        <div class="club-details">
          <h3>{{ currentClub.name }}</h3>
          <p>主教练: {{ currentClub.headCoach || '暂无' }}</p>
          <p>翻译: {{ currentClub.translator || '暂无' }}</p>
          <p>赞助商: {{ currentClub.sponsor || '暂无' }}</p>
          <p>总身价: {{ totalValue }}（含主教练与全队）</p>
        </div>
      </div>
      <el-divider />
      <div class="club-members">
        <h3>教练组</h3>
        <div class="member-row">
          <div class="avatar"></div>
          <div class="meta">
            <div class="name">{{ coach.name || currentClub.headCoach || '暂无' }}</div>
            <div class="sub">主教练</div>
          </div>
          <div class="stat">{{ coachValue }}</div>
        </div>

        <h3 style="margin-top: 16px">球员</h3>
        <el-collapse>
          <el-collapse-item title="前锋" name="fw">
            <el-table :data="groups.FW" size="small" style="width: 100%">
              <el-table-column prop="name" label="球员" />
              <el-table-column prop="age" label="年龄" width="80" />
              <el-table-column prop="goals" label="进球" width="80" />
              <el-table-column prop="assists" label="助攻" width="80" />
              <el-table-column prop="marketValue" label="身价" width="100" />
            </el-table>
          </el-collapse-item>
          <el-collapse-item title="中场" name="mf">
            <el-table :data="groups.MF" size="small" style="width: 100%">
              <el-table-column prop="name" label="球员" />
              <el-table-column prop="age" label="年龄" width="80" />
              <el-table-column prop="goals" label="进球" width="80" />
              <el-table-column prop="assists" label="助攻" width="80" />
              <el-table-column prop="marketValue" label="身价" width="100" />
            </el-table>
          </el-collapse-item>
          <el-collapse-item title="后卫" name="df">
            <el-table :data="groups.DF" size="small" style="width: 100%">
              <el-table-column prop="name" label="球员" />
              <el-table-column prop="age" label="年龄" width="80" />
              <el-table-column prop="goals" label="进球" width="80" />
              <el-table-column prop="assists" label="助攻" width="80" />
              <el-table-column prop="marketValue" label="身价" width="100" />
            </el-table>
          </el-collapse-item>
          <el-collapse-item title="门将" name="gk">
            <el-table :data="groups.GK" size="small" style="width: 100%">
              <el-table-column prop="name" label="球员" />
              <el-table-column prop="age" label="年龄" width="80" />
              <el-table-column prop="goals" label="进球" width="80" />
              <el-table-column prop="assists" label="助攻" width="80" />
              <el-table-column prop="marketValue" label="身价" width="100" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>

    <!-- 联赛积分榜 -->
    <el-card v-if="hasJoinedClub && currentClub" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>联赛积分榜</span>
          <el-tag v-if="currentLeagueName" type="success" size="small">{{ currentLeagueName }}</el-tag>
        </div>
      </template>
      <div v-if="leagueStandings.length > 0">
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
      </div>
      <div v-else style="padding: 20px; text-align: center; color: #999;">
        <p>暂无联赛积分榜信息</p>
      </div>
    </el-card>

    <!-- 俱乐部赛程信息 -->
    <el-card v-if="hasJoinedClub && currentClub" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>俱乐部赛程</span>
        </div>
      </template>
      <div v-if="Object.keys(teamMatches).length > 0">
        <div v-for="(matches, month) in teamMatches" :key="month" class="month-section">
          <h3 class="month-title">{{ month }}</h3>
          <div v-for="match in matches" :key="match.id" class="match-item">
            <div class="match-time">{{ formatMatchTime(match.matchTime) }}</div>
            <div class="match-teams">
              <div class="team home-team">
                <div class="team-logo"></div>
                <span class="team-name">{{ match.homeTeamName }}</span>
              </div>
              <div class="match-score">
                <span v-if="match.status === 'COMPLETED' || match.status === 'ONGOING'" class="score">
                  {{ match.homeScore }} : {{ match.awayScore }}
                </span>
                <span v-else class="status">VS</span>
              </div>
              <div class="team away-team">
                <span class="team-name">{{ match.awayTeamName }}</span>
                <div class="team-logo"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else style="padding: 20px; text-align: center; color: #999;">
        <p>暂无赛程信息</p>
      </div>
    </el-card>

    <!-- 当球员不是俱乐部成员时显示提示 -->
    <el-card v-else>
      <template #header>
        <div class="card-header">
          <span>等待加入俱乐部</span>
        </div>
      </template>
      <div style="padding: 40px; text-align: center; color: #999;">
        <p style="font-size: 16px; margin-bottom: 10px;">您目前是{{ playerStatus || '未知' }}状态</p>
        <p v-if="playerStatus && playerStatus !== '自由身'" style="font-size: 14px; color: #e6a23c; margin-top: 10px;">⚠️ 状态显示为"{{ playerStatus }}"，但未获取到所属俱乐部信息</p>
        <p v-if="playerStatus && playerStatus !== '自由身'" style="font-size: 13px; color: #f56c6c; margin-top: 5px;">可能原因：数据库中该球员的 teamId 字段为空</p>
        <p style="font-size: 14px; margin-top: 15px;">请联系管理员检查并设置您的所属俱乐部</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const currentClub = ref(null)
const clubs = ref([])
const applicationStatus = ref(null)
const form = ref({ clubId: '', reason: '' })
const formRef = ref(null)
const loading = ref(false)
const coach = ref({})
const coachValue = ref(0)
const totalValue = ref(0)
const groups = ref({ FW: [], MF: [], DF: [], GK: [], OTHER: [] })
const playerStatus = ref('') // 球员状态
const playerTeamId = ref(null) // 球员所属俱乐部ID
const leagueStandings = ref([]) // 联赛排名
const currentLeagueName = ref('') // 当前联赛名称
const allMatches = ref([]) // 所有比赛

// 计算属性：当前俱乐部的排名
const currentClubRank = computed(() => {
  if (!currentClub.value || leagueStandings.value.length === 0) return '-'
  const currentStanding = leagueStandings.value.find(s => 
    s.clubId === currentClub.value.id || s.teamName === currentClub.value.name
  )
  return currentStanding ? currentStanding.rank : '-'
})

// 计算属性：判断球员是否已加入俱乐部
// 支持多种状态：已签约、俱乐部成员等（只要不是"自由身"或空值）
const hasJoinedClub = computed(() => {
  if (playerTeamId.value) return true
  const status = playerStatus.value || ''
  return status !== '' && status !== '自由身' && status !== '自由球员'
})

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatMatchTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const dayOfWeek = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes} ${dayOfWeek}`
}

// 计算属性：过滤出当前俱乐部的比赛并按月份分组
const teamMatches = computed(() => {
  if (!currentClub.value) return {}
  const clubId = currentClub.value.id
  const clubName = currentClub.value.name
  
  const matches = (allMatches.value || []).filter((match) => {
    // 通过ID或名称匹配
    return (match.homeTeamId === clubId || match.awayTeamId === clubId) ||
           (match.homeTeamName === clubName || match.awayTeamName === clubName)
  })

  const grouped = {}
  matches.forEach(match => {
    const date = new Date(match.matchTime || match.startTime)
    if (isNaN(date.getTime())) return
    
    const month = `${date.getFullYear()}年${date.getMonth() + 1}月`
    if (!grouped[month]) {
      grouped[month] = []
    }
    
    // 标准化比赛状态
    let status = match.status
    if (status === 'ENDED' || status === '已结束') status = 'COMPLETED'
    else if (status === 'ONGOING' || status === '进行中') status = 'ONGOING'
    else status = 'UPCOMING'
    
    grouped[month].push({
      id: match.id,
      homeTeamName: match.homeTeamName || '',
      awayTeamName: match.awayTeamName || '',
      matchTime: match.matchTime || match.startTime,
      status: status,
      homeScore: match.homeScore ?? 0,
      awayScore: match.awayScore ?? 0
    })
  })
  
  return grouped
})

const tableRowClassName = ({ row }) => {
  if (currentClub.value && (row.clubId === currentClub.value.id || row.teamName === currentClub.value.name)) {
    return 'current-club-row'
  }
  return ''
}

const getCurrentClub = async () => {
  try {
    if (!playerTeamId.value) {
      currentClub.value = null
      return
    }
    const response = await axios.get('/api/player/club/current')
    console.log('获取俱乐部API响应:', response.data)
    if (response.data.code === 200 && response.data.data) {
      currentClub.value = response.data.data
      console.log('currentClub设置为:', currentClub.value)
    } else {
      console.warn('俱乐部API返回null，可能teamId为空')
    }
  } catch (error) {
    console.error('获取当前俱乐部失败:', error)
  }
}

const getPlayerStatus = async () => {
  try {
    const response = await axios.get('/api/player/me')
    if (response.data.code === 200) {
      playerStatus.value = response.data.data.status || ''
      playerTeamId.value = response.data.data.teamId || null
      console.log('球员状态:', playerStatus.value)
    }
  } catch (error) {
    console.error('获取球员状态失败:', error)
  }
}

const getClubMembers = async () => {
  try {
    if (!playerTeamId.value) {
      return
    }
    const response = await axios.get('/api/player/club/members')
    console.log('获取俱乐部成员API响应:', response.data)
    if (response.data.code === 200) {
      const data = response.data.data
      if (data) {
        coach.value = data.coach || {}
        coachValue.value = data.coach?.value || 0
        totalValue.value = data.totalValue || 0
        groups.value = data.groups || groups.value
        // 如果从members API获取到了club信息，也设置到currentClub
        if (data.club && !currentClub.value) {
          currentClub.value = data.club
          console.log('从members API获取到club:', currentClub.value)
        }
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const getLeagueStandings = async () => {
  try {
    // 如果没有俱乐部，不获取排名
    if (!playerTeamId.value) {
      console.log('没有俱乐部信息，跳过获取联赛排名')
      leagueStandings.value = []
      return
    }

    // 获取所有联赛列表
    const leaguesRes = await axios.get('/api/league/list')
    if (leaguesRes?.data?.code !== 200 || !leaguesRes.data.data || leaguesRes.data.data.length === 0) {
      console.log('没有找到任何联赛')
      leagueStandings.value = []
      return
    }

    // 遍历每个联赛，找到包含当前俱乐部的联赛
    for (const league of leaguesRes.data.data) {
      const standingsRes = await axios.get(`/api/league/${league.id}/standings`)
      if (standingsRes?.data?.code === 200 && standingsRes.data.data && standingsRes.data.data.length > 0) {
        const standings = standingsRes.data.data
        // 检查当前俱乐部是否在这个联赛的积分榜中
        const clubInLeague = standings.find(s => s.clubId === playerTeamId.value || s.teamName === currentClub.value?.name)
        if (clubInLeague) {
          // 找到了！设置这个联赛的排名
          leagueStandings.value = standings
          currentLeagueName.value = league.name
          console.log(`找到俱乐部 ${currentClub.value?.name || ''} 在联赛 ${league.name} 中的排名，共 ${standings.length} 支球队`)
          return
        }
      }
    }

    // 如果在所有联赛中都没找到当前俱乐部
    console.log('俱乐部未参加任何联赛')
    leagueStandings.value = []
  } catch (e) {
    console.error('获取联赛排名失败:', e)
    leagueStandings.value = []
  }
}

const getClubMatches = async () => {
  try {
    const response = await axios.get('/api/match/list')
    if (response.data.code === 200) {
      allMatches.value = response.data.data || []
    }
  } catch (e) {
    console.error('获取比赛列表失败:', e)
    allMatches.value = []
  }
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

const getApplicationStatus = async () => {
  try {
    const response = await axios.get('/api/player/club/application/status')
    if (response.data.code === 200) {
      applicationStatus.value = response.data.data
    }
  } catch (error) {
    console.error('获取申请状态失败:', error)
  }
}

const getStatusType = () => {
  if (!applicationStatus.value) return 'info'
  switch (applicationStatus.value.status) {
    case '待审核':
      return 'warning'
    case '已审核':
    case '已通过':
      return 'success'
    case '拒绝':
    case '已拒绝':
      return 'error'
    default:
      return 'info'
  }
}

const submitApplication = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const token = localStorage.getItem('token')
        const response = await axios.post('/api/player/club/apply', form.value)
        if (response.data.code === 200) {
          ElMessage.success('申请提交成功，等待审核')
          await getApplicationStatus()
        }
      } catch (error) {
        console.error('提交申请失败:', error)
        ElMessage.error(error.response?.data?.message || '提交申请失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const leaveClub = async () => {
  try {
    // 弹窗让用户输入离队原因
    const { value: reason } = await ElMessageBox.prompt('请输入离队原因（可选）', '申请离开俱乐部', {
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      inputPlaceholder: '请说明离队原因...'
    })

    const response = await axios.post('/api/player/club/leave', { reason: reason || '' })
    if (response.data.code === 200) {
      ElMessage.success('离队申请已提交，等待管理员审核')
      // 更新状态显示
      playerStatus.value = response.data.data?.status || playerStatus.value
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交离队申请失败:', error)
      ElMessage.error(error.response?.data?.message || '提交离队申请失败')
    }
  }
}

onMounted(async () => {
  // 先取球员状态/俱乐部ID，再加载俱乐部相关数据
  await getPlayerStatus()
  await Promise.all([
    getCurrentClub(),
    getClubs(),
    getApplicationStatus(),
    getClubMembers(),
    getLeagueStandings(),
    getClubMatches()
  ])
})
</script>

<style scoped>
.club {
  padding: 20px;
}

.club h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.club-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 0;
}

.club-logo {
  width: 80px;
  height: 80px;
  background-color: #e8e8e8;
  border-radius: 50%;
}

.club-logo.small {
  width: 40px;
  height: 40px;
}

.club-details h3 {
  margin-bottom: 10px;
  color: #333;
}

.club-details p {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.club-application {
  padding: 20px 0;
}

.club-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.application-status {
  padding: 20px 0;
}

.status-detail {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.status-detail.error {
  background-color: #fff1f0;
  border-left: 4px solid #ff4d4f;
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
  background-color: #e8e8e8;
  border-radius: 50%;
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

@media (max-width: 768px) {
  .club {
    padding: 10px;
  }
  
  .club-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .el-form {
    width: 100%;
  }
  
  .el-form-item__label {
    font-size: 12px;
    width: 80px;
  }
  
  .el-form-item__content {
    margin-left: 90px !important;
  }
  
  .el-button {
    width: 100%;
    margin-bottom: 10px;
  }
  
  .el-button:last-child {
    margin-bottom: 0;
  }
}

/* 高亮当前俱乐部的行 */
:deep(.current-club-row) {
  background-color: #e6f7ff !important;
  font-weight: bold;
}

:deep(.current-club-row:hover) {
  background-color: #bae7ff !important;
}
</style>