<template>
  <div class="detail">
    <!-- Floating Back Button -->
    <button class="back-btn" @click="$router.push('/home')">
      <el-icon :size="20"><ArrowLeft /></el-icon>
    </button>

    <!-- Left: Media Gallery (Full Screen) -->
    <div class="media">
      <el-carousel height="100%" indicator-position="none" v-if="imageList.length > 1">
        <el-carousel-item v-for="(img, idx) in imageList" :key="idx">
          <el-image
            :src="img"
            fit="cover"
            class="carousel-img"
            @click="openViewer(idx)"
          />
        </el-carousel-item>
      </el-carousel>
      <el-image
        v-else
        :src="imageList[0] || placeholder"
        fit="cover"
        class="single-img"
        @click="openViewer(0)"
      />
    </div>

    <!-- Right: Info Panel (Scrollable) -->
    <div class="info-panel">
      <div class="info-header">
        <div class="eyebrow">房源详情</div>
        <h1 class="title">{{ house?.title || '加载中...' }}</h1>
        <div class="address">
          <el-icon><Location /></el-icon>
          {{ house?.district }} · {{ house?.address }}
        </div>
      </div>

      <div class="info-content">
        <div class="price-card">
          <div>
            <span class="price">¥{{ Number(house?.rentPrice) || 0 }}</span>
            <span class="unit">/ 月</span>
          </div>
          <el-tag :type="house?.status === 'AVAILABLE' ? 'success' : 'info'" effect="dark">
            {{ getStatusText(house?.status) }}
          </el-tag>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span class="meta-label">户型</span>
            <span class="meta-value">{{ house?.houseType }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">面积</span>
            <span class="meta-value">{{ Number(house?.area) || 0 }} ㎡</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">楼层</span>
            <span class="meta-value">{{ Number(house?.floor) || 0 }}层</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">浏览</span>
            <span class="meta-value">{{ house?.viewCount || 0 }}次</span>
          </div>
        </div>

        <div class="section">
          <div class="section-title">房主信息</div>
          <p>{{ house?.ownerName || '未填写' }}</p>
        </div>

        <div class="section">
          <div class="section-title">关于此房源</div>
          <p>{{ house?.description || '暂无描述' }}</p>
        </div>

        <div class="section">
          <div class="section-title">配套设施</div>
          <p>{{ house?.facilities || '暂无' }}</p>
        </div>
      </div>

      <!-- Sticky Action Bar -->
      <div class="action-bar">
        <template v-if="house?.status === 'AVAILABLE' && isLoggedIn && !isOwner">
          <el-button type="primary" class="glow" @click="showRentDialog = true">
            我要租房
          </el-button>
          <el-button plain @click="goChatWithOwner">
            联系房主
          </el-button>
        </template>
        <el-button v-else-if="!isLoggedIn" type="primary" class="glow" @click="redirectToLogin">
          登录后租房
        </el-button>
        <el-tag v-else-if="isOwner" type="info" style="width: 100%; height: 48px; display: flex; align-items: center; justify-content: center;">
          这是您的房源
        </el-tag>
      </div>
    </div>

    <!-- Rent Dialog -->
    <el-dialog v-model="showRentDialog" title="签订租房合同" width="500px">
      <el-form :model="contractForm" label-width="100px">
        <el-form-item label="月租">
          <el-input-number v-model="contractForm.rentPrice" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="押金">
          <el-input-number v-model="contractForm.deposit" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="租期开端">
          <el-date-picker v-model="contractForm.startDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="租期结束">
          <el-date-picker v-model="contractForm.endDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contractForm.notes" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRentDialog = false">取消</el-button>
        <el-button type="primary" class="glow" @click="handleRent" :loading="rentLoading">确认签约</el-button>
      </template>
    </el-dialog>

    <el-image-viewer
      v-if="viewerVisible"
      :url-list="imageList"
      :initial-index="viewerIndex"
      hide-on-click-modal
      @close="viewerVisible = false"
      @switch="viewerIndex = $event"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Location } from '@element-plus/icons-vue'
import { getHouseById } from '../api/house'
import { createContract } from '../api/contract'

const route = useRoute()
const router = useRouter()
const house = ref(null)
const loading = ref(false)
const showRentDialog = ref(false)
const rentLoading = ref(false)
const imageList = ref([])
const placeholder = 'https://via.placeholder.com/800x600?text=No+Image'

const viewerVisible = ref(false)
const viewerIndex = ref(0)

const contractForm = reactive({
  rentPrice: 0,
  deposit: 0,
  startDate: '',
  endDate: '',
  notes: ''
})

