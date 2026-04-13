<template>
  <div class="dashboard-layout">
    <el-row :gutter="20">
      <!-- 左侧：仅首页展示俱乐部基础信息 -->
      <el-col :span="24">
        <el-card class="basic-card">
          <template #header>
            <div class="card-header">
              <span>俱乐部信息</span>
              <el-button type="primary" size="small" @click="saveClubInfo">保存信息</el-button>
            </div>
          </template>

          <div class="club-info">
            <p>
              俱乐部头像:
              <el-upload
                action="/api/upload/avatar"
                :headers="{ Authorization: `Bearer ${getToken()}` }"
                :show-file-list="false"
                :on-success="handleClubLogoSuccess"
              >
                <el-avatar :size="56" :src="clubInfo.logo || '/src/assets/images/default-avatar.svg'" />
              </el-upload>
            </p>
            <p>俱乐部名称: <el-input v-model="clubInfo.name" style="width: 260px" /></p>
            <p>主教练: <el-input v-model="clubInfo.headCoach" style="width: 260px" /></p>
            <p>翻译(多个用逗号分隔): <el-input v-model="clubInfo.translator" style="width: 360px" /></p>
            <p>
              赞助商:
              <span v-if="sponsorList.length === 0" style="margin-left: 8px;">暂无</span>
              <el-tag
                v-for="s in sponsorList"
                :key="s"
                size="small"
                style="margin-left: 8px;"
              >
                {{ s }}
              </el-tag>
            </p>
            <p>
              添加赞助商:
              <el-input
                v-model="newSponsor"
                style="width: 260px; margin-left: 10px"
                placeholder="输入赞助商名称"
                @keyup.enter="addSponsor"
              />
              <el-button type="primary" size="small" style="margin-left: 10px" @click="addSponsor">添加</el-button>
            </p>
            <p>俱乐部人员数量: {{ clubPersonnelCount }}</p>
            <p>球员数量: {{ playerCount }}</p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：模块导航 + 对应内容 -->
      <el-col :span="24">
        <el-card class="right-card">
          <template #header>
            <div class="card-header">
              <span>俱乐部模块</span>
            </div>
          </template>

          <el-menu
            :default-active="activePanel"
            mode="horizontal"
            @select="onPanelSelect"
            style="margin-bottom: 16px"
          >
            <el-menu-item index="personnel">俱乐部人员管理</el-menu-item>
            <el-menu-item index="schedule">赛事赛程</el-menu-item>
            <el-menu-item index="players">球员</el-menu-item>
            <el-menu-item index="rankings">联赛排行榜</el-menu-item>
          </el-menu>

          <!-- 俱乐部人员管理 -->
          <div v-if="activePanel === 'personnel'">
            <el-card class="panel-inner" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>俱乐部人员管理</span>
                  <el-button type="primary" @click="addCoach">添加人员</el-button>
                </div>
              </template>

              <el-table :data="allPersonnel" style="width: 100%">
                <el-table-column prop="name" label="姓名" />
                <el-table-column prop="type" label="类型" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.type === '球员' ? 'success' : 'warning'" size="small">
                      {{ scope.row.type }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="position" label="职位/位置" width="120" />
                <el-table-column prop="age" label="年龄" width="80" />
                <el-table-column prop="nationality" label="国籍" width="120" />
                <el-table-column prop="goals" label="进球" width="80" v-if="false">
                  <template #default="scope">
                    <span v-if="scope.row.type === '球员'">{{ scope.row.goals || 0 }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="assists" label="助攻" width="80" v-if="false">
                  <template #default="scope">
                    <span v-if="scope.row.type === '球员'">{{ scope.row.assists || 0 }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="marketValue" label="身价 (万欧)" width="120">
                  <template #default="scope">
                    <span v-if="scope.row.type === '球员'">{{ scope.row.marketValue || 0 }}</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="170">
                  <template #default="scope">
                    <el-button type="primary" size="small" @click="editPersonnel(scope.row)">编辑</el-button>
                    <el-button type="danger" size="small" @click="deletePersonnel(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <!-- 赛事赛程 -->
          <div v-else-if="activePanel === 'schedule'">
            <el-card class="panel-inner" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>赛事赛程</span>
                  <div class="season-selector">
                    <el-button @click="previousSeason">上赛季</el-button>
                    <el-select v-model="currentSeason" @change="fetchSchedule" style="width: 100px">
                      <el-option label="2026" value="2026" />
                      <el-option label="2025" value="2025" />
                      <el-option label="2024" value="2024" />
                    </el-select>
                    <el-button @click="nextSeason">下赛季</el-button>
                  </div>
                </div>
              </template>

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
            </el-card>
          </div>

          <!-- 球员 -->
          <div v-else-if="activePanel === 'players'">
            <el-card class="panel-inner" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>球员</span>
                </div>
              </template>

              <el-table :data="players" style="width: 100%">
                <el-table-column prop="realName" label="姓名" />
                <el-table-column prop="position" label="位置" width="100" />
                <el-table-column prop="age" label="年龄" width="80" />
                <el-table-column prop="nationality" label="国籍" width="120" />
                <el-table-column prop="goals" label="进球" width="80" />
                <el-table-column prop="assists" label="助攻" width="80" />
                <el-table-column prop="marketValue" label="身价(万欧)" width="120" />
              </el-table>
            </el-card>
          </div>

          <!-- 联赛排行榜 -->
          <div v-else-if="activePanel === 'rankings'">
            <el-card class="panel-inner" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>联赛排行榜</span>
                  <el-select
                    v-if="myLeagues && myLeagues.length"
                    v-model="selectedLeagueId"
                    style="width: 200px"
                    placeholder="选择联赛"
                    @change="updateLeagueStandings"
                  >
                    <el-option v-for="l in myLeagues" :key="l.id" :label="l.name" :value="l.id" />
                  </el-select>
                </div>
              </template>

              <el-table :data="sortedLeagueStandings" style="width: 100%">
                <el-table-column prop="rank" label="排名" width="80" />
                <el-table-column prop="teamName" label="球队" />
                <el-table-column prop="played" label="场次" width="80" />
                <el-table-column prop="wins" label="胜" width="80" />
                <el-table-column prop="draws" label="平" width="80" />
                <el-table-column prop="losses" label="负" width="80" />
                <el-table-column prop="goalDiff" label="净胜球" width="90" />
                <el-table-column prop="points" label="积分" width="80" />
              </el-table>
            </el-card>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="coachDialogVisible"
      :title="editingCoach ? '编辑人员' : '添加人员'"
      width="520px"
    >
      <el-form :model="coachForm" label-width="90px">
        <el-form-item label="职位类型">
          <el-select v-model="personnelType" style="width: 100%" @change="onPersonnelTypeChange">
            <el-option label="球员" value="PLAYER" />
            <el-option label="主教练" value="HEAD_COACH" />
            <el-option label="翻译" value="TRANSLATOR" />
            <el-option label="助理教练" value="ASSISTANT_COACH" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="personnelType === 'PLAYER'" label="自由身球员">
          <el-select
            v-model="selectedFreeAgentUserId"
            filterable
            placeholder="请选择自由身球员"
            style="width: 100%"
          >
            <el-option
              v-for="fa in freeAgents"
              :key="fa.userId"
              :label="`${fa.realName || ''}(${fa.position || ''})`"
              :value="fa.userId"
            />
          </el-select>
        </el-form-item>

        <template v-else>
          <el-form-item label="姓名">
            <el-input v-model="coachForm.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="职位">
            <el-input v-model="coachForm.position" disabled />
          </el-form-item>
          <el-form-item label="年龄">
            <el-input v-model.number="coachForm.age" type="number" placeholder="请输入年龄" />
          </el-form-item>
          <el-form-item label="国籍">
            <el-input v-model="coachForm.nationality" placeholder="请输入国籍" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="coachDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCoach">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const clubInfo = ref({ name: '', headCoach: '', translator: '', sponsor: '', logo: '' })
const userStore = useUserStore()
const newSponsor = ref('')
const sponsorList = computed(() => {
  const raw = String(clubInfo.value.sponsor || '')
  return raw.split(',').map(s => s.trim()).filter(Boolean)
})
const clubPersonnelCount = ref(0)

const teamCount = ref(1)
const playerCount = ref(0)
const matches = ref([])
const activeTab = ref('coaches')
// 右侧模块切换：home=仅基础信息；personnel/schedule/players/rankings=对应内容
const activePanel = ref('personnel')
const currentSeason = ref('2026')

const coaches = ref([])
const coachDialogVisible = ref(false)
const editingCoach = ref(false)
const coachForm = ref({
  id: null,
  name: '',
  position: '',
  age: null,
  nationality: ''
})

// 合并所有人员（教练 + 球员）
const allPersonnel = computed(() => {
  const personnelList = []
  
  // 添加教练组
  coaches.value.forEach(coach => {
    personnelList.push({
      ...coach,
      type: '教练',
      goals: null,
      assists: null,
      marketValue: null
    })
  })
  
  // 添加球员
  players.value.forEach(player => {
    personnelList.push({
      id: player.id,
      name: player.realName,
      type: '球员',
      position: player.position,
      age: player.age,
      nationality: player.nationality,
      goals: player.goals,
      assists: player.assists,
      marketValue: player.marketValue,
      isPlayer: true
    })
  })
  
  return personnelList
})

// 添加人员：支持 球员/主教练/翻译/助理教练
const personnelType = ref('HEAD_COACH')
const selectedFreeAgentUserId = ref(null)
const freeAgents = ref([])

const staffPositionMap = {
  HEAD_COACH: '主教练',
  TRANSLATOR: '翻译',
  ASSISTANT_COACH: '助理教练'
}

const players = ref([])

const leagueStandings = ref([])
const myLeagues = ref([])
const selectedLeagueId = ref(null)

const sortedLeagueStandings = computed(() => Array.isArray(leagueStandings.value) ? leagueStandings.value : [])

const applications = ref([])

const parseMatchTitle = (title) => {
  if (!title) return { homeTeamName: '', awayTeamName: '' }
  // 兼容：`A VS B` / `AVSB` / `A   VS   B` 等多种格式
  const parts = String(title).split(/\s*vs\s*/i)
  if (parts.length >= 2) {
    return { homeTeamName: parts[0], awayTeamName: parts.slice(1).join(' vs ') }
  }
  return { homeTeamName: String(title), awayTeamName: '' }
}

const teamMatches = computed(() => {
  const grouped = {}
  ;(matches.value || []).forEach((m) => {
    const matchTime = m.startTime || m.matchTime
    const date = new Date(matchTime)
    if (Number.isNaN(date.getTime())) return
    const month = `${date.getFullYear()}年${date.getMonth() + 1}月`
    if (!grouped[month]) grouped[month] = []

    const { homeTeamName, awayTeamName } = parseMatchTitle(m.title)

    // 将后端比赛状态映射到页面需要的状态字段
    let status = 'UPCOMING'
    if (m.status === 'ENDED') status = 'COMPLETED'
    else if (m.status === 'ONGOING') status = 'ONGOING'

    grouped[month].push({
      id: m.id,
      homeTeamName,
      awayTeamName,
      homeTeamLogo: m.homeTeamLogo || '',
      awayTeamLogo: m.awayTeamLogo || '',
      matchTime,
      status,
      homeScore: 0,
      awayScore: 0
    })
  })
  return grouped
})

const getToken = () => {
  try {
    return localStorage.getItem('token') || ''
  } catch (error) {
    return ''
  }
}

const onPanelSelect = (key) => {
  activePanel.value = key
}

const fetchSchedule = async () => {
  try {
    // 优先使用俱乐部赛程接口
    const response = await axios.get('/api/club/schedule', {
      params: { season: currentSeason.value }
    })
    if (response.data.code === 200) {
      matches.value = response.data.data || []
      return
    }
  } catch (e) {
    console.warn('获取俱乐部赛程失败，回退到公开赛事列表:', e)
  }

  try {
    const response = await axios.get('/api/match/list')
    if (response.data.code === 200) {
      matches.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取赛事列表失败:', error)
    matches.value = []
  }
}

const getClubInfo = async () => {
  try {
    const response = await axios.get('/api/club/info')
    if (response.data.code === 200) {
      clubInfo.value = response.data.data || {}
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '获取俱乐部信息失败')
  }
}

const getClubSummary = async () => {
  try {
    const response = await axios.get('/api/club/info/summary')
    if (response.data.code === 200) {
      const data = response.data.data || {}
      playerCount.value = data.playerCount || 0
      teamCount.value = data.teamCount || 1
      players.value = data.members || []
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '读取俱乐部成员失败')
  }
}

const getCoaches = async () => {
  try {
    const response = await axios.get('/api/club/coaches')
    if (response.data.code === 200) {
      coaches.value = response.data.data || []
      clubPersonnelCount.value = coaches.value.length
    }
  } catch (error) {
    console.error('获取教练失败:', error)
    coaches.value = []
    clubPersonnelCount.value = 0
  }
}

const fetchFreeAgents = async () => {
  try {
    const response = await axios.get('/api/club/free-agents')
    if (response.data.code === 200) {
      freeAgents.value = response.data.data || []
    } else {
      freeAgents.value = []
    }
  } catch (e) {
    console.error('获取自由身球员失败:', e)
    freeAgents.value = []
  }
}

const onPersonnelTypeChange = async () => {
  // 球员：展示自由身球员选择列表
  if (personnelType.value === 'PLAYER') {
    selectedFreeAgentUserId.value = null
    if (!freeAgents.value.length) {
      await fetchFreeAgents()
    }
    return
  }

  // 教练/翻译/助理教练：position 跟随选择类型自动填充
  selectedFreeAgentUserId.value = null
  coachForm.value.position = staffPositionMap[personnelType.value] || coachForm.value.position
}

const getRankings = async () => {
  try {
    const leaguesRes = await axios.get('/api/club/my/leagues')
    if (leaguesRes?.data?.code === 200) {
      myLeagues.value = leaguesRes.data.data || []
      if (!selectedLeagueId.value && myLeagues.value.length) {
        selectedLeagueId.value = myLeagues.value[0].id
      }
    }
    if (!selectedLeagueId.value) {
      leagueStandings.value = []
      return
    }
    const res = await axios.get(`/api/league/${selectedLeagueId.value}/standings`)
    if (res?.data?.code === 200) {
      leagueStandings.value = res.data.data || []
    } else {
      leagueStandings.value = []
    }
  } catch (e) {
    console.error('获取联赛积分榜失败:', e)
    leagueStandings.value = []
  }
}

const updateLeagueStandings = async (leagueId) => {
  if (!leagueId) {
    leagueStandings.value = []
    return
  }
  try {
    const res = await axios.get(`/api/league/${leagueId}/standings`)
    if (res?.data?.code === 200) {
      leagueStandings.value = res.data.data || []
    } else {
      leagueStandings.value = []
    }
  } catch (e) {
    console.error('获取联赛积分榜失败:', e)
    leagueStandings.value = []
  }
}

const getApplications = async () => {
  try {
    const response = await axios.get('/api/club/player-apply/list')
    if (response.data.code === 200) {
      const list = response.data.data || []
      applications.value = list.map((pi) => ({
        id: pi.id,
        playerId: pi.userId,
        playerName: pi.realName,
        status: 'PENDING',
        remark: pi.clubRemark || pi.adminRemark || ''
      }))
    }
  } catch (error) {
    console.error('获取球员申请列表失败:', error)
    applications.value = []
  }
}

const saveClubInfo = async () => {
  try {
    const response = await axios.put('/api/club/info', {
      name: clubInfo.value.name,
      headCoach: clubInfo.value.headCoach,
      translator: clubInfo.value.translator,
      sponsor: clubInfo.value.sponsor,
      logo: clubInfo.value.logo
    })
    if (response.data.code === 200) {
      ElMessage.success('俱乐部信息已保存')
      await getClubInfo()
      await getClubSummary()
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

const addSponsor = () => {
  const v = newSponsor.value.trim()
  if (!v) {
    ElMessage.error('请输入赞助商名称')
    return
  }

  const next = [...sponsorList.value]
  if (next.includes(v)) {
    ElMessage.warning('该赞助商已存在')
    newSponsor.value = ''
    return
  }

  next.push(v)
  clubInfo.value.sponsor = next.join(',')
  newSponsor.value = ''
}

const registerMatch = async (id) => {
  try {
    const response = await axios.post(`/api/club/match/register/${id}`)
    if (response.data.code === 200) {
      ElMessage.success('报名成功')
    }
  } catch (error) {
    console.error('报名失败:', error)
    ElMessage.error(error.response?.data?.message || '报名失败')
  }
}

const createTeam = () => {
  // 创建球队
  console.log('创建球队')
}

const addCoach = () => {
  // 添加俱乐部人员（教练）
  editingCoach.value = false
  personnelType.value = 'HEAD_COACH'
  selectedFreeAgentUserId.value = null
  coachForm.value = {
    id: null,
    name: '',
    position: staffPositionMap.HEAD_COACH,
    age: null,
    nationality: ''
  }
  coachDialogVisible.value = true
}

const editCoach = (coach) => {
  editingCoach.value = true
  selectedFreeAgentUserId.value = null
  const pos = coach.position || ''
  if (pos.includes('主教练')) personnelType.value = 'HEAD_COACH'
  else if (pos.includes('翻译')) personnelType.value = 'TRANSLATOR'
  else personnelType.value = 'ASSISTANT_COACH'
  coachForm.value = {
    id: coach.id,
    name: coach.name || '',
    position: coach.position || staffPositionMap[personnelType.value] || '',
    age: coach.age ?? null,
    nationality: coach.nationality || ''
  }
  coachDialogVisible.value = true
}

const deleteCoach = async (id) => {
  try {
    const res = await axios.delete(`/api/club/coaches/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      await getCoaches()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

// 编辑人员（区分教练和球员）
const editPersonnel = (person) => {
  if (person.isPlayer) {
    // 编辑球员
    ElMessage.info('请前往球员档案模块修改信息')
  } else {
    // 编辑教练
    editCoach(person)
  }
}

// 删除人员（区分教练和球员）
const deletePersonnel = async (person) => {
  if (person.isPlayer) {
    // 删除球员
    ElMessage.info('球员离队请走审核流程')
  } else {
    // 删除教练
    await deleteCoach(person.id)
  }
}

const saveCoach = async () => {
  try {
    // 球员加入俱乐部：只能添加自由身球员
    if (personnelType.value === 'PLAYER') {
      if (!selectedFreeAgentUserId.value) {
        ElMessage.error('请选择自由身球员')
        return
      }
      const res = await axios.post('/api/club/roster/players/add', {
        playerUserId: selectedFreeAgentUserId.value
      })
      if (res.data.code === 200) {
        ElMessage.success('球员添加成功')
        coachDialogVisible.value = false
        await getClubSummary()
      } else {
        ElMessage.error(res.data.message || '添加失败')
      }
      return
    }

    // 教练/翻译/助理教练：保证 position 与选择类型一致
    coachForm.value.position = staffPositionMap[personnelType.value] || coachForm.value.position

    const name = (coachForm.value.name || '').trim()
    const position = (coachForm.value.position || '').trim()
    if (!name || !position) {
      ElMessage.error('姓名和职位不能为空')
      return
    }

    // 添加时：若同名同职位已存在，自动改为更新（避免重复）
    if (!editingCoach.value) {
      const existed = (coaches.value || []).find(c => c.name === name && c.position === position)
      if (existed) {
        const res = await axios.put(`/api/club/coaches/${existed.id}`, coachForm.value)
        if (res.data.code === 200) {
          ElMessage.success('人员已存在，已更新')
          coachDialogVisible.value = false
          await getCoaches()
        } else {
          ElMessage.error(res.data.message || '更新失败')
        }
        return
      }

      const res = await axios.post('/api/club/coaches', coachForm.value)
      if (res.data.code === 200) {
        ElMessage.success('添加成功')
        coachDialogVisible.value = false
        await getCoaches()
      } else {
        ElMessage.error(res.data.message || '添加失败')
      }
      return
    }

    // 编辑
    if (!coachForm.value.id) {
      ElMessage.error('缺少要编辑的人员 ID')
      return
    }
    const res = await axios.put(`/api/club/coaches/${coachForm.value.id}`, coachForm.value)
    if (res.data.code === 200) {
      ElMessage.success('更新成功')
      coachDialogVisible.value = false
      await getCoaches()
    } else {
      ElMessage.error(res.data.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

const addPlayer = () => {
  ElMessage.info('球员归属由球员申请和审核流程管理')
}

const editPlayer = (player) => {
  ElMessage.info('请前往球员档案模块修改信息')
}

const deletePlayer = (id) => {
  ElMessage.info('球员离队请走审核流程')
}

const handleClubLogoSuccess = (res) => {
  if (res.code === 200) {
    clubInfo.value.logo = res.data
    // 同步更新 userStore 中的头像，确保右上角头像刷新
    userStore.avatar = res.data
    localStorage.setItem('avatar', res.data)
    ElMessage.success('俱乐部头像上传成功，记得保存信息')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const approveApplication = async (playerUserId) => {
  try {
    const res = await axios.put(`/api/club/player-apply/audit/${playerUserId}`, {
      action: 'APPROVE',
      remark: ''
    })
    if (res.data.code === 200) {
      ElMessage.success('审核通过')
      await getApplications()
    }
  } catch (error) {
    console.error('审核通过失败:', error)
    ElMessage.error(error.response?.data?.message || '审核通过失败')
  }
}

const rejectApplication = async (playerUserId) => {
  try {
    const res = await axios.put(`/api/club/player-apply/audit/${playerUserId}`, {
      action: 'REJECT',
      remark: ''
    })
    if (res.data.code === 200) {
      ElMessage.success('审核拒绝')
      await getApplications()
    }
  } catch (error) {
    console.error('审核拒绝失败:', error)
    ElMessage.error(error.response?.data?.message || '审核拒绝失败')
  }
}

const previousSeason = () => {
  const year = parseInt(currentSeason.value)
  if (year > 2024) {
    currentSeason.value = (year - 1).toString()
    fetchSchedule()
  }
}

const nextSeason = () => {
  const year = parseInt(currentSeason.value)
  if (year < 2026) {
    currentSeason.value = (year + 1).toString()
    fetchSchedule()
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
  getClubInfo()
  getClubSummary()
  getCoaches()
  getRankings()
  fetchSchedule()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-layout {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.basic-card,
.right-card {
  /* 让页面仅按内容高度滚动，避免布局异常 */
}

.panel-inner {
  margin-top: 0;
}

.right-empty {
  color: #666;
  padding: 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.club-info {
  padding: 20px;
}

.club-info p {
  margin-bottom: 10px;
  font-size: 16px;
  color: #333;
}

.section {
  margin-top: 20px;
}

.tabs {
  margin-top: 20px;
}

.season-selector {
  display: flex;
  align-items: center;
  gap: 10px;
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

.team-logo {
  width: 32px;
  height: 32px;
  background-color: #e8e8e8;
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

@media (max-width: 768px) {
  .dashboard {
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