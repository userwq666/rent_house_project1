<template>
  <div class="profile-container">
    <div class="header">
      <div>
        <div class="title">个人信息</div>
        <div class="sub">管理您的账户信息</div>
      </div>
      <el-button text @click="$router.push('/home')">返回首页</el-button>
    </div>

    <div class="profile-card" v-loading="loading">
      <el-form :model="form" :rules="rules" ref="profileFormRef" label-width="100px" class="profile-form">
        <div class="avatar-section">
          <div class="avatar-placeholder">{{ form.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
          <div class="user-meta">
            <div class="username">{{ form.username }}</div>
            <el-tag :type="form.accountType === 'ADMIN' ? 'danger' : 'success'" size="small">
              {{ form.accountType === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号（可选）" />
        </el-form-item>
        
        <div class="actions">
          <el-button type="primary" class="glow" :loading="saving" @click="handleSave">保存信息</el-button>
          <el-button text @click="loadProfile">取消更改</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '../api/auth'

const profileFormRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  username: '',
  accountType: '',
  realName: '',
  phone: '',
  email: '',
  idCard: ''
})

const rules = reactive({
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度应在 2-50 个字符之间', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  idCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ]
})

const loadProfile = async () => {
  try {
    loading.value = true
    const { data } = await getProfile()
    form.username = data.username
    form.accountType = data.accountType
    form.realName = data.realName || ''
    form.phone = data.phone || ''
    form.email = data.email || ''
    form.idCard = data.idCard || ''
  } catch (error) {
    ElMessage.error(error.response?.data || '获取个人信息失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  try {
    await profileFormRef.value.validate()
    saving.value = true
    await updateProfile({
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      idCard: form.idCard
    })
    ElMessage.success('已更新个人信息')
    loadProfile()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '更新失败')
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-container {
  width: 100%;
  max-width: 1600px;
  margin: 20px auto;
  padding: 0 20px;
  box-sizing: border-box;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  margin-bottom: 32px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  min-width: 1000px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.sub {
  color: #909399;
  font-size: 14px;
}

.profile-card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e4e7ed;
  min-width: 1000px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent) 0%, var(--accent-hover) 100%);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.2);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.username {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

.actions {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.actions .el-button--primary {
  height: 40px;
  padding: 0 24px;
}

/* Form Styles */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text);
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: 8px;
  padding: 4px 12px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--text-light) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}
</style>