const isLoggedIn = computed(() => !!sessionStorage.getItem('token'))
const currentUserId = computed(() => sessionStorage.getItem('userId'))
const isOwner = computed(() => house.value && house.value.ownerId == currentUserId.value)

const fetchHouse = async () => {
  if (!isLoggedIn.value) {
    redirectToLogin()
    return
  }
  try {
    loading.value = true
    const { data } = await getHouseById(route.params.id)
    house.value = data
    contractForm.rentPrice = Number(data.rentPrice) || 0
    contractForm.deposit = Number(data.deposit) || 0
    imageList.value = parseImages(data.images)
  } catch (error) {
    ElMessage.error('获取房源详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const parseImages = (images) => {
  if (!images) return [placeholder]
  const arr = images.includes('|')
    ? images.split('|').map((s) => s.trim()).filter(Boolean)
    : [images.trim()]
  return arr.length ? arr : [placeholder]
}

const handleRent = async () => {
  if (!contractForm.startDate || !contractForm.endDate) {
    ElMessage.warning('请选择租期')
    return
  }
  try {
    rentLoading.value = true
    await createContract({
      houseId: house.value.id,
      tenantId: Number(sessionStorage.getItem('userId')),
      rentPrice: Number(contractForm.rentPrice),
      deposit: Number(contractForm.deposit),
      startDate: contractForm.startDate,
      endDate: contractForm.endDate,
      notes: contractForm.notes
    })
    ElMessage.success('签约成功')
    showRentDialog.value = false
    router.push('/my-contracts')
  } catch (error) {
    ElMessage.error(error.response?.data || '签约失败')
  } finally {
    rentLoading.value = false
  }
}

const redirectToLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

const goChatWithOwner = () => {
  if (!house.value?.ownerId) {
    ElMessage.warning('未找到房主信息')
    return
  }
  router.push({ path: '/messages', query: { receiverId: house.value.ownerId } })
}

const getStatusText = (status) => {
  const map = {
    AVAILABLE: '可租',
    RENTED: '已租',
    OFFLINE: '下架'
  }
  return map[status] || status
}

const openViewer = (idx) => {
  viewerIndex.value = idx
  viewerVisible.value = true
}

onMounted(() => {
  fetchHouse()
})
</script>

<style scoped>
.detail {
  height: 100vh;
  width: 100vw;
  display: flex;
  overflow: hidden;
  background: #fff;
}

/* Left Side: Media (Full Height, Flex) */
.media {
  flex: 1;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.media .el-carousel,
.media .el-image,
.carousel-img,
.single-img {
  width: 100%;
  height: 100%;
}

.carousel-img,
.single-img {
  object-fit: cover;
}

/* Floating Back Button */
.back-btn {
  position: absolute;
  top: 24px;
  left: 24px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.back-btn:hover {
  transform: scale(1.05);
  background: #fff;
}

/* Right Side: Info Panel (Fixed Width, Scrollable) */
.info-panel {
  width: 400px;
  height: 100%;
  overflow-y: auto;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--border);
  padding: 0; /* Padding handled inside sections */
  flex-shrink: 0;
}

.info-header {
  padding: 32px 32px 24px;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  z-index: 5;
  border-bottom: 1px solid transparent;
  transition: border-color 0.3s;
}

.info-content {
  padding: 0 32px 32px;
  flex: 1;
}

/* Typography */
.eyebrow {
  color: var(--accent);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text);
  line-height: 1.2;
  margin-bottom: 8px;
}

.address {
  color: var(--text-light);
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Price Section */
.price-card {
  margin: 24px 0;
  padding: 20px;
  background: #f8fafc;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: 32px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1;
}

.unit {
  font-size: 14px;
  color: var(--muted);
  margin-left: 2px;
}

/* Meta Grid */
.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 32px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: var(--muted);
}

.meta-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

/* Sections */
.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text);
}

.section p {
  color: var(--text-light);
  line-height: 1.6;
  font-size: 14px;
  margin: 0;
}

/* Bottom Actions (Sticky) */
.action-bar {
  position: sticky;
  bottom: 0;
  padding: 20px 32px;
  background: #fff;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 12px;
}

.action-bar .el-button {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
}

/* Scrollbar for Info Panel */
.info-panel::-webkit-scrollbar {
  width: 6px;
}
.info-panel::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

/* Responsive */
@media (max-width: 960px) {
  .detail {
    flex-direction: column;
    overflow-y: auto;
  }
  
  .media {
    height: 40vh;
    flex: none;
  }
  
  .info-panel {
    width: 100%;
    height: auto;
    border-left: none;
    overflow: visible;
  }
}
</style>
