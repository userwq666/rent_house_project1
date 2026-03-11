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
        <el-tab-pane :label="isStaff ? '分配给我的合同' : '全部合同'" name="all" />
        <el-tab-pane v-if="!isStaff" label="作为房主" name="landlord" />
        <el-tab-pane v-if="!isStaff" label="作为租客" name="tenant" />
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
            <el-tag :type="getUserRoleLabel(row) === '房主' ? 'success' : 'primary'">
              {{ getUserRoleLabel(row) }}
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
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button
                v-if="!isStaff && row.status === 'PENDING_LANDLORD_APPROVAL'"
                size="small"
                type="primary"
                class="full-btn"
                @click="openApproveDialog(row.id)"
              >
                同意合同
              </el-button>
              <el-button
                v-else-if="isStaff && row.status === 'PENDING_STAFF_SIGNING'"
                size="small"
                type="primary"
                class="full-btn"
                @click="handleUploadSignedFile(row)"
              >
                上传签约合同
              </el-button>
              <el-button
                v-else-if="!isStaff && row.status === 'ACTIVE'"
                size="small"
                type="danger"
                class="full-btn"
                @click="openTerminate(row)"
              >
                终止
              </el-button>
              <div
                v-else-if="!isStaff && row.status === 'TERMINATION_PENDING_COUNTERPARTY' && row.terminationResponderId === currentUserId"
                class="action-cell"
              >
                <el-button size="small" type="success" class="full-btn" @click="counterpartyDecision(row, true)">
                  同意终止
                </el-button>
                <el-button size="small" type="danger" class="full-btn" @click="counterpartyDecision(row, false)">
                  拒绝终止
                </el-button>
              </div>
              <el-tag
                v-else-if="row.status === 'TERMINATION_PENDING' || row.status === 'TERMINATION_PENDING_STAFF_REVIEW'"
                type="warning"
              >
                审核中
              </el-tag>
              <el-tag v-else-if="row.status === 'TERMINATION_FORCE_PENDING_JOINT_REVIEW'" type="warning">
                强制终止联合审核中
              </el-tag>
              <div class="link-row">
                <el-button v-if="!isStaff" size="small" text type="primary" class="link-btn" @click="quickChat('user', row)">
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
          <el-switch v-model="terminateDialog.form.force" :disabled="!canApplyForceTerminate(terminateDialog.contractRow)" />
          <div v-if="terminateDialog.contractRow" class="staff-tip">
            当前普通终止被拒次数：{{ getMyRejectCount(terminateDialog.contractRow) }}（达到 3 次可申请强制终止）
          </div>
        </el-form-item>
        <el-form-item label="说明" v-if="terminateDialog.form.force">
          <el-input v-model="terminateDialog.form.forceReason" placeholder="请输入强制终止说明" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="证据附件" v-if="terminateDialog.form.force">
          <el-input
            v-model="terminateDialog.form.evidenceUrls"
            placeholder="请输入证据附件URL或说明（可多条，逗号分隔）"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="terminateDialog.visible = false">取消</el-button>
        <el-button type="primary" class="glow" :loading="terminateDialog.loading" @click="submitTerminate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="staffAssignDialog.visible" title="指定签约业务员" width="420px">
      <div class="staff-tip">可手动指定业务员，不选择则系统随机分配。</div>
      <el-select v-model="staffAssignDialog.selectedStaffId" clearable style="width: 100%" placeholder="不指定（随机分配）">
        <el-option
          v-for="item in staffAssignDialog.options"
          :key="item.id"
          :label="formatStaffOption(item)"
          :value="item.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="staffAssignDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="staffAssignDialog.loading" @click="submitApproveByLandlord">确定</el-button>
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
  getMyStaffContracts,
  uploadSignedContract,
  terminateContract,
  respondTerminationByCounterparty,
  approveContractByLandlord,
  approveContractByLandlordWithStaff,
  getAvailableStaffOptions
} from '../api/contract'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('all')
const allContracts = ref([])
const landlordContracts = ref([])
const tenantContracts = ref([])
const currentUserId = Number(sessionStorage.getItem('accountId') || sessionStorage.getItem('userId'))
const userType = sessionStorage.getItem('accountType') || sessionStorage.getItem('userType')
const isStaff = userType === 'STAFF'

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
  contractRow: null,
  loading: false,
  form: {
    reason: '',
    force: false,
    forceReason: '',
    evidenceUrls: ''
  }
})

