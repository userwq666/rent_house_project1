<template>
  <div class="admin">
    <div class="header glass">
      <div>
        <div class="title">管理后台</div>
        <div class="sub">房源、合同、用户统一控制台</div>
      </div>
      <el-button text @click="$router.push('/home')">返回首页</el-button>
    </div>

    <div class="stats">
      <div class="stat glass" v-for="item in statItems" :key="item.title">
        <div class="stat-title">{{ item.title }}</div>
        <div class="stat-value">{{ item.value }}</div>
      </div>
    </div>

    <div class="panel glass">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="房源" name="houses">
          <el-table :data="houses" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="ownerName" label="房主" width="120" />
            <el-table-column prop="district" label="区域" width="100" />
            <el-table-column prop="rentPrice" label="租金" width="120">
              <template #default="{ row }">
                <span class="price">¥{{ row.rentPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getHouseStatusType(row.status)">
                  {{ getHouseStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="100" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text @click="viewHouse(row.id)">查看</el-button>
                <el-button v-if="row.status !== 'RENTED'" size="small" type="danger" @click="deleteHouseAdmin(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="合同" name="contracts">
          <el-table :data="contracts" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="houseTitle" label="房源" min-width="200" />
            <el-table-column prop="landlordName" label="房主" width="120" />
            <el-table-column prop="tenantName" label="租客" width="120" />
            <el-table-column prop="rentPrice" label="月租" width="120">
              <template #default="{ row }">
                <span class="price">¥{{ row.rentPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column label="租期" width="220">
              <template #default="{ row }">
                {{ formatDate(row.startDate) }} ~ {{ formatDate(row.endDate) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getContractStatusType(row.status)">
                  {{ getContractStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="action-cell">
                  <el-button
                    v-if="row.status === 'PENDING_ADMIN_APPROVAL'"
                    size="small"
                    type="primary"
                    @click="approveContract(row.id)"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="row.status === 'PENDING_ADMIN_APPROVAL'"
                    size="small"
                    type="danger"
                    @click="rejectContract(row.id)"
                  >
                    拒绝
                  </el-button>
                  <el-button
                    v-if="row.status === 'ACTIVE'"
                    size="small"
                    type="danger"
                    @click="terminateContractAdmin(row.id)"
                  >
                    终止
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="用户" name="users">
          <el-table :data="users" v-loading="loading" style="width: 100%">
            <el-table-column prop="userId" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="160" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="accountType" label="角色" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.accountType === 'ADMIN'" type="danger">管理员</el-tag>
                <el-tag v-else>用户</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="允许发布" width="140">
              <template #default="{ row }">
                <el-switch
                  v-model="row.canPublish"
                  active-text="允许"
                  inactive-text="禁用"
                  @change="value => handleRestrictionChange(row, 'canPublish', value)"
                  :disabled="row.accountType === 'ADMIN'"
                />
              </template>
            </el-table-column>
            <el-table-column label="允许租赁" width="140">
              <template #default="{ row }">
                <el-switch
                  v-model="row.canRent"
                  active-text="允许"
                  inactive-text="禁用"
                  @change="value => handleRestrictionChange(row, 'canRent', value)"
                  :disabled="row.accountType === 'ADMIN'"
                />
              </template>
            </el-table-column>
            <el-table-column label="账号状态" width="160">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  active-text="启用"
                  inactive-text="锁定"
                  :disabled="row.accountType === 'ADMIN'"
                  @change="value => handleStatusChange(row, value)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllHousesAdmin, deleteHouse } from '../api/house'
import { getAllContracts, adminTerminateContract, approveContractByAdmin } from '../api/contract'
import { fetchAllUsers, updateUserRestrictions, updateUserStatus } from '../api/admin'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('houses')
const houses = ref([])
const contracts = ref([])
const users = ref([])

// 定时刷新合同状态（每 5 秒）
let contractRefreshInterval = null

const stats = reactive({
  totalHouses: 0,
  availableHouses: 0,
  rentedHouses: 0,
  activeContracts: 0
})

const statItems = computed(() => [
  { title: '总房源', value: stats.totalHouses },
  { title: '可租', value: stats.availableHouses },
  { title: '已租', value: stats.rentedHouses },
  { title: '活跃合同', value: stats.activeContracts }
])

const fetchHouses = async () => {
  try {
    loading.value = true
    const { data } = await getAllHousesAdmin()
    houses.value = data
    updateStats()
  } catch (error) {
    ElMessage.error('获取房源列表失败')
  } finally {
    loading.value = false
  }
}

const fetchContracts = async () => {
  try {
    loading.value = true
    const { data } = await getAllContracts()
    contracts.value = data
    updateStats()
  } catch (error) {
    ElMessage.error('获取合同列表失败')
  } finally {
    loading.value = false
  }
}

const fetchUsers = async () => {
  try {
    loading.value = true
    const { data } = await fetchAllUsers()
    users.value = data
  } catch (error) {
    ElMessage.error(error.response?.data || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const updateStats = () => {
  stats.totalHouses = houses.value.length
  stats.availableHouses = houses.value.filter(h => h.status === 'AVAILABLE').length
  stats.rentedHouses = houses.value.filter(h => h.status === 'RENTED').length
  stats.activeContracts = contracts.value.filter(c => c.status === 'ACTIVE').length
}

const handleTabClick = () => {
  if (activeTab.value === 'houses') {
    fetchHouses()
  } else if (activeTab.value === 'contracts') {
    fetchContracts()
  } else if (activeTab.value === 'users') {
    fetchUsers()
  }
}

// 启动合同状态定时刷新
const startContractRefresh = () => {
  if (contractRefreshInterval) {
    clearInterval(contractRefreshInterval)
  }
  contractRefreshInterval = setInterval(() => {
    if (activeTab.value === 'contracts') {
      fetchContracts()
    }
  }, 5000) // 每 5 秒刷新一次
}

// 停止合同状态定时刷新
const stopContractRefresh = () => {
  if (contractRefreshInterval) {
    clearInterval(contractRefreshInterval)
    contractRefreshInterval = null
  }
}

const viewHouse = (id) => {
  router.push(`/house/${id}`)
}

const deleteHouseAdmin = async (id) => {
  try {
    await ElMessageBox.confirm('管理员删除操作不可恢复，确定删除吗？', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await deleteHouse(id)
    ElMessage.success('删除成功')
    fetchHouses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '删除失败')
    }
  }
}

const terminateContractAdmin = async (id) => {
  try {
    await ElMessageBox.confirm('确定要终止此合同吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminTerminateContract(id)
    ElMessage.success('合同已终止')
    fetchContracts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '操作失败')
    }
  }
}

const approveContract = async (id) => {
  try {
    await ElMessageBox.confirm('确认通过该合同审批吗？', '提示', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await approveContractByAdmin(id)
    ElMessage.success('合同已生效')
    fetchContracts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '操作失败')
    }
  }
}

const rejectContract = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝合同', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning',
      inputPattern: /.+/, 
      inputErrorMessage: '请填写拒绝原因'
    })
    await import('../api/contract').then(async ({ rejectContractByAdmin }) => {
      await rejectContractByAdmin(id, { reason: value })
      ElMessage.success('合同已被拒绝')
      fetchContracts()
    })
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '操作失败')
    }
  }
}

const handleRestrictionChange = async (row, field, value) => {
  try {
    await updateUserRestrictions(row.userId, {
      canPublish: row.canPublish,
      canRent: row.canRent
    })
    ElMessage.success('权限设置已更新')
  } catch (error) {
    ElMessage.error(error.response?.data || '更新权限失败')
    row[field] = !value
  }
}

const handleStatusChange = async (row, value) => {
  try {
    await updateUserStatus(row.userId, { enabled: row.enabled })
    ElMessage.success(row.enabled ? '账号已启用' : '账号已锁定')
  } catch (error) {
    ElMessage.error(error.response?.data || '更新账号状态失败')
    row.enabled = !value
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.split('T')[0]
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').split('.')[0]
}

const getHouseStatusType = (status) => {
  const map = { AVAILABLE: 'success', PENDING: 'warning', RENTED: 'info', OFFLINE: 'warning' }
  return map[status] || ''
}

const getHouseStatusText = (status) => {
  const map = { AVAILABLE: '可租', PENDING: '等待中', RENTED: '已租', OFFLINE: '下架' }
  return map[status] || status
}

const getContractStatusType = (status) => {
  const map = {
    ACTIVE: 'success',
    EXPIRED: 'info',
    TERMINATED: 'danger',
    TERMINATION_PENDING: 'warning',
    PENDING_LANDLORD_APPROVAL: 'warning',
    PENDING_ADMIN_APPROVAL: 'warning'
  }
  return map[status] || ''
}

const getContractStatusText = (status) => {
  const map = {
    ACTIVE: '进行中',
    EXPIRED: '已到期',
    TERMINATED: '已终止',
    TERMINATION_PENDING: '待终止确认',
    PENDING_LANDLORD_APPROVAL: '待房主审批',
    PENDING_ADMIN_APPROVAL: '待管理员审批'
  }
  return map[status] || status
}

onMounted(() => {
  fetchHouses()
  fetchContracts()
  fetchUsers()
  startContractRefresh() // 启动定时刷新
})

onUnmounted(() => {
  stopContractRefresh() // 清理定时器
})
</script>

<style scoped>
.admin {
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

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.stat {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.stat:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-title {
  color: #909399;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1;
  letter-spacing: -0.02em;
}

.panel {
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e4e7ed;
  min-height: 600px;
  overflow-x: auto;
}

/* Tabs Customization */
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border);
}

:deep(.el-tabs__item) {
  font-size: 16px;
  height: 50px;
  line-height: 50px;
  font-weight: 500;
  color: var(--text-light);
}

:deep(.el-tabs__item.is-active) {
  font-weight: 600;
  color: var(--accent);
}

/* Table Styles */
:deep(.el-table) {
  --el-table-header-bg-color: #f8fafc;
  --el-table-border-color: var(--border);
  --el-table-row-hover-bg-color: #f1f5f9;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f8fafc;
  font-weight: 600;
  color: var(--text-light);
  height: 48px;
}

:deep(.el-tag) {
  border-radius: 6px;
  font-weight: 600;
}

.price {
  font-weight: 600;
  color: var(--text);
  font-family: var(--font-main);
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .admin {
    padding: 0 16px;
  }
  
  .header {
    padding: 20px 24px;
  }
  
  .title {
    font-size: 20px;
  }
  
  .stats {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .admin {
    margin: 20px auto 40px;
    padding: 0 12px;
  }
  
  .header {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  
  .title {
    font-size: 18px;
  }
  
  .sub {
    font-size: 13px;
  }
  
  .stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  .stat {
    padding: 16px;
  }
  
  .panel {
    padding: 16px;
  }
}
</style>
