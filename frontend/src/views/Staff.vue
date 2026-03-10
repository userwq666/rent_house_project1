<template>
  <div class="staff">
    <div class="header glass">
      <div>
        <div class="title">业务后台</div>
        <div class="sub">处理房源审核、签约上传与终止审核</div>
      </div>
      <el-button text @click="$router.push('/home')">返回首页</el-button>
    </div>

    <div class="panel glass">
      <el-tabs v-model="activeTab" @tab-click="loadData">
        <el-tab-pane label="待审核房源" name="houses" />
        <el-tab-pane label="我的合同" name="contracts" />
      </el-tabs>

      <el-table v-if="activeTab === 'houses'" :data="pendingHouses" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="ownerName" label="房主" width="120" />
        <el-table-column prop="district" label="区域" width="120" />
        <el-table-column prop="rentPrice" label="租金" width="120">
          <template #default="{ row }">¥{{ row.rentPrice }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="approveHouse(row.id)">通过</el-button>
            <el-button type="danger" size="small" @click="rejectHouse(row.id)">拒绝</el-button>
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
            <el-tag>{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING_STAFF_SIGNING'"
              type="primary"
              size="small"
              @click="uploadContract(row.id)"
            >
              上传签约合同
            </el-button>
            <el-button
              v-if="row.status === 'TERMINATION_PENDING_STAFF_REVIEW' && row.terminationRequestId"
              type="success"
              size="small"
              @click="decideTermination(row.terminationRequestId, true)"
            >
              同意终止
            </el-button>
            <el-button
              v-if="row.status === 'TERMINATION_PENDING_STAFF_REVIEW' && row.terminationRequestId"
              type="danger"
              size="small"
              @click="decideTermination(row.terminationRequestId, false)"
            >
              驳回终止
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click="openChat(row.landlordId, row.landlordName)"
            >
              联系房主
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click="openChat(row.tenantId, row.tenantName)"
            >
              联系租客
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStaffPendingHouses, approveHouseByStaff, rejectHouseByStaff } from '../api/house'
import { getMyStaffContracts, uploadSignedContract, respondTermination } from '../api/contract'

const router = useRouter()
const activeTab = ref('houses')
const loading = ref(false)
const pendingHouses = ref([])
const myContracts = ref([])

const statusText = (status) => {
  const map = {
    PENDING_STAFF_SIGNING: '待业务员签约',
    PENDING_ADMIN_APPROVAL: '待管理员审核',
    TERMINATION_PENDING_STAFF_REVIEW: '待业务员终止审核',
    ACTIVE: '进行中',
    TERMINATED: '已终止'
  }
  return map[status] || status
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
    await respondTermination(requestId, { approve, comment: approve ? '同意终止' : '不同意终止' })
    ElMessage.success(approve ? '已同意终止' : '已驳回终止')
    loadData()
  } catch (error) {
    ElMessage.error(error.response?.data || '操作失败')
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
.staff { max-width: 1400px; margin: 20px auto; padding: 0 20px; }
.header { display:flex; justify-content:space-between; align-items:center; padding:20px; margin-bottom:20px; }
.title { font-size: 28px; font-weight: 700; }
.sub { color: #909399; }
.panel { padding: 20px; }
</style>
