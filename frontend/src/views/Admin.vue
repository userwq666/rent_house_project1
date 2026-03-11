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
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="ownerName" label="房东" width="140" />
            <el-table-column prop="district" label="区域" width="120" />
            <el-table-column prop="rentPrice" label="租金" width="120">
              <template #default="{ row }">
                <span class="price">¥{{ row.rentPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="getHouseStatusType(row.status)">{{ getHouseStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="100" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text @click="viewHouse(row.id)">查看</el-button>
                <el-button
                  v-if="row.status !== 'RENTED'"
                  size="small"
                  type="danger"
                  @click="deleteHouseAdmin(row.id)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="合同" name="contracts">
          <el-table :data="contracts" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="houseTitle" label="房源" min-width="220" />
            <el-table-column prop="landlordName" label="房东" width="120" />
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
            <el-table-column prop="status" label="状态" width="170">
              <template #default="{ row }">
                <el-tag :type="getContractStatusType(row.status)">{{ getContractStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="330" fixed="right">
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
                    驳回
                  </el-button>
                  <el-button
                    v-if="row.status === 'ACTIVE'"
                    size="small"
                    type="danger"
                    @click="terminateContractAdmin(row.id)"
                  >
                    强制终止
                  </el-button>
                  <el-button
                    v-if="row.status === 'TERMINATION_FORCE_PENDING_JOINT_REVIEW' && row.terminationRequestId"
                    size="small"
                    type="primary"
                    @click="decideForceTerminationByAdmin(row, true)"
                  >
                    强制终止通过
                  </el-button>
                  <el-button
                    v-if="row.status === 'TERMINATION_FORCE_PENDING_JOINT_REVIEW' && row.terminationRequestId"
                    size="small"
                    type="warning"
                    @click="decideForceTerminationByAdmin(row, false)"
                  >
                    强制终止驳回
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="用户" name="users">
          <el-table :data="users" v-loading="loading" style="width: 100%">
            <el-table-column prop="userId" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="180" />
            <el-table-column prop="realName" label="姓名" width="140" />
            <el-table-column prop="phone" label="手机号" width="160" />
            <el-table-column prop="email" label="邮箱" min-width="200" />
            <el-table-column prop="accountType" label="角色" width="110">
              <template #default="{ row }">
                <el-tag>{{ row.accountType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="允许发布" width="140">
              <template #default="{ row }">
                <el-switch
                  v-model="row.canPublish"
                  active-text="允许"
                  inactive-text="禁用"
                  @change="value => handleRestrictionChange(row, 'canPublish', value)"
                />
              </template>
            </el-table-column>
            <el-table-column label="允许租房" width="140">
              <template #default="{ row }">
                <el-switch
                  v-model="row.canRent"
                  active-text="允许"
                  inactive-text="禁用"
                  @change="value => handleRestrictionChange(row, 'canRent', value)"
                />
              </template>
            </el-table-column>
            <el-table-column label="账号状态" width="170">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  active-text="启用"
                  inactive-text="锁定"
                  @change="value => handleStatusChange(row, value)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="190">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="业务员" name="staff">
          <div class="staff-entry">
            <div class="staff-entry-title">新增业务员</div>
            <div class="staff-entry-sub">跳转到统一注册流程，直接创建 `STAFF` 账号</div>
            <el-button class="staff-entry-btn glow" size="large" @click="goCreateStaff">
              + 新增业务员
            </el-button>
          </div>

          <el-table :data="staffList" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="180" />
            <el-table-column prop="displayName" label="姓名" width="180" />
            <el-table-column prop="phone" label="手机号" width="180" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag type="warning">{{ row.role || 'STAFF' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="170">
              <template #default="{ row }">
                <el-switch
                  v-model="row.enabled"
                  active-text="启用"
                  inactive-text="停用"
                  @change="value => handleStaffStatusChange(row, value)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="190">
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
import {
  getAllContracts,
  adminTerminateContract,
  approveContractByAdmin,
  rejectContractByAdmin,
  respondTermination
} from '../api/contract'
import { fetchAllUsers, updateUserRestrictions, updateUserStatus, fetchStaffList, updateStaffStatus } from '../api/admin'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('houses')
const houses = ref([])
const contracts = ref([])
const users = ref([])
const staffList = ref([])
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
    houses.value = data || []
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
    contracts.value = data || []
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
    users.value = data || []
  } catch (error) {
    ElMessage.error(error.response?.data || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStaffs = async () => {
  try {
    loading.value = true
    const { data } = await fetchStaffList()
    staffList.value = data || []
  } catch (error) {
    ElMessage.error(error.response?.data || '获取业务员列表失败')
  } finally {
    loading.value = false
  }
}

const updateStats = () => {
  stats.totalHouses = houses.value.length
  stats.availableHouses = houses.value.filter(item => item.status === 'AVAILABLE').length
  stats.rentedHouses = houses.value.filter(item => item.status === 'RENTED').length
  stats.activeContracts = contracts.value.filter(item => item.status === 'ACTIVE').length
}

const handleTabClick = () => {
  if (activeTab.value === 'houses') fetchHouses()
  else if (activeTab.value === 'contracts') fetchContracts()
  else if (activeTab.value === 'users') fetchUsers()
  else if (activeTab.value === 'staff') fetchStaffs()
}

const startContractRefresh = () => {
  if (contractRefreshInterval) clearInterval(contractRefreshInterval)
  contractRefreshInterval = setInterval(() => {
    if (activeTab.value === 'contracts') fetchContracts()
  }, 5000)
}

const stopContractRefresh = () => {
  if (!contractRefreshInterval) return
  clearInterval(contractRefreshInterval)
  contractRefreshInterval = null
}

const viewHouse = (id) => {
  router.push(`/house/${id}`)
}

const goCreateStaff = () => {
  router.push('/register?accountType=STAFF&fromAdmin=1')
}

const deleteHouseAdmin = async (id) => {
  try {
    await ElMessageBox.confirm('管理员删除后不可恢复，确认删除该房源吗？', '警告', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteHouse(id)
    ElMessage.success('删除成功')
    fetchHouses()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data || '删除失败')
  }
}

const terminateContractAdmin = async (id) => {
  try {
    await ElMessageBox.confirm('确认强制终止该合同吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminTerminateContract(id)
    ElMessage.success('合同已终止')
    fetchContracts()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data || '操作失败')
  }
}

const approveContract = async (id) => {
  try {
    await ElMessageBox.confirm('确认通过该合同吗？', '提示', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await approveContractByAdmin(id)
    ElMessage.success('合同已生效')
    fetchContracts()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data || '操作失败')
  }
}

const rejectContract = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回合同', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      type: 'warning',
      inputPattern: /.+/,
      inputErrorMessage: '请填写驳回原因'
    })
    await rejectContractByAdmin(id, { reason: value })
    ElMessage.success('合同已驳回')
    fetchContracts()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data || '操作失败')
  }
}

const decideForceTerminationByAdmin = async (row, approve) => {
  try {
    const { value } = await ElMessageBox.prompt(
      approve ? '请填写管理员裁决说明' : '请填写驳回原因',
      approve ? '通过强制终止' : '驳回强制终止',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPattern: /.+/,
        inputErrorMessage: '请填写说明'
      }
    )
    await respondTermination(row.terminationRequestId, { approve, comment: value })
    ElMessage.success(approve ? '已提交通过意见' : '已驳回强制终止')
    fetchContracts()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data || '操作失败')
  }
}

const handleRestrictionChange = async (row, field, value) => {
  try {
    await updateUserRestrictions(row.userId, {
      canPublish: row.canPublish,
      canRent: row.canRent
    })
    ElMessage.success('权限已更新')
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

const handleStaffStatusChange = async (row, value) => {
  try {
    await updateStaffStatus(row.id, { enabled: row.enabled })
    ElMessage.success(row.enabled ? '业务员已启用' : '业务员已停用')
  } catch (error) {
    ElMessage.error(error.response?.data || '更新业务员状态失败')
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
  const map = {
    AVAILABLE: 'success',
    PENDING: 'warning',
    PENDING_STAFF_REVIEW: 'warning',
    RENTED: 'info',
    OFFLINE: 'danger'
  }
  return map[status] || ''
}

const getHouseStatusText = (status) => {
  const map = {
    AVAILABLE: '可租',
    PENDING: '处理中',
    PENDING_STAFF_REVIEW: '待业务员审核',
    RENTED: '已租',
    OFFLINE: '下架'
  }
  return map[status] || status
}

const getContractStatusType = (status) => {
  const map = {
    ACTIVE: 'success',
    EXPIRED: 'info',
    TERMINATED: 'danger',
    REJECTED: 'danger',
    TERMINATION_PENDING: 'warning',
    TERMINATION_PENDING_COUNTERPARTY: 'warning',
    TERMINATION_PENDING_STAFF_REVIEW: 'warning',
    TERMINATION_FORCE_PENDING_JOINT_REVIEW: 'warning',
    PENDING_LANDLORD_APPROVAL: 'warning',
    PENDING_STAFF_SIGNING: 'warning',
    PENDING_ADMIN_APPROVAL: 'warning'
  }
  return map[status] || ''
}

const getContractStatusText = (status) => {
  const map = {
    ACTIVE: '进行中',
    EXPIRED: '已到期',
    TERMINATED: '已终止',
    REJECTED: '已拒绝',
    TERMINATION_PENDING: '终止处理中',
    TERMINATION_PENDING_COUNTERPARTY: '待对方确认终止',
    TERMINATION_PENDING_STAFF_REVIEW: '待业务员终止审核',
    TERMINATION_FORCE_PENDING_JOINT_REVIEW: '强制终止联合审核中',
    PENDING_LANDLORD_APPROVAL: '待房东审批',
    PENDING_STAFF_SIGNING: '待业务员签约',
    PENDING_ADMIN_APPROVAL: '待管理员审批'
  }
  return map[status] || status
}

onMounted(() => {
  fetchHouses()
  fetchContracts()
  fetchUsers()
  fetchStaffs()
  startContractRefresh()
})

onUnmounted(() => {
  stopContractRefresh()
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
  margin-bottom: 28px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
}

.sub {
  color: #909399;
  font-size: 14px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 28px;
}

.stat {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.stat-title {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1;
}

.panel {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  min-height: 620px;
}

.price {
  font-weight: 700;
}

.action-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.staff-entry {
  border: 1px dashed #dcdfe6;
  border-radius: 16px;
  padding: 28px 20px;
  margin-bottom: 20px;
  text-align: center;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  animation: entryFade 0.25s ease;
}

.staff-entry-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.staff-entry-sub {
  color: #909399;
  margin-top: 8px;
  margin-bottom: 20px;
}

.staff-entry-btn {
  min-width: 260px;
  height: 52px;
  font-size: 18px;
  font-weight: 700;
  border-radius: 12px;
}

@keyframes entryFade {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .admin {
    padding: 0 12px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .title {
    font-size: 22px;
  }

  .panel {
    padding: 14px;
  }

  .staff-entry-btn {
    min-width: 100%;
  }
}
</style>
