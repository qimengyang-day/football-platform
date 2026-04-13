<template>
  <div class="profile">
    <h1>个人档案</h1>

    <el-alert
      v-if="submitStatus && submitStatus.status === 'PENDING'"
      title="你有一条待管理员审核的档案修改申请"
      type="warning"
      show-icon
      style="margin-bottom: 12px"
    />
    <el-alert
      v-else-if="submitStatus && submitStatus.status === 'APPROVED'"
      :title="`档案修改已通过：${submitStatus.adminRemark || '无备注'}`"
      type="success"
      show-icon
      style="margin-bottom: 12px"
    />
    <el-alert
      v-else-if="submitStatus && submitStatus.status === 'REJECTED'"
      :title="`档案修改被拒绝：${submitStatus.adminRemark || '无备注'}`"
      type="error"
      show-icon
      style="margin-bottom: 12px"
    />
    <el-card>
      <template #header>
        <div class="card-header">
          <span>球员信息</span>
          <el-button type="primary" @click="editMode = !editMode">{{ editMode ? '取消' : '编辑' }}</el-button>
        </div>
      </template>
      <el-form :model="playerInfo" :rules="rules" ref="playerFormRef" label-width="100px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="playerInfo.realName" :disabled="!editMode" />
        </el-form-item>
        <el-form-item label="身高 (cm)" prop="height">
          <el-input v-model.number="playerInfo.height" :disabled="!editMode" type="number" />
        </el-form-item>
        <el-form-item label="体重 (kg)" prop="weight">
          <el-input v-model.number="playerInfo.weight" :disabled="!editMode" type="number" />
        </el-form-item>
        <el-form-item label="位置" prop="position">
          <el-select v-model="playerInfo.position" :disabled="!editMode" placeholder="请选择位置">
            <el-option label="前锋" value="前锋" />
            <el-option label="中场" value="中场" />
            <el-option label="后卫" value="后卫" />
            <el-option label="门将" value="门将" />
          </el-select>
        </el-form-item>
        <el-form-item label="身价(万元)" prop="marketValue">
          <el-input v-model.number="playerInfo.marketValue" :disabled="!editMode" type="number" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input v-model.number="playerInfo.age" :disabled="!editMode" type="number" />
        </el-form-item>
        <el-form-item label="国籍" prop="nationality">
          <el-input v-model="playerInfo.nationality" :disabled="!editMode" />
        </el-form-item>
        <el-form-item label="电话号" prop="phone">
          <el-input v-model="playerInfo.phone" :disabled="!editMode" />
        </el-form-item>
        <el-form-item v-if="editMode">
          <el-button type="primary" @click="submitProfileForAudit">提交审核</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>审核状态</span>
        </div>
      </template>
      <div class="status-info">
        <el-alert
          :title="`加入状态: ${playerInfo.joinStatus || '无'}`"
          :type="getAlertType()"
          show-icon
        />
        <div v-if="playerInfo.adminRemark" class="remark">
          <strong>管理员备注:</strong> {{ playerInfo.adminRemark }}
        </div>
        <div v-if="playerInfo.clubRemark" class="remark">
          <strong>俱乐部备注:</strong> {{ playerInfo.clubRemark }}
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>统计数据</span>
        </div>
      </template>
      <div class="stats">
        <div class="stat-item">
          <div class="stat-value">{{ playerInfo.goals || 0 }}</div>
          <div class="stat-label">进球</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ playerInfo.assists || 0 }}</div>
          <div class="stat-label">助攻</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const editMode = ref(false)
const playerFormRef = ref(null)
const submitStatus = ref(null)
const playerInfo = ref({
  realName: '',
  height: '',
  weight: '',
  position: '',
  age: null,
  nationality: '',
  phone: '',
  joinStatus: null,
  applyTeamId: null,
  applyReason: '',
  adminRemark: '',
  clubRemark: '',
  goals: 0,
  assists: 0,
  marketValue: 0
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  height: [{ required: true, message: '请输入身高', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入体重', trigger: 'blur' }],
  position: [{ required: true, message: '请选择位置', trigger: 'change' }]
}

const getAlertType = () => {
  switch (playerInfo.value.joinStatus) {
    case '待审核':
      return 'warning'
    case '已审核':
      return 'success'
    case '拒绝':
      return 'error'
    default:
      return 'info'
  }
}

const getPlayerInfo = async () => {
  try {
    // 获取当前登录用户的球员档案
    const response = await axios.get('/api/player/me')
    if (response.data.code === 200) {
      playerInfo.value = response.data.data
      console.log('获取到的球员信息:', playerInfo.value)
      console.log('球员ID:', playerInfo.value.id)
    }
  } catch (error) {
    console.error('获取球员信息失败:', error)
  }
}

const getSubmitStatus = async () => {
  try {
    const res = await axios.get('/api/player/profile/submit/status')
    if (res.data.code === 200) submitStatus.value = res.data.data
  } catch (e) {
    // ignore
  }
}

const submitProfileForAudit = async () => {
  if (!playerFormRef.value) return
  await playerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const response = await axios.post('/api/player/profile/submit', playerInfo.value)
        console.log('Response:', response)
        if (response.data.code === 200) {
          ElMessage.success('已提交管理员审核')
          editMode.value = false
          await getSubmitStatus()
        }
      } catch (error) {
        console.error('更新失败:', error)
        console.error('Error response:', error.response)
        ElMessage.error(error.response?.data?.message || '提交失败')
      }
    }
  })
}

onMounted(() => {
  getPlayerInfo()
  getSubmitStatus()
})
</script>

<style scoped>
.profile {
  padding: 20px;
}

.profile h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-info {
  padding: 20px;
}

.remark {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.stats {
  display: flex;
  gap: 40px;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 8px;
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #52c41a;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}
</style>