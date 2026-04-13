<template>
  <div class="platform-contact">
    <h1>平台联系方式管理</h1>
    
    <el-card>
      <template #header>
        <div class="card-header">
          <span>联系我们信息</span>
        </div>
      </template>
      
      <el-form :model="contactForm" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="官方微信" prop="officialWechat">
          <el-input v-model="contactForm.officialWechat" placeholder="请输入官方微信号" />
        </el-form-item>
        
        <el-form-item label="官方邮箱" prop="officialEmail">
          <el-input v-model="contactForm.officialEmail" placeholder="请输入官方邮箱地址" />
        </el-form-item>
        
        <el-form-item label="联系电话" prop="officialPhone">
          <el-input v-model="contactForm.officialPhone" placeholder="请输入联系电话" />
        </el-form-item>
        
        <el-form-item label="官方QQ" prop="officialQq">
          <el-input v-model="contactForm.officialQq" placeholder="请输入官方QQ号" />
        </el-form-item>
        
        <el-form-item label="官方网站" prop="officialWebsite">
          <el-input v-model="contactForm.officialWebsite" placeholder="请输入官方网站地址" />
        </el-form-item>
        
        <el-form-item label="备注" prop="remark">
          <el-input v-model="contactForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="saveContact">保存</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from '@/utils/axios'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const loading = ref(false)

const contactForm = ref({
  id: null,
  officialWechat: '',
  officialEmail: '',
  officialPhone: '',
  officialQq: '',
  officialWebsite: '',
  remark: ''
})

const rules = {
  officialWechat: [{ required: true, message: '请输入官方微信号', trigger: 'blur' }],
  officialEmail: [
    { required: true, message: '请输入官方邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  officialPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const getContact = async () => {
  try {
    const response = await axios.get('/api/admin/contact')
    if (response.data.code === 200 && response.data.data) {
      contactForm.value = {
        id: response.data.data.id,
        officialWechat: response.data.data.officialWechat || '',
        officialEmail: response.data.data.officialEmail || '',
        officialPhone: response.data.data.officialPhone || '',
        officialQq: response.data.data.officialQq || '',
        officialWebsite: response.data.data.officialWebsite || '',
        remark: response.data.data.remark || ''
      }
    }
  } catch (error) {
    console.error('获取联系方式失败:', error)
    ElMessage.error('获取联系方式失败')
  }
}

const saveContact = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.put('/api/admin/contact/update', contactForm.value)
        if (response.data.code === 200) {
          ElMessage.success('保存成功')
        } else {
          ElMessage.error(response.data.message || '保存失败')
        }
      } catch (error) {
        console.error('保存联系方式失败:', error)
        ElMessage.error(error.response?.data?.message || '保存失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  getContact()
})
</script>

<style scoped>
.platform-contact {
  padding: 20px;
}

.platform-contact h1 {
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
