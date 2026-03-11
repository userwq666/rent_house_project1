<template>
  <div class="home">
    <div class="topbar">
      <div class="logo" @click="router.push('/home')">
        <el-icon :size="24" color="#4f46e5"><House /></el-icon>
        <span>EasyRent</span>
      </div>
      <div class="nav" v-if="isLoggedIn">
        <el-button text :class="{ active: $route.path === '/home' }" @click="router.push('/home')">首页</el-button>
        <el-button v-if="auth.userType === 'USER'" text @click="router.push('/my-houses')">我的房源</el-button>
        <el-button v-if="auth.userType === 'USER'" text @click="router.push('/my-contracts')">我的合同</el-button>
        <el-button text @click="router.push('/messages')">
          消息中心
          <el-badge v-if="unreadCount > 0" :value="unreadCount" class="badge-item" />
        </el-button>
        <el-button v-if="auth.userType === 'ADMIN'" text type="danger" @click="router.push('/admin')">管理后台</el-button>
        <el-button v-if="auth.userType === 'STAFF'" text type="warning" @click="router.push('/staff')">业务后台</el-button>
        <el-button text @click="router.push('/profile')">个人中心</el-button>
      </div>
      <div class="actions">
        <template v-if="isLoggedIn">
          <span class="username">Hi, {{ auth.username }}</span>
          <el-button type="primary" size="small" class="glow" @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" class="glow" @click="$router.push('/login')">登录</el-button>
          <el-button text @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>

    <div class="hero">
      <div class="hero-text">
        <div class="eyebrow">智能租赁平台</div>
        <h1>寻找你的<br />理想居所</h1>
        <p class="sub">连接房东与租客，提供安全、透明、便捷的房屋租赁体验。从这里开始你的新生活。</p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" class="glow" @click="scrollToSearch">开始找房</el-button>
          <el-button v-if="auth.userType === 'USER'" size="large" @click="$router.push('/house/publish')">我是房东，发布房源</el-button>
          <el-button v-if="auth.userType === 'STAFF'" size="large" type="warning" @click="$router.push('/staff')">进入业务后台</el-button>
        </div>
      </div>
    </div>

    <div class="search" id="search-anchor">
      <div class="search-form">
        <el-form-item>
          <el-input v-model="searchForm.city" placeholder="区域/城市" :prefix-icon="Location" style="width: 160px" />
        </el-form-item>
        <div class="divider">|</div>
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="关键词 / #ID / id:ID" :prefix-icon="Search" style="width: 240px" />
        </el-form-item>
        <div class="divider">|</div>
        <el-form-item>
          <el-select v-model="searchForm.priceRange" placeholder="价格范围" style="width: 118px">
            <el-option label="1000以下" value="0-1000" />
            <el-option label="1000-2000" value="1000-2000" />
            <el-option label="2000-4000" value="2000-4000" />
            <el-option label="4000以上" value="4000-999999" />
          </el-select>
        </el-form-item>
        <div class="divider">|</div>
        <el-form-item>
          <el-select v-model="searchForm.areaRange" placeholder="面积范围" style="width: 118px">
            <el-option label="50㎡以下" value="0-50" />
            <el-option label="50-80㎡" value="50-80" />
            <el-option label="80-120㎡" value="80-120" />
            <el-option label="120㎡以上" value="120-99999" />
          </el-select>
        </el-form-item>
        <div class="divider">|</div>
        <el-form-item>
          <el-select v-model="searchForm.houseType" placeholder="户型" style="width: 106px">
            <el-option label="一居室" value="1居" />
            <el-option label="一室一厅" value="1室1厅" />
            <el-option label="两室一厅" value="2室1厅" />
            <el-option label="三室一厅" value="3室1厅" />
            <el-option label="三室两厅" value="3室2厅" />
            <el-option label="四室两厅" value="4室2厅" />
          </el-select>
        </el-form-item>
        <div class="divider">|</div>
        <el-form-item>
          <el-select v-model="searchForm.sortBy" placeholder="排序" style="width: 120px">
            <el-option label="最新发布" value="latest" />
            <el-option label="租金从低到高" value="price_asc" />
            <el-option label="租金从高到低" value="price_desc" />
            <el-option label="面积从小到大" value="area_asc" />
            <el-option label="面积从大到小" value="area_desc" />
          </el-select>
        </el-form-item>
        <el-button type="primary" class="glow" :loading="searching" @click="handleSearch">搜索</el-button>
        <el-button :disabled="searching" @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="houses">
      <div class="section-head">
        <h3>热门房源</h3>
        <p class="muted">为您精选的优质好房</p>
      </div>

      <div class="house-grid-container" v-loading="loading">
        <div v-for="house in houses" :key="house.id" class="house-card" @click="goToDetail(house.id)">
          <div class="house-thumb">
            <img :src="getImageList(house.images)[0]" class="thumb-img" />
          </div>
          <div class="house-body">
            <div class="house-title">{{ house.title }}</div>
            <div class="house-address">
              {{ house.district }} · {{ house.address }}
            </div>
            <div class="house-meta">
              <div class="meta-tag">{{ house.houseType }}</div>
              <div class="meta-tag">{{ house.area }}㎡</div>
              <div class="meta-tag">{{ house.floor }}层</div>
            </div>
            <div class="house-footer">
              <div class="price">
                ¥{{ Number(house.rentPrice) || 0 }}<span class="unit">/月</span>
              </div>
              <el-tag size="small" :type="house.status === 'AVAILABLE' ? 'success' : 'info'">
                {{ getStatusText(house.status) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && houses.length === 0" description="暂无房源" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, Search, Location } from '@element-plus/icons-vue'
import { getAvailableHouses, searchHouses } from '../api/house'
import { getUnreadMessageCount } from '../api/messages'

const router = useRouter()
const route = useRoute()
const houses = ref([])
const loading = ref(false)
const searching = ref(false)
const unreadCount = ref(0)

const searchForm = reactive({
  city: '',
  keyword: '',
  priceRange: '',
  areaRange: '',
  houseType: '',
  sortBy: 'latest'
})

const auth = reactive({
  token: sessionStorage.getItem('token'),
  username: sessionStorage.getItem('username'),
  userType: sessionStorage.getItem('accountType') || sessionStorage.getItem('userType')
})

const isLoggedIn = computed(() => !!auth.token)

const syncAuth = () => {
  auth.token = sessionStorage.getItem('token')
  auth.username = sessionStorage.getItem('username')
  auth.userType = sessionStorage.getItem('accountType') || sessionStorage.getItem('userType')
}

// 获取未读消息数
const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return
  
  try {
    const { data } = await getUnreadMessageCount()
    unreadCount.value = data || 0
  } catch (error) {
    console.error('获取未读消息数失败:', error)
  }
}

// 处理未读消息数变化事件
const handleUnreadCountChange = (event) => {
  unreadCount.value = event.detail || 0
}

const fetchHouses = async ({ showLoading = false } = {}) => {
  try {
    if (showLoading) {
      loading.value = true
    }
    const { data } = await getAvailableHouses()
    houses.value = data || []
  } catch (error) {
    ElMessage.error('获取房源失败')
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

const handleSearch = async () => {
  try {
    searching.value = true
    let min = null
    let max = null
    let minArea = null
    let maxArea = null
    if (searchForm.priceRange) {
      const [a, b] = searchForm.priceRange.split('-')
      min = Number(a)
      max = Number(b)
    }
    if (searchForm.areaRange) {
      const [a, b] = searchForm.areaRange.split('-')
      minArea = Number(a)
      maxArea = Number(b)
    }
    
    const { data } = await searchHouses({
      district: searchForm.city || null,
      minPrice: min,
      maxPrice: max,
      keyword: searchForm.keyword || null,
      houseType: searchForm.houseType || null,
      minArea,
      maxArea,
      sortBy: searchForm.sortBy || 'latest'
    })
    houses.value = data || []
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const handleReset = async () => {
  searchForm.city = ''
  searchForm.keyword = ''
  searchForm.priceRange = ''
  searchForm.areaRange = ''
  searchForm.houseType = ''
  searchForm.sortBy = 'latest'
  await handleSearch()
}

const scrollToSearch = () => {
  document.getElementById('search-anchor')?.scrollIntoView({ behavior: 'smooth' })
}

const goToDetail = (id) => {
  router.push(`/house/${id}`)
}

const handleLogout = () => {
  sessionStorage.clear()
  syncAuth()
  ElMessage.success('已退出')
  router.push('/login')
}

const getStatusText = (status) => {
  const map = { AVAILABLE: '可租', RENTED: '已租', OFFLINE: '下架' }
  return map[status] || status
}

const getImageList = (images) => {
  const placeholder = 'https://via.placeholder.com/400x240?text=House'
  if (!images) return [placeholder]
  const arr = images.includes('|')
    ? images.split('|').map((s) => s.trim()).filter(Boolean)
    : [images.trim()]
  return arr.length ? arr : [placeholder]
}

onMounted(() => {
  fetchHouses({ showLoading: true })
  fetchUnreadCount()
  window.addEventListener('storage', syncAuth)
  window.addEventListener('unreadcountchange', handleUnreadCountChange)
})

onUnmounted(() => {
  window.removeEventListener('storage', syncAuth)
  window.removeEventListener('unreadcountchange', handleUnreadCountChange)
})
</script>

<style scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px 60px;
}

/* Topbar - 简化导航栏 */
.topbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 30px;
  margin-bottom: 30px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
  font-size: 22px;
  color: #FF6600;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.05);
}

.nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav .el-button {
  color: #333;
  font-weight: 600;
  padding: 10px 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.nav .el-button:hover {
  color: #FF6600;
  background: rgba(255, 102, 0, 0.05);
}

.nav .el-button.active {
  color: #FF6600;
  background: rgba(255, 102, 0, 0.1);
}

.badge-item {
  margin-left: 6px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.username {
  color: #666;
  font-size: 15px;
  font-weight: 600;
  margin-right: 8px;
}

/* Hero Section - 简化的英雄区域 */
.hero {
  position: relative;
  padding: 100px 60px;
  margin-bottom: 50px;
  background: linear-gradient(135deg, #FF6600 0%, #FF8533 100%);
  overflow: hidden;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(255, 102, 0, 0.3);
  animation: fadeInUp 0.8s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero::before {
  content: "";
  position: absolute;
  top: -50%;
  right: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0) 70%);
  border-radius: 50%;
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-30px, 30px);
  }
}

.hero-text {
  position: relative;
  z-index: 1;
  max-width: 700px;
  color: white;
}

.eyebrow {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 20px;
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.hero-text h1 {
  font-size: 56px;
  line-height: 1.1;
  font-weight: 900;
  margin-bottom: 20px;
  color: white;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
  animation: slideInLeft 0.8s ease 0.2s both;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.sub {
  font-size: 20px;
  line-height: 1.6;
  margin-bottom: 40px;
  opacity: 0.95;
  animation: slideInLeft 0.8s ease 0.4s both;
}

.hero-buttons {
  display: flex;
  gap: 20px;
  animation: fadeInUp 0.8s ease 0.6s both;
}

/* Search Section - 简洁搜索框 */
.search {
  padding: 24px 28px;
  margin-bottom: 60px;
  background: white;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  border-radius: 16px;
  animation: fadeInDown 0.6s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.search-form {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.search-form .el-form-item {
  margin-bottom: 0;
  margin-right: 0;
}

.divider {
  color: #ddd;
  margin: 0 4px;
}

/* Houses Grid - 卡片式布局 */
.section-head {
  margin-bottom: 30px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.section-head h3 {
  font-size: 28px;
  font-weight: 800;
  color: #1A1A1A;
  position: relative;
  padding-bottom: 10px;
}

.section-head h3::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 0;
  width: 60px;
  height: 4px;
  background: linear-gradient(90deg, #FF6600 0%, #FF8533 100%);
  border-radius: 2px;
}

.section-head .muted {
  color: #999;
  margin: 0 0 0 15px;
  font-size: 15px;
}

.house-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 30px;
}

.house-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  height: 100%;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  animation: scaleIn 0.6s ease backwards;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.house-card:nth-child(1) { animation-delay: 0.1s; }
.house-card:nth-child(2) { animation-delay: 0.2s; }
.house-card:nth-child(3) { animation-delay: 0.3s; }
.house-card:nth-child(4) { animation-delay: 0.4s; }
.house-card:nth-child(5) { animation-delay: 0.5s; }
.house-card:nth-child(6) { animation-delay: 0.6s; }

.house-card:hover {
  transform: translateY(-10px) scale(1.02);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.house-thumb {
  position: relative;
  overflow: hidden;
  height: 220px;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}

.house-card:hover .thumb-img {
  transform: scale(1.1);
}

.house-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.house-title {
  font-size: 20px;
  font-weight: 700;
  color: #1A1A1A;
  margin-bottom: 10px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-address {
  color: #666;
  font-size: 14px;
  margin-bottom: 15px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.house-meta {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.meta-tag {
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.meta-tag:hover {
  background: rgba(255, 102, 0, 0.1);
  color: #FF6600;
}

.house-footer {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.price {
  color: #FF6600;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  transition: transform 0.3s ease;
}

.house-card:hover .price {
  transform: scale(1.1);
}

.unit {
  font-size: 14px;
  color: #999;
  margin-left: 4px;
  font-weight: 500;
}

/* Loading 动画 */
:deep(.el-loading-spinner) {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

/* Responsive */
@media (max-width: 768px) {
  .home {
    padding: 0 15px 30px;
  }
  
  .topbar {
    padding: 12px 20px;
  }
  
  .hero {
    padding: 60px 30px;
  }
  
  .hero-text h1 {
    font-size: 36px;
  }
  
  .sub {
    font-size: 16px;
  }
  
  .house-grid-container {
    grid-template-columns: 1fr;
  }

  .search {
    padding: 16px;
  }

  .search-form {
    flex-wrap: wrap;
    white-space: normal;
    gap: 8px;
  }

  .search-form :deep(.el-form-item) {
    width: 100%;
  }

  .divider {
    display: none;
  }
}
</style>
