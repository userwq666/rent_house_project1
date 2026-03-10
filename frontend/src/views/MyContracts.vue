<template>
  <div class="my-contracts">
    <div class="header glass">
      <div>
        <div class="title">我的合同</div>
        <div class="sub">按角色查看，申请终止或退租</div>
      </div>
      <el-button text @click="$router.push('/home')">返回首页</el-button>
    </div>

    <div class="card glass">
      <el-tabs v-model="activeTab" @tab-click="fetchContracts">
        <el-tab-pane label="全部合同" name="all" />
        <el-tab-pane label="作为房主" name="landlord" />
        <el-tab-pane label="作为租客" name="tenant" />
      </el-tabs>

      <el-table :data="currentContracts" v-loading="loading" style="width: 100%">
        <el-table-column prop="houseTitle" label="房源" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click.stop="quickChat('house', row)">{{ row.houseTitle }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="houseAddress" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="我的身份" width="100">
          <template #default="{ row }">
            <el-tag :type="getUserRole(row) === '房主' ? 'success' : 'primary'">
              {{ getUserRole(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对方" width="140">
          <template #default="{ row }">
            {{ getOtherPartyName(row) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="联系方式" width="150">
          <template #default="{ row }">
            {{ getOtherPartyPhone(row) || '未提供' }}
          </template>
        </el-table-column>
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
        <el-table-column prop="status" label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
            <el-tag v-if="row.terminationStatus === 'PENDING'" type="warning" style="margin-left: 6px">
              待确认
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button
                v-if="row.status === 'PENDING_LANDLORD_APPROVAL'"
                size="small"
                type="primary"
                class="full-btn"
                @click="approveByLandlord(row.id)"
              >
                同意合同
              </el-button>
              <el-button
                v-else-if="row.status === 'ACTIVE'"
                size="small"
                type="danger"
                class="full-btn"
                @click="openTerminate(row)"
              >
                终止
              </el-button>
              <el-tag v-else-if="row.status === 'TERMINATION_PENDING'" type="warning">审核中</el-tag>
              <div class="link-row">
                <el-button size="small" text type="primary" class="link-btn" @click="quickChat('user', row)">
                  联系对方
                </el-button>
                <el-button size="small" text class="link-btn" @click="quickChat('house', row)">
                  查看房源
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="currentContracts.length === 0 && !loading" description="暂无合同" />
    </div>

    <el-dialog v-model="terminateDialog.visible" title="申请终止合同">
      <el-form :model="terminateDialog.form" label-width="100px">
        <el-form-item label="原因" v-if="!terminateDialog.form.force">
          <el-input v-model="terminateDialog.form.reason" placeholder="请输入终止原因" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="强制终止">
          <el-switch v-model="terminateDialog.form.force" />
        </el-form-item>
        <el-form-item label="说明" v-if="terminateDialog.form.force">
          <el-input v-model="terminateDialog.form.forceReason" placeholder="请输入强制终止说明" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="terminateDialog.visible = false">取消</el-button>
        <el-button type="primary" class="glow" :loading="terminateDialog.loading" @click="submitTerminate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getMyContracts,
  getLandlordContracts,
  getTenantContracts,
  terminateContract,
  approveContractByLandlord
} from '../api/contract'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('all')
const allContracts = ref([])
const landlordContracts = ref([])
const tenantContracts = ref([])
const currentUserId = Number(sessionStorage.getItem('userId'))
const userType = sessionStorage.getItem('userType')

// 定时刷新合同状态（每 5 秒）
let contractRefreshInterval = null

// 如果是管理员，显示提示信息并返回首页
if (userType === 'ADMIN') {
  ElMessage.warning('管理员不能访问合同管理页面')
  router.push('/home')
}

const terminateDialog = reactive({
  visible: false,
  contractId: null,
  loading: false,
  form: {
    reason: '',
    force: false,
    forceReason: ''
  }
})

const currentContracts = computed(() => {
  if (activeTab.value === 'all') return allContracts.value
  if (activeTab.value === 'landlord') return landlordContracts.value
  return tenantContracts.value
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.split('T')[0]
}

const getUserRole = (contract) => (contract.landlordId === currentUserId ? '房主' : '租客')

const getOtherPartyName = (contract) => (contract.landlordId === currentUserId ? contract.tenantName : contract.landlordName)

const getOtherPartyPhone = (contract) => (contract.landlordId === currentUserId ? contract.tenantPhone : contract.landlordPhone)

const getStatusType = (status) => {
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

const getStatusText = (status) => {
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

const fetchContracts = async () => {
  try {
    loading.value = true
    if (activeTab.value === 'all') {
      const res = await getMyContracts()
      allContracts.value = res.data || []
    } else if (activeTab.value === 'landlord') {
      const res = await getLandlordContracts()
      landlordContracts.value = res.data || []
    } else {
      const res = await getTenantContracts()
      tenantContracts.value = res.data || []
    }
  } catch (error) {
    ElMessage.error(error.response?.data || '获取合同列表失败')
  } finally {
    loading.value = false
  }
}

const openTerminate = (row) => {
  terminateDialog.visible = true
  terminateDialog.contractId = row.id
  terminateDialog.form.reason = ''
  terminateDialog.form.force = false
  terminateDialog.form.forceReason = ''
}

const submitTerminate = async () => {
  if (!terminateDialog.form.force && !terminateDialog.form.reason) {
    ElMessage.warning('请填写终止原因')
    return
  }
  if (terminateDialog.form.force && !terminateDialog.form.forceReason) {
    ElMessage.warning('请填写强制终止说明')
    return
  }
  try {
    terminateDialog.loading = true
    await terminateContract(terminateDialog.contractId, {
      reason: terminateDialog.form.reason,
      force: terminateDialog.form.force,
      forceReason: terminateDialog.form.forceReason
    })
    ElMessage.success(terminateDialog.form.force ? '已强制终止合同' : '终止申请已发送')
    terminateDialog.visible = false
    fetchContracts()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
  } finally {
    terminateDialog.loading = false
  }
}

const approveByLandlord = async (id) => {
  try {
    await approveContractByLandlord(id)
    ElMessage.success('已提交管理员审批')
    fetchContracts()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
  }
}

const quickChat = (type, row) => {
  if (!row) return
  if (type === 'user') {
    const receiverId = row.landlordId === currentUserId ? row.tenantId : row.landlordId
    if (receiverId) {
      router.push({ path: '/messages', query: { receiverId } })
    } else {
      ElMessage.warning('未找到对方信息')
    }
  } else if (type === 'house') {
    router.push(`/house/${row.houseId}`)
  }
}

// 启动合同状态定时刷新
const startContractRefresh = () => {
  if (contractRefreshInterval) {
    clearInterval(contractRefreshInterval)
  }
  contractRefreshInterval = setInterval(() => {
    console.log('定时刷新合同列表...')
    fetchContracts()
  }, 5000) // 每 5 秒刷新一次
}

// 停止合同状态定时刷新
const stopContractRefresh = () => {
  if (contractRefreshInterval) {
    clearInterval(contractRefreshInterval)
    contractRefreshInterval = null
  }
}

fetchContracts()

onMounted(() => {
  startContractRefresh() // 启动定时刷新
})

onUnmounted(() => {
  stopContractRefresh() // 清理定时器
})
</script>

<style scoped>
.my-contracts {
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

.card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e4e7ed;
  min-width: 1000px;
  overflow-x: auto;
}

.price {
  color: var(--text);
  font-weight: 600;
  font-family: var(--font-main);
}

.action-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.full-btn {
  width: 100%;
}

.link-row {
  display: flex;
  gap: 12px;
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

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border);
}

:deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-light);
}

:deep(.el-tabs__item.is-active) {
  color: var(--accent);
  font-weight: 600;
}

:deep(.el-tag) {
  border-radius: 6px;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .my-contracts {
    padding: 0 16px;
  }
  
  .header {
    padding: 20px 24px;
  }
  
  .title {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .my-contracts {
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
  
  .card {
    padding: 16px;
  }
}
</style>
