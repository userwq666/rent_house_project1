<template>
  <div class="canvas">
    <div class="orb orb-a"></div>
    <div class="orb orb-b"></div>
    <div class="register-container glass">
      <div class="card-header">
        <h2>{{ isStaffRegister ? '创建业务员账号' : '创建账号' }}</h2>
        <p>{{ isStaffRegister ? '由管理员发起的业务员注册流程' : '开始你的租房与管理流程' }}</p>
      </div>

      <el-form ref="registerFormRef" :model="registerForm" :rules="rules" label-width="90px" class="register-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item label="姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" :prefix-icon="Phone" />
        </el-form-item>

        <el-form-item v-if="!isStaffRegister" label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" :prefix-icon="Message" />
        </el-form-item>

        <el-form-item v-if="!isStaffRegister" label="身份证" prop="idCard">
          <el-input v-model="registerForm.idCard" placeholder="请输入身份证号（可选）" />
        </el-form-item>

        <el-button type="primary" :loading="loading" class="glow primary-btn" @click="handleRegister">
          {{ isStaffRegister ? '创建业务员' : '注册' }}
        </el-button>

        <div class="actions">
          <el-button v-if="isStaffRegister" text type="primary" @click="$router.push('/admin')">返回管理后台</el-button>
          <el-button v-else text type="primary" @click="$router.push('/login')">已有账号？去登录</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Message } from '@element-plus/icons-vue'
import { register } from '../api/auth'

const router = useRouter()
const route = useRoute()
const registerFormRef = ref(null)
const loading = ref(false)

const targetAccountType = computed(() => {
  const value = String(route.query.accountType || '').toUpperCase()
  return value === 'STAFF' ? 'STAFF' : 'USER'
})
const isStaffRegister = computed(() => targetAccountType.value === 'STAFF')

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  idCard: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: isStaffRegister.value
    ? []
    : [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
      ]
}))

const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
    loading.value = true

    const payload = {
      username: registerForm.username,
      password: registerForm.password,
      accountType: targetAccountType.value,
      realName: registerForm.realName,
      phone: registerForm.phone,
      email: isStaffRegister.value ? null : registerForm.email,
      idCard: isStaffRegister.value ? null : registerForm.idCard || null
    }

    const { data } = await register(payload)
    if (data.message?.includes('成功')) {
      ElMessage.success(isStaffRegister.value ? '业务员创建成功' : '注册成功，请登录')
      if (isStaffRegister.value) {
        router.push('/admin')
      } else {
        router.push('/login')
      }
      return
    }
    ElMessage.error(data.message || '注册失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.response?.data || '注册失败，请稍后重试')
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
  padding: 20px;
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

.register-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 540px;
  padding: 44px;
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
  margin-bottom: 30px;
}

.card-header h2 {
  margin: 0 0 10px 0;
  font-size: 30px;
  font-weight: 800;
  color: #1a1a1a;
}

.card-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.primary-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 16px;
  margin-top: 8px;
  font-weight: 700;
}

.actions {
  text-align: center;
  margin-top: 6px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #333;
}

:deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 2px rgba(255, 102, 0, 0.2) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #ff6600 inset !important;
}

@media (max-width: 768px) {
  .register-container {
    padding: 28px;
  }

  .card-header h2 {
    font-size: 24px;
  }
}
</style>
