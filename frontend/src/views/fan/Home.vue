<template>
  <div class="home">
    <!-- 导航栏 -->
    <div class="nav-bar">
      <el-tabs v-model="activeLeagueId">
        <el-tab-pane
          v-for="league in leagues"
          :key="league.id"
          :label="league.name"
          :name="league.id"
        ></el-tab-pane>
      </el-tabs>
    </div>

    <!-- 最新赛事（与管理员赛事管理同源数据，按上方联赛 Tab 过滤） -->
    <div class="section">
      <h2>最新赛事</h2>
      <el-empty v-if="!displayMatches.length" description="该联赛暂无赛程" />
      <div v-else class="match-list">
        <el-card v-for="match in displayMatches" :key="match.id" class="match-card">
          <div class="match-info">
            <div class="match-header">
              <span class="match-date">{{ formatDate(match.startTime) }}</span>
              <span class="match-league">{{ match.leagueName || '联赛' }}</span>
            </div>
            <div class="match-teams">
              <div class="team">
                <div class="team-logo"></div>
                <span class="team-name">{{ match.homeTeam || '主队' }}</span>
              </div>
              <div class="match-score">
                <template v-if="showScore(match)">
                  <span class="score">{{ match.homeScore }}</span>
                  <span class="separator">-</span>
                  <span class="score">{{ match.awayScore }}</span>
                </template>
                <template v-else>
                  <span class="score-pending">{{ statusText(match.status) }}</span>
                </template>
              </div>
              <div class="team">
                <span class="team-name">{{ match.awayTeam || '客队' }}</span>
                <div class="team-logo"></div>
              </div>
            </div>
            <div v-if="match.matchResult" class="match-result-hint">{{ match.matchResult }}</div>
            <div class="match-footer">
              <span class="match-time">{{ formatTime(match.startTime) }}</span>
              <el-button type="primary" size="small" @click="viewMatch(match.id)">查看详情</el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 联赛排行榜 -->
    <div class="section">
      <h2>联赛排行榜</h2>
      <el-table :data="rankingList" style="width: 100%">
        <el-table-column prop="rank" label="排名" width="80"></el-table-column>
        <el-table-column prop="team" label="球队">
          <template #default="scope">
            <div class="team-info">
              <img :src="scope.row.teamLogo || '/src/assets/images/default-avatar.svg'" class="team-logo small" alt="team logo" />
              <span>{{ scope.row.team }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="matches" label="场次" width="80"></el-table-column>
        <el-table-column prop="wins" label="胜" width="80"></el-table-column>
        <el-table-column prop="draws" label="平" width="80"></el-table-column>
        <el-table-column prop="losses" label="负" width="80"></el-table-column>
        <el-table-column prop="points" label="积分" width="80"></el-table-column>
      </el-table>
    </div>

    <!-- 关注球队 -->
    <div class="section">
      <h2>关注球队</h2>
      <div class="follow-club-list">
        <el-card v-for="club in clubs" :key="club.id" class="follow-club-card">
          <div class="follow-club-row">
            <img :src="club.logo || '/src/assets/images/default-avatar.svg'" class="club-logo" alt="club logo" />
            <div class="club-info">
              <div class="club-name">{{ club.name }}</div>
            </div>
          </div>
          <div class="follow-actions">
            <el-button
              size="small"
              :type="isFollowed(club.id) ? 'info' : 'primary'"
              @click="toggleFollow(club.id)"
            >
              {{ isFollowed(club.id) ? '已关注' : '关注' }}
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 联系我们 -->
    <div class="section">
      <h2>联系我们</h2>
      <el-card v-if="contact" class="contact-card">
        <div>微信：{{ contact.officialWechat || '-' }}</div>
        <div>邮箱：{{ contact.officialEmail || '-' }}</div>
        <div>电话：{{ contact.officialPhone || '-' }}</div>
        <div>QQ：{{ contact.officialQq || '-' }}</div>
        <div>官网：{{ contact.officialWebsite || '-' }}</div>
        <div v-if="contact.remark" class="contact-remark">{{ contact.remark }}</div>
      </el-card>
      <el-card v-else class="contact-card">暂无联系信息</el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import axios from '@/utils/axios'
import { useRouter } from 'vue-router'

const router = useRouter()

const activeLeagueId = ref(null)
const leagues = ref([])
/** 全量赛事（与管理员端同源 /api/match/list），按联赛 Tab 再过滤展示 */
const allMatches = ref([])
const rankingList = ref([])

const clubs = ref([])
const followedClubIds = ref([])
const contact = ref(null)

const parseMatchTitle = (title) => {
  if (!title) return { homeTeam: '', awayTeam: '' }
  // 兼容 "A VS B" / "A vs B" / 无空格 "武汉三镇VS河南队"
  const parts = String(title).split(/\s*vs\s*/i)
  if (parts.length >= 2) {
    return { homeTeam: parts[0].trim(), awayTeam: parts.slice(1).join(' vs ').trim() }
  }
  return { homeTeam: String(title), awayTeam: '' }
}

const normalizeMatchStatus = (s) => {
  if (!s) return 'REGISTERING'
  if (s === '已结束') return 'ENDED'
  if (s === '进行中') return 'ONGOING'
  if (s === '报名中' || s === '待比赛' || s === '未开始') return 'REGISTERING'
  return s
}

const mapMatchRow = (m) => {
  const fromTitle = parseMatchTitle(m.title)
  const homeTeam = m.homeTeamName || fromTitle.homeTeam
  const awayTeam = m.awayTeamName || fromTitle.awayTeam
  const st = normalizeMatchStatus(m.status)
  return {
    ...m,
    homeTeam,
    awayTeam,
    status: st,
    homeScore: m.homeScore,
    awayScore: m.awayScore
  }
}

/** 当前 Tab 对应联赛下的赛事，按开赛时间倒序 */
const displayMatches = computed(() => {
  const lid = activeLeagueId.value
  if (lid == null || lid === '') return []
  const idNum = Number(lid)
  const list = allMatches.value.filter((m) => m.leagueId != null && Number(m.leagueId) === idNum)
  return [...list].sort((a, b) => {
    const ta = a.startTime ? new Date(a.startTime).getTime() : 0
    const tb = b.startTime ? new Date(b.startTime).getTime() : 0
    return tb - ta
  })
})

const showScore = (m) => {
  const st = m.status
  if (st === 'ENDED' || st === '已结束') {
    return m.homeScore != null && m.awayScore != null
  }
  if (st === 'ONGOING') {
    return m.homeScore != null && m.awayScore != null
  }
  return false
}

const statusText = (st) => {
  if (st === 'ONGOING') return '进行中'
  if (st === 'ENDED' || st === '已结束') return '已结束'
  return '未开始'
}

const getMatches = async () => {
  try {
    const response = await axios.get('/api/match/list')
    if (response.data.code === 200) {
      // 必须使用后端返回的比分与 enrich 后的赛果，禁止写死 0:0
      allMatches.value = (response.data.data || []).map((m) => mapMatchRow(m))
    }
  } catch (error) {
    console.error('获取赛事列表失败:', error)
  }
}

const viewMatch = (id) => {
  // 跳转到赛事详情页面
  router.push(`/fan/match/${id}`)
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const isFollowed = (clubId) => {
  return followedClubIds.value.includes(clubId)
}

const getClubs = async () => {
  try {
    const response = await axios.get('/api/club/list')
    if (response.data.code === 200) {
      clubs.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取俱乐部列表失败:', error)
  }
}

const getFollowedClubs = async () => {
  try {
    const response = await axios.get('/api/fan/follow/club/list')
    if (response.data.code === 200) {
      followedClubIds.value = (response.data.data || []).map(c => c.id)
    }
  } catch (error) {
    console.error('获取关注列表失败:', error)
  }
}

const toggleFollow = async (clubId) => {
  try {
    if (isFollowed(clubId)) {
      await axios.delete(`/api/fan/follow/club/${clubId}`)
    } else {
      await axios.post(`/api/fan/follow/club/${clubId}`)
    }
    await getFollowedClubs()
  } catch (error) {
    console.error('关注/取消关注失败:', error)
  }
}

const getContact = async () => {
  try {
    const response = await axios.get('/api/public/contact')
    if (response.data.code === 200) {
      contact.value = response.data.data
    }
  } catch (error) {
    console.error('获取联系方式失败:', error)
  }
}

// 获取所有联赛
const getLeagues = async () => {
  try {
    console.log('开始获取联赛列表...');
    const response = await axios.get('/api/league/list')
    console.log('联赛列表响应:', response.data);
    if (response.data.code === 200) {
      leagues.value = response.data.data || []
      console.log('联赛数量:', leagues.value.length);
      // 默认选中第一个联赛
      if (leagues.value.length > 0) {
        activeLeagueId.value = leagues.value[0].id
        console.log('默认选中联赛 ID:', activeLeagueId.value);
        getLeagueStandings(leagues.value[0].id)
      }
    } else {
      console.error('联赛列表响应码错误:', response.data);
    }
  } catch (error) {
    console.error('获取联赛列表失败:', error)
    console.error('错误详情:', error.response?.data);
    console.error('错误状态码:', error.response?.status);
    console.error('错误消息:', error.response?.data?.message || error.message);
  }
}

// 获取联赛积分榜（统一按后端 standings：积分↓，净胜球↓）
const getLeagueStandings = async (leagueId) => {
  try {
    console.log('开始获取联赛', leagueId, '的积分榜...')
    const response = await axios.get(`/api/league/${leagueId}/standings`)
    console.log('积分榜响应:', response.data)
    if (response.data.code === 200) {
      const list = response.data.data || []
      rankingList.value = list.map((r) => ({
        rank: r.rank,
        team: r.teamName,
        teamLogo: r.teamLogo,
        matches: r.played,
        wins: r.wins,
        draws: r.draws,
        losses: r.losses,
        points: r.points
      }))
    } else {
      rankingList.value = []
    }
  } catch (error) {
    console.error('获取联赛积分榜失败:', error)
    rankingList.value = []
  }
}

// 监听联赛切换：积分榜随 Tab 变化；赛事列表由 displayMatches 计算属性过滤
watch(activeLeagueId, (newLeagueId) => {
  if (newLeagueId) {
    getLeagueStandings(newLeagueId)
  }
})

onMounted(() => {
  getLeagues()
  getMatches()

  getClubs()
  getFollowedClubs()
  getContact()
})
</script>

<style scoped>
.home {
  padding: 20px;
}

.nav-bar {
  margin-bottom: 30px;
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 8px;
}

.section {
  margin-bottom: 40px;
}

.section h2 {
  margin-bottom: 20px;
  color: #faad14;
  font-size: 18px;
  font-weight: bold;
}

.match-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.match-card {
  transition: transform 0.3s ease;
}

.match-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.match-info {
  padding: 10px;
}

.match-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.match-teams {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.team {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.team:first-child {
  justify-content: flex-end;
}

.team-logo {
  width: 40px;
  height: 40px;
  background-color: #e8e8e8;
  border-radius: 50%;
}

.team-logo.small {
  width: 24px;
  height: 24px;
}

.team-name {
  font-weight: bold;
}

.match-score {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.score-pending {
  font-size: 16px;
  font-weight: 600;
  color: #909399;
}

.match-result-hint {
  text-align: center;
  font-size: 13px;
  color: #67c23a;
  margin-bottom: 8px;
}

.separator {
  font-size: 18px;
  color: #999;
}

.match-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.ranking-list {
  margin-top: 20px;
}

.team-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comment-list {
  display: grid;
  gap: 20px;
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

.follow-club-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.follow-club-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 110px;
}

.follow-club-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.club-logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e8e8e8;
}

.club-name {
  font-weight: 600;
}

.follow-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.contact-card {
  line-height: 2;
}

.contact-remark {
  margin-top: 8px;
  color: #666;
}
</style>