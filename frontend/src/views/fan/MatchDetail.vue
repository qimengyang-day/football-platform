<template>
  <div class="match-detail">
    <h1>赛事详情</h1>
    <el-card v-if="match">
      <template #header>
        <div class="card-header">
          <span>{{ match.homeTeam }} vs {{ match.awayTeam }}</span>
        </div>
      </template>
      <div class="match-info">
        <div class="match-time">
          <el-icon><Calendar /></el-icon>
          <span>{{ formatDate(match.startTime) }} {{ formatTime(match.startTime) }}</span>
        </div>
        <div class="match-score">
          <div class="team">
            <div class="team-logo"></div>
            <span class="team-name">{{ match.homeTeam }}</span>
          </div>
          <div class="score">
            <template v-if="showScore(match)">
              <span>{{ match.homeScore }}</span>
              <span class="separator">-</span>
              <span>{{ match.awayScore }}</span>
            </template>
            <template v-else>
              <span class="pending">{{ statusText(match.status) }}</span>
            </template>
          </div>
          <div class="team">
            <span class="team-name">{{ match.awayTeam }}</span>
            <div class="team-logo"></div>
          </div>
        </div>
        <div class="match-stats">
          <div class="stat-item">
            <span class="stat-label">联赛</span>
            <span class="stat-value">{{ match.leagueName || '未知' }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">状态</span>
            <span class="stat-value">{{ statusText(match.status) }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 赛事评分 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>赛事评分</span>
        </div>
      </template>
      <div class="match-rating">
        <div class="rating-info" v-if="userScore">
          <p>您的评分：</p>
          <el-rate v-model="userScore" disabled />
          <p class="rating-time">评分时间：{{ formatDate(userScoreTime) }}</p>
        </div>
        <div class="rating-form" v-else>
          <p>请为这场赛事评分：</p>
          <el-rate v-model="score" :max="5" />
          <el-button type="primary" @click="submitScore" style="margin-top: 15px">提交评分</el-button>
        </div>
        <div class="average-rating" v-if="averageScore">
          <p>平均评分：</p>
          <el-rate v-model="averageScore" disabled />
          <p class="rating-count">{{ ratingCount }}人评分</p>
        </div>
      </div>
    </el-card>

    <!-- 评论区 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>评论区</span>
        </div>
      </template>
      <div class="comment-section">
        <el-form :model="commentForm" ref="commentFormRef" label-width="0">
          <el-form-item prop="content">
            <el-input
              v-model="commentForm.content"
              type="textarea"
              rows="3"
              placeholder="写下您的评论..."
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitComment">发布评论</el-button>
          </el-form-item>
        </el-form>
        <div class="comment-list">
          <el-card v-for="comment in comments" :key="comment.id" class="comment-card">
            <div class="comment-header">
              <div class="user-info">
                <el-avatar :size="40" :src="comment.avatar || '/src/assets/images/default-avatar.svg'" />
                <div class="user-details">
                  <span class="username">{{ comment.nickname || comment.username }}</span>
                  <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
                </div>
              </div>
            </div>
            <div class="comment-content">
              {{ comment.content }}
            </div>
            <div class="comment-footer">
              <el-button size="small" @click="likeComment(comment.id)" :type="comment.isLiked ? 'primary' : 'default'">
                <el-icon><i-ep-thumbs-up /></el-icon>
                {{ comment.likes }}
              </el-button>
              <el-button size="small" @click="openReplyDialog(comment)" type="default">
                <el-icon><i-ep-chat-dot-round /></el-icon>
                {{ (comment.replyCount || 0) + ' 回复' }}
              </el-button>
              <el-tag
                v-if="(comment.myReplyCount || 0) > 0"
                type="info"
                size="small"
                style="margin-left: 8px;"
              >
                你被回复({{ comment.myReplyCount }})
              </el-tag>
            </div>
          </el-card>
        </div>
        <el-pagination
          v-if="total > 0"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
          style="margin-top: 20px"
        />
      </div>
    </el-card>

    <!-- 评论回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="评论回复" width="720px">
      <div v-if="activeComment" class="reply-dialog-meta">
        <div class="reply-dialog-line">
          <span class="meta-label">评论：</span>
          <span class="meta-value">{{ activeComment.content }}</span>
        </div>
        <div class="reply-dialog-line">
          <span class="meta-label">回复数：</span>
          <span class="meta-value">{{ activeComment.replyCount || 0 }}</span>
          <el-tag v-if="hasReplyToMe" type="warning" style="margin-left: 12px;">你有回复</el-tag>
        </div>
      </div>

      <div class="reply-list">
        <div
          v-for="r in replies"
          :key="r.id"
          class="reply-item"
          :style="{ marginLeft: getReplyDepth(r) * 18 + 'px' }"
        >
          <div class="reply-top">
            <span class="reply-user">{{ r.replyUsername || r.username }}</span>
            <span v-if="r.replyToUsername" class="reply-to">回复 {{ r.replyToUsername }}</span>
            <span class="reply-time">{{ formatDate(r.createTime) }}</span>
          </div>
          <div class="reply-content">{{ r.content }}</div>
          <div class="reply-actions">
            <el-button size="small" @click="replyTo(r)" type="default">回复</el-button>
          </div>
          <el-tag v-if="myUserId && Number(r.replyToUserId) === Number(myUserId)" type="info" size="small" style="margin-top: 6px;">你被回复</el-tag>
        </div>
      </div>

      <div class="reply-editor">
        <div class="reply-target-line">
          <span class="meta-label">回复对象：</span>
          <span class="meta-value">{{ replyTargetUsername }}</span>
        </div>
        <el-input
          v-model="replyForm.content"
          type="textarea"
          :rows="3"
          placeholder="输入回复内容..."
        />
        <div class="reply-editor-actions">
          <el-button type="primary" @click="submitReply" :loading="replySubmitting">发送回复</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'

const route = useRoute()
const matchId = route.params.id

const match = ref(null)
const score = ref(0)
const userScore = ref(null)
const userScoreTime = ref(null)
const averageScore = ref(null)
const ratingCount = ref(0)
const commentForm = ref({ content: '' })
const commentFormRef = ref(null)
const comments = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 评论回复（评论回复/回复回复）
const replyDialogVisible = ref(false)
const activeComment = ref(null)
const replies = ref([])
const replyTargetUsername = ref('')
const replyForm = ref({
  content: '',
  parentReplyId: null,
  replyToUserId: null
})
const replySubmitting = ref(false)
const myUserId = ref(null)

const replyById = computed(() => {
  const m = new Map()
  ;(replies.value || []).forEach(r => m.set(r.id, r))
  return m
})

const hasReplyToMe = computed(() => {
  if (!myUserId.value) return false
  return (replies.value || []).some(r => Number(r.replyToUserId) === Number(myUserId.value))
})

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

const parseMatchTitle = (title) => {
  if (!title) return { homeTeam: '主队', awayTeam: '客队' }
  const parts = String(title).split(/\s*vs\s*/i)
  if (parts.length >= 2) return { homeTeam: parts[0].trim(), awayTeam: parts.slice(1).join(' vs ').trim() }
  return { homeTeam: String(title), awayTeam: '' }
}

const normalizeMatchStatus = (s) => {
  if (!s) return 'REGISTERING'
  if (s === '已结束') return 'ENDED'
  if (s === '进行中') return 'ONGOING'
  if (s === '报名中' || s === '待比赛' || s === '未开始') return 'REGISTERING'
  return s
}

const statusText = (s) => {
  const st = normalizeMatchStatus(s)
  if (st === 'ENDED') return '已结束'
  if (st === 'ONGOING') return '进行中'
  return '未开始'
}

const showScore = (m) => {
  const st = normalizeMatchStatus(m?.status)
  if (st === 'ENDED' || st === 'ONGOING') {
    return m?.homeScore != null && m?.awayScore != null
  }
  return false
}

const getMatchDetail = async () => {
  try {
    const response = await axios.get(`/api/match/${matchId}`)
    if (response.data.code === 200) {
      const m = response.data.data
      const fromTitle = parseMatchTitle(m.title)
      match.value = {
        ...m,
        homeTeam: m.homeTeamName || fromTitle.homeTeam,
        awayTeam: m.awayTeamName || fromTitle.awayTeam,
        status: normalizeMatchStatus(m.status),
        homeScore: m.homeScore,
        awayScore: m.awayScore
      }
    }
  } catch (error) {
    console.error('获取赛事详情失败:', error)
    ElMessage.error('获取赛事详情失败')
  }
}

const getMatchScore = async () => {
  try {
    const response = await axios.get(`/api/fan/match/${matchId}/rating/me`)
    if (response.data.code === 200 && response.data.data) {
      userScore.value = response.data.data.stars
      userScoreTime.value = response.data.data.createTime
    }
  } catch (error) {
    console.error('获取用户评分失败:', error)
  }
}

const getAverageScore = async () => {
  try {
    const response = await axios.get(`/api/fan/match/${matchId}/rating/summary`)
    if (response.data.code === 200) {
      averageScore.value = response.data.data.avg
      ratingCount.value = response.data.data.count
    }
  } catch (error) {
    console.error('获取平均评分失败:', error)
  }
}

const submitScore = async () => {
  if (score.value === 0) {
    ElMessage.warning('请选择评分')
    return
  }

  try {
    const response = await axios.post(`/api/fan/match/${matchId}/rating`, {
      stars: score.value
    })
    if (response.data.code === 200) {
      ElMessage.success('评分成功')
      await getMatchScore()
      await getAverageScore()
    }
  } catch (error) {
    console.error('评分失败:', error)
    ElMessage.error(error.response?.data?.message || '评分失败')
  }
}

const getComments = async () => {
  try {
    const response = await axios.get(`/api/fan/match/${matchId}/comments`)
    if (response.data.code === 200) {
      const all = response.data.data || []
      total.value = all.length
      const start = (pageNum.value - 1) * pageSize.value
      comments.value = all.slice(start, start + pageSize.value)
    }
  } catch (error) {
    console.error('获取评论失败:', error)
    ElMessage.error('获取评论失败')
  }
}

const loadCommentReplies = async (commentId) => {
  try {
    const res = await axios.get(`/api/fan/comment/${commentId}/replies`)
    if (res.data.code === 200) {
      replies.value = res.data.data || []
    } else {
      replies.value = []
    }
  } catch (e) {
    console.error('加载回复失败:', e)
    replies.value = []
  }
}

const openReplyDialog = async (comment) => {
  activeComment.value = comment
  replyDialogVisible.value = true
  replyForm.value.parentReplyId = null
  replyForm.value.replyToUserId = comment.userId
  replyForm.value.content = ''
  replyTargetUsername.value = comment.nickname || comment.username || '用户'
  await loadCommentReplies(comment.id)
}

const getReplyDepth = (r) => {
  let depth = 0
  let cur = r
  while (cur && cur.parentReplyId) {
    depth += 1
    cur = replyById.value.get(cur.parentReplyId)
    if (depth > 20) break
  }
  return depth
}

const replyTo = (r) => {
  replyForm.value.parentReplyId = r.id
  replyForm.value.replyToUserId = r.replyUserId
  replyTargetUsername.value = r.replyUsername || r.username || '用户'
  replyForm.value.content = ''
}

const submitReply = async () => {
  if (!activeComment.value?.id) return
  const content = (replyForm.value.content || '').trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    replySubmitting.value = true
    const payload = {
      content,
      parentReplyId: replyForm.value.parentReplyId,
      replyToUserId: replyForm.value.replyToUserId
    }
    const res = await axios.post(`/api/fan/comment/${activeComment.value.id}/reply`, payload)
    if (res.data.code === 200) {
      ElMessage.success('回复成功')
      replyForm.value.content = ''
      await loadCommentReplies(activeComment.value.id)
    } else {
      ElMessage.error(res.data.message || '回复失败')
    }
  } catch (e) {
    console.error('回复失败:', e)
    ElMessage.error(e.response?.data?.message || '回复失败')
  } finally {
    replySubmitting.value = false
  }
}

const loadMe = async () => {
  try {
    const res = await axios.get('/api/auth/info')
    if (res.data.code === 200 && res.data.data) {
      myUserId.value = res.data.data.id
    }
  } catch (e) {
    myUserId.value = null
  }
}

const submitComment = async () => {
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    const response = await axios.post(`/api/fan/match/${matchId}/comment`, {
      content: commentForm.value.content
    })
    if (response.data.code === 200) {
      ElMessage.success('评论成功')
      commentForm.value.content = ''
      pageNum.value = 1
      await getComments()
    }
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error(error.response?.data?.message || '评论失败')
  }
}

