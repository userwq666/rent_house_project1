<template>
  <div class="staff">
    <div class="header glass">
      <div>
        <div class="title">业务后台</div>
        <div class="sub">房源、合同待办统一控制台</div>
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
      <el-tabs v-model="activeTab" @tab-click="loadData">
        <el-tab-pane label="房源待办" name="houses" />
        <el-tab-pane label="合同待办" name="contracts" />
      </el-tabs>

      <el-table v-if="activeTab === 'houses'" :data="pendingHouses" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="ownerName" label="房主" width="120" />
        <el-table-column prop="district" label="区域" width="120" />
        <el-table-column prop="rentPrice" label="租金" width="120">
          <template #default="{ row }"><span class="price">¥{{ row.rentPrice }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-cell action-inline">
              <el-button type="primary" size="small" @click="approveHouse(row.id)">通过</el-button>
              <el-button type="danger" size="small" @click="rejectHouse(row.id)">拒绝</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="myContracts" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="合同ID" width="100" />
        <el-table-column prop="houseTitle" label="房源" min-width="200" />
        <el-table-column prop="landlordName" label="房主" width="120" />
        <el-table-column prop="tenantName" label="租客" width="120" />
        <el-table-column prop="status" label="状态" width="170">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button
                v-if="row.status === 'PENDING_STAFF_SIGNING'"
                type="primary"
                size="small"
                class="full-btn"
                @click="uploadContract(row.id)"
              >
                上传签约合同
              </el-button>
              <el-button
                v-if="(row.status === 'TERMINATION_PENDING_STAFF_REVIEW' || row.status === 'TERMINATION_FORCE_PENDING_JOINT_REVIEW') && row.terminationRequestId"
                type="success"
                size="small"
                class="full-btn"
                @click="decideTermination(row.terminationRequestId, true)"
              >
                同意终止
              </el-button>
              <el-button
                v-if="(row.status === 'TERMINATION_PENDING_STAFF_REVIEW' || row.status === 'TERMINATION_FORCE_PENDING_JOINT_REVIEW') && row.terminationRequestId"
                type="danger"
                size="small"
                class="full-btn"
                @click="decideTermination(row.terminationRequestId, false)"
              >
                驳回终止
              </el-button>
              <div class="link-row">
                <el-button
                  type="primary"
                  text
                  size="small"
                  @click="openChat(row.landlordId, row.landlordName)"
                >
                  联系房主
                </el-button>
                <el-button
                  type="primary"
                  text
                  size="small"
                  @click="openChat(row.tenantId, row.tenantName)"
                >
                  联系租客
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && activeTab === 'houses' && pendingHouses.length === 0" description="暂无房源待办" />
      <el-empty v-if="!loading && activeTab === 'contracts' && myContracts.length === 0" description="暂无合同待办" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStaffPendingHouses, approveHouseByStaff, rejectHouseByStaff } from '../api/house'
import { getMyStaffContracts, uploadSignedContract, respondTermination } from '../api/contract'

const router = useRouter()
const activeTab = ref('houses')
const loading = ref(false)
const pendingHouses = ref([])
const myContracts = ref([])

const statItems = computed(() => [
  { title: '房源待办', value: pendingHouses.value.length },
  { title: '合同待办', value: myContracts.value.length },
  { title: '终止待审', value: myContracts.value.filter(c => c.status === 'TERMINATION_PENDING_STAFF_REVIEW' || c.status === 'TERMINATION_FORCE_PENDING_JOINT_REVIEW').length }
])

const statusText = (status) => {
  const map = {
    PENDING_STAFF_SIGNING: '待业务员签约',
    PENDING_ADMIN_APPROVAL: '待管理员审核',
    TERMINATION_PENDING_STAFF_REVIEW: '待业务员终止审核',
    TERMINATION_FORCE_PENDING_JOINT_REVIEW: '强制终止待联合审核',
    ACTIVE: '进行中',
    TERMINATED: '已终止',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const statusType = (status) => {
  const map = {
    PENDING_STAFF_SIGNING: 'warning',
    PENDING_ADMIN_APPROVAL: 'warning',
    TERMINATION_PENDING_STAFF_REVIEW: 'warning',
    TERMINATION_FORCE_PENDING_JOINT_REVIEW: 'warning',
    ACTIVE: 'success',
    TERMINATED: 'danger',
    REJECTED: 'danger'
  }
  return map[status] || ''
}

const loadData = async () => {
  try {
    loading.value = true
    if (activeTab.value === 'houses') {
      const { data } = await getStaffPendingHouses()
      pendingHouses.value = data || []
    } else {
      const { data } = await getMyStaffContracts()
      myContracts.value = data || []
    }
  } catch (error) {
    ElMessage.error(error.response?.data || '加载失败')
  } finally {
    loading.value = false
  }
}

const approveHouse = async (id) => {
  try {
    await approveHouseByStaff(id)
    ElMessage.success('已通过房源审核')
    loadData()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
  }
}

const rejectHouse = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回房源', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请填写原因'
    })
    await rejectHouseByStaff(id, value)
    ElMessage.success('已驳回房源')
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data || '操作失败')
  }
}

const uploadContract = async (contractId) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf,.jpg,.jpeg,.png'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    try {
      await uploadSignedContract(contractId, file)
      ElMessage.success('上传成功')
      loadData()
    } catch (error) {
      ElMessage.error(error.response?.data || '上传失败')
    }
  }
  input.click()
}

const decideTermination = async (requestId, approve) => {
  try {
    const { value } = await ElMessageBox.prompt(
      approve ? '请输入处理说明（强制终止时将作为后续方案）' : '请输入驳回原因',
      approve ? '同意终止' : '驳回终止',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPattern: /.+/,
        inputErrorMessage: '请填写说明'
      }
    )
    await respondTermination(requestId, { approve, comment: value })
    ElMessage.success(approve ? '已同意终止' : '已驳回终止')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '操作失败')
    }
  }
}

const openChat = (receiverId, receiverName) => {
  if (!receiverId) {
    ElMessage.warning('缺少联系人信息')
    return
  }
  router.push({ path: '/messages', query: { receiverId, receiverName } })
}

onMounted(loadData)
</script>

<style scoped>
.staff {
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
}

.stat-title {
  color: #909399;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
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

.price {
  color: var(--text);
  font-weight: 600;
}

.action-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.action-inline {
  flex-direction: row;
  align-items: center;
}

.full-btn {
  width: 100%;
}

.link-row {
  display: flex;
  gap: 12px;
}

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

@media (max-width: 1200px) {
  .staff {
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
  .staff {
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