const staffAssignDialog = reactive({
  visible: false,
  contractId: null,
  selectedStaffId: null,
  loading: false,
  options: []
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
const getUserRoleLabel = (contract) => {
  if (isStaff) return '业务员'
  return getUserRole(contract)
}

const getOtherPartyName = (contract) => (contract.landlordId === currentUserId ? contract.tenantName : contract.landlordName)

const getOtherPartyPhone = (contract) => (contract.landlordId === currentUserId ? contract.tenantPhone : contract.landlordPhone)

const getStatusType = (status) => {
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

const getStatusText = (status) => {
  const map = {
    ACTIVE: '进行中',
    EXPIRED: '已到期',
    TERMINATED: '已终止',
    REJECTED: '已拒绝',
    TERMINATION_PENDING: '待终止确认',
    TERMINATION_PENDING_COUNTERPARTY: '待对方确认终止',
    TERMINATION_PENDING_STAFF_REVIEW: '待业务员终止审核',
    TERMINATION_FORCE_PENDING_JOINT_REVIEW: '强制终止待联合审核',
    PENDING_LANDLORD_APPROVAL: '待房主审批',
    PENDING_STAFF_SIGNING: '待业务员签约',
    PENDING_ADMIN_APPROVAL: '待管理员审批'
  }
  return map[status] || status
}

const fetchContracts = async () => {
  try {
    loading.value = true
    if (isStaff) {
      const res = await getMyStaffContracts()
      allContracts.value = res.data || []
      return
    }
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
  terminateDialog.contractRow = row
  terminateDialog.form.reason = ''
  terminateDialog.form.force = false
  terminateDialog.form.forceReason = ''
  terminateDialog.form.evidenceUrls = ''
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
  if (terminateDialog.form.force && !terminateDialog.form.evidenceUrls) {
    ElMessage.warning('请填写证据附件（URL 或文件说明）')
    return
  }
  try {
    terminateDialog.loading = true
    await terminateContract(terminateDialog.contractId, {
      reason: terminateDialog.form.reason,
      force: terminateDialog.form.force,
      forceReason: terminateDialog.form.forceReason,
      evidenceUrls: terminateDialog.form.evidenceUrls
    })
    ElMessage.success(terminateDialog.form.force ? '强制终止申请已提交' : '终止申请已发送')
    terminateDialog.visible = false
    fetchContracts()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
  } finally {
    terminateDialog.loading = false
  }
}

const getMyRejectCount = (row) => {
  if (!row) return 0
  if (row.landlordId === currentUserId) return row.landlordTerminationRejectCount || 0
  if (row.tenantId === currentUserId) return row.tenantTerminationRejectCount || 0
  return 0
}

const canApplyForceTerminate = (row) => getMyRejectCount(row) >= 3

const counterpartyDecision = async (row, approve) => {
  if (!row?.terminationRequestId) {
    ElMessage.warning('终止申请记录不存在')
    return
  }
  const comment = approve ? '同意终止' : '不同意终止'
  try {
    await respondTerminationByCounterparty(row.terminationRequestId, { approve, comment })
    ElMessage.success(approve ? '已同意终止，进入业务员审核' : '已拒绝终止')
    fetchContracts()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
  }
}

const formatStaffOption = (staff) => {
  const name = staff.displayName || `业务员#${staff.id}`
  return staff.phone ? `${name}（${staff.phone}）` : name
}

const openApproveDialog = async (contractId) => {
  try {
    staffAssignDialog.loading = true
    const { data } = await getAvailableStaffOptions()
    staffAssignDialog.options = data || []
    staffAssignDialog.contractId = contractId
    staffAssignDialog.selectedStaffId = null
    staffAssignDialog.visible = true
  } catch (error) {
    ElMessage.error(error.response?.data || '获取业务员列表失败')
  } finally {
    staffAssignDialog.loading = false
  }
}

const submitApproveByLandlord = async () => {
  if (!staffAssignDialog.contractId) {
    return
  }
  try {
    staffAssignDialog.loading = true
    if (staffAssignDialog.selectedStaffId) {
      await approveContractByLandlordWithStaff(staffAssignDialog.contractId, staffAssignDialog.selectedStaffId)
      ElMessage.success('已指定业务员并进入签约流程')
    } else {
      await approveContractByLandlord(staffAssignDialog.contractId)
      ElMessage.success('已随机分配业务员并进入签约流程')
    }
    staffAssignDialog.visible = false
    fetchContracts()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
  } finally {
    staffAssignDialog.loading = false
  }
}

const handleUploadSignedFile = async (row) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf,.jpg,.jpeg,.png'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    try {
      await uploadSignedContract(row.id, file)
      ElMessage.success('上传成功，已提交管理员审核')
      fetchContracts()
    } catch (error) {
      ElMessage.error(error.response?.data || '上传失败')
    }
  }
  input.click()
}

const quickChat = (type, row) => {
  if (!row) return
  if (type === 'user') {
    const receiverId = row.landlordId === currentUserId ? row.tenantId : row.landlordId
    const receiverName = row.landlordId === currentUserId ? row.tenantName : row.landlordName
    if (receiverId) {
      router.push({ path: '/messages', query: { receiverId, receiverName } })
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

.staff-tip {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
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
