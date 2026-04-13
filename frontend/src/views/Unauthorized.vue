<template>
  <div class="unauthorized">
    <h1>401 - 未授权</h1>
    <p>您没有权限访问此页面</p>
    <el-button type="primary" @click="goToLogin">返回登录</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.unauthorized {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.unauthorized h1 {
  font-size: 48px;
  color: #ff4d4f;
  margin-bottom: 20px;
}

.unauthorized p {
  font-size: 18px;
  color: #666;
  margin-bottom: 30px;
}
</style>