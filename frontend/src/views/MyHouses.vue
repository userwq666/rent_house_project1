<template>
  <div class="my-houses">
    <div class="header glass">
      <div>
        <div class="title">我的房源</div>
        <div class="sub">管理、上下架、编辑你的发布</div>
      </div>
      <div class="header-actions">
        <el-button text @click="$router.push('/home')">返回首页</el-button>
        <el-button v-if="userType !== 'ADMIN'" type="primary" class="glow" @click="$router.push('/house/publish')">发布新房源</el-button>
      </div>
    </div>

    <div class="list glass">
      <el-table :data="houses" v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="房源标题" min-width="200" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="houseType" label="户型" width="110" />
        <el-table-column prop="rentPrice" label="租金" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ Number(row.rentPrice) || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="90" />
        <el-table-column label="租客" width="140">
          <template #default="{ row }">
            <span v-if="row.status === 'RENTED'">{{ row.currentTenantName || '（未记录）' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="租客电话" width="150">
          <template #default="{ row }">
            <span v-if="row.status === 'RENTED'">{{ row.currentTenantPhone || '（未记录）' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="handleView(row.id)">查看</el-button>
            <el-button size="small" text :disabled="row.status === 'RENTED'" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'AVAILABLE'"
              size="small"
              type="warning"
              @click="handleOffline(row.id)"
            >
              下架
            </el-button>
            <el-button
              v-if="row.status === 'OFFLINE'"
              size="small"
              type="success"
              @click="handleOnline(row.id)"
            >
              上架
            </el-button>
            <el-button
              v-if="row.status !== 'RENTED'"
              size="small"
              type="danger"
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="houses.length === 0 && !loading" description="还没有发布房源" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyHouses, offlineHouse, onlineHouse, deleteHouse } from '../api/house'

const router = useRouter()
const houses = ref([])
const loading = ref(false)
const userType = sessionStorage.getItem('accountType') || sessionStorage.getItem('userType') || ''

// 如果是管理员，在路由层面就已经拦截，这里不再重复检查

const fetchHouses = async () => {
  try {
    loading.value = true
    const { data } = await getMyHouses()
    houses.value = data
  } catch (error) {
    ElMessage.error('获取房源列表失败')
  } finally {
    loading.value = false
  }
}

const handleView = (id) => {
  router.push(`/house/${id}`)
}

const handleEdit = (row) => {
  if (row.status === 'RENTED') {
    ElMessage.warning('房源已出租，不可编辑')
    return
  }
  router.push(`/house/edit/${row.id}`)
}

const handleOffline = async (id) => {
  try {
    await ElMessageBox.confirm('确定要下架此房源吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await offlineHouse(id)
    ElMessage.success('下架成功')
    fetchHouses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data || '下架失败')
    }
  }
}

const handleOnline = async (id) => {
  try {
    await onlineHouse(id)
    ElMessage.success('上架成功')
    fetchHouses()
  } catch (error) {
    ElMessage.error(error.response?.data || '上架失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除此房源吗？此操作不可恢复！', '警告', {
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

const getStatusType = (status) => {
  const map = {
    AVAILABLE: 'success',
    RENTED: 'info',
    OFFLINE: 'warning'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    AVAILABLE: '可租',
    RENTED: '已租',
    OFFLINE: '下架'
  }
  return map[status] || status
}

onMounted(() => {
  fetchHouses()
})
</script>

<style scoped>
.my-houses {
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

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.list {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e4e7ed;
  min-width: 1000px;
}

.price {
  color: var(--text);
  font-weight: 600;
  font-family: var(--font-main);
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

:deep(.el-button--small) {
  padding: 5px 8px;
  font-size: 13px;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .my-houses {
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
  .my-houses {
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
  
  .list {
    padding: 16px;
  }
}
</style>
