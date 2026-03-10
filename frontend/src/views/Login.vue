<template>
  <div class="canvas">
    <div class="orb orb-a"></div>
    <div class="orb orb-b"></div>
    <div class="login-container glass">
      <div class="card-header">
        <el-icon :size="42" color="#2563eb"><House /></el-icon>
        <h2>欢迎回来</h2>
        <p>登录后管理你的房源与合同</p>
      </div>
      <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0px" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="用户名" size="large" clearable>
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            placeholder="密码"
            type="password"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button type="primary" size="large" class="glow primary-btn" :loading="loading" @click="handleLogin">
          登录
        </el-button>

        <div class="actions">
          <el-button text type="primary" @click="$router.push('/register')">
            没有账号？去注册
          </el-button>
          <el-button text disabled>忘记密码</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, House } from '@element-plus/icons-vue'
import { login } from '../api/auth'

const router = useRouter()
const route = useRoute()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = ref({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度应在 3-50 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
    loading.value = true

    const { data } = await login(loginForm.value)

    console.log('登录响应:', data)

    if (data.token) {
      sessionStorage.setItem('token', data.token)
      sessionStorage.setItem('userId', data.userId)
      sessionStorage.setItem('username', data.username)
      sessionStorage.setItem('userType', data.userType)
      
      ElMessage.success(data.message || '登录成功')
      
      console.log('用户类型:', data.userType)
      
      if (data.userType === 'ADMIN') {
        console.log('管理员登录，跳转至后台')
        router.push('/admin')
      } else {
        const redirectPath = route.query.redirect || '/home'
        console.log('普通用户登录，跳转至:', redirectPath)
        router.push(redirectPath)
      }
    } else {
      ElMessage.error(data.message || '登录失败')
    }
  } catch (error) {
    console.error('登录错误:', error)
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.canvas {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
  animation: float 8s infinite ease-in-out;
  z-index: 0;
}

.orb-a {
  width: 500px;
  height: 500px;
  top: -150px;
  left: -150px;
  background: radial-gradient(circle, rgba(255, 102, 0, 0.3) 0%, rgba(255, 102, 0, 0) 70%);
  animation-delay: 0s;
}

.orb-b {
  width: 400px;
  height: 400px;
  bottom: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(255, 133, 51, 0.3) 0%, rgba(255, 133, 51, 0) 70%);
  animation-delay: -4s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(30px, 40px);
  }
}

.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 460px;
  padding: 50px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.8) inset;
  animation: slideInUp 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(60px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 40px;
}

.card-header h2 {
  margin: 0 0 10px 0;
  color: #1A1A1A;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.card-header p {
  margin: 0;
  color: #666;
  font-size: 15px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.actions {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}

.primary-btn {
  width: 100%;
  height: 50px;
  font-size: 17px;
  border-radius: 12px;
  letter-spacing: 0.02em;
  font-weight: 700;
  margin-top: 8px;
}

/* Element Plus 输入框增强 */
:deep(.el-input__wrapper) {
  height: 50px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.el-input__inner) {
  font-size: 16px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px rgba(255, 102, 0, 0.2) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #FF6600 inset !important;
}

/* 按钮动画 */
:deep(.el-button) {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-button:active) {
  transform: scale(0.98);
}

/* 响应式 */
@media (max-width: 768px) {
  .login-container {
    padding: 35px;
    margin: 20px;
  }
  
  .card-header h2 {
    font-size: 26px;
  }
}
</style>