const likeComment = async (commentId) => {
  try {
    const response = await axios.put(`/api/fan/like/${commentId}`, {})
    if (response.data.code === 200) {
      await getComments()
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error(error.response?.data?.message || '点赞失败')
  }
}

const handlePageChange = (currentPage) => {
  pageNum.value = currentPage
  getComments()
}

onMounted(() => {
  getMatchDetail()
  getMatchScore()
  getAverageScore()
  getComments()
  loadMe()
})
</script>

<style scoped>
.match-detail {
  padding: 20px;
}

.match-detail h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.match-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 0;
}

.match-time {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #666;
}

.match-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  border-top: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
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
  width: 50px;
  height: 50px;
  background-color: #e8e8e8;
  border-radius: 50%;
}

.team-name {
  font-size: 18px;
  font-weight: bold;
}

.score {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 32px;
  font-weight: bold;
  color: #333;
}

.pending {
  font-size: 18px;
  color: #909399;
}

.separator {
  font-size: 24px;
  color: #999;
}

.match-stats {
  display: flex;
  gap: 30px;
  font-size: 14px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.stat-label {
  color: #666;
}

.stat-value {
  font-weight: bold;
  color: #333;
}

.match-rating {
  padding: 20px 0;
}

.rating-info,
.rating-form,
.average-rating {
  margin-bottom: 20px;
}

.rating-time,
.rating-count {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.comment-section {
  padding: 20px 0;
}

.comment-list {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
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

.comment-content {
  margin-bottom: 15px;
  line-height: 1.5;
}

.comment-footer {
  display: flex;
  gap: 20px;
}

.reply-dialog-meta {
  margin-bottom: 12px;
}

.reply-dialog-line {
  margin-bottom: 6px;
}

.meta-label {
  color: #666;
  margin-right: 6px;
}

.meta-value {
  color: #333;
}

.reply-list {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  max-height: 360px;
  overflow: auto;
}

.reply-item {
  margin-bottom: 12px;
}

.reply-top {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.reply-user {
  font-weight: 600;
}

.reply-to {
  color: #666;
  font-size: 13px;
}

.reply-time {
  color: #999;
  font-size: 12px;
  margin-left: auto;
}

.reply-content {
  margin-top: 6px;
  line-height: 1.5;
}

.reply-actions {
  margin-top: 6px;
}

.reply-editor {
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  margin-top: 12px;
}

.reply-target-line {
  margin-bottom: 10px;
}

.reply-editor-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .match-detail {
    padding: 10px;
  }
  
  .match-score {
    flex-direction: column;
    gap: 20px;
  }
  
  .team {
    flex-direction: row;
    width: 100%;
    justify-content: space-between !important;
  }
  
  .score {
    font-size: 24px;
  }
  
  .match-stats {
    flex-direction: column;
    gap: 10px;
  }
}
</style>