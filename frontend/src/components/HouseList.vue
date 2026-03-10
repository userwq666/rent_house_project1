<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openAddDialog" :icon="Plus">新增房屋</el-button>
      <el-button type="danger" @click="handleClearAll" :icon="Delete">清空所有</el-button>
    </div>

    <el-table :data="houses" style="width: 100%" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="name" label="房主姓名" width="120" />
      <el-table-column prop="numIphone" label="电话号码" width="150" />
      <el-table-column prop="location" label="地址" show-overflow-tooltip />
      <el-table-column prop="moneyRent" label="月租" width="120">
        <template #default="scope">
          ¥{{ scope.row.moneyRent }}
        </template>
      </el-table-column>
      <el-table-column prop="houseType" label="户型" width="100" />
      <el-table-column prop="state" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.state === '已出租' ? 'success' : 'warning'">
            {{ scope.row.state }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template #default="scope">
          <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="房主姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="电话号码" prop="numIphone">
          <el-input v-model="form.numIphone" />
        </el-form-item>
        <el-form-item label="地址" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="月租" prop="moneyRent">
          <el-input-number v-model="form.moneyRent" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="户型" prop="houseType">
          <el-input v-model="form.houseType" placeholder="例如: 两室一厅" />
        </el-form-item>
        <el-form-item label="状态" prop="state">
          <el-select v-model="form.state">
            <el-option label="未出租" value="未出租" />
            <el-option label="已出租" value="已出租" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import axios from '../utils/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const houses = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增房屋')
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  numIphone: '',
  location: '',
  moneyRent: 0,
  houseType: '',
  state: '未出租'
})

const rules = {
  name: [{ required: true, message: '请输入房主姓名', trigger: 'blur' }],
  numIphone: [{ required: true, message: '请输入电话号码', trigger: 'blur' }],
  location: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  moneyRent: [{ required: true, message: '请输入月租', trigger: 'blur' }],
  houseType: [{ required: true, message: '请输入户型', trigger: 'blur' }],
  state: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const fetchHouses = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/houses')
    houses.value = response.data
  } catch (error) {
    ElMessage.error('获取房屋列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  dialogTitle.value = '新增房屋'
  isEdit.value = false
  form.id = null
  form.name = ''
  form.numIphone = ''
  form.location = ''
  form.moneyRent = 0
  form.houseType = ''
  form.state = '未出租'
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogTitle.value = '编辑房屋'
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await axios.put(`/api/houses/${form.id}`, form)
          ElMessage.success('修改成功')
        } else {
          await axios.post('/api/houses', form)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        fetchHouses()
      } catch (error) {
        ElMessage.error('操作失败')
        console.error(error)
      }
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除该房屋信息吗?',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      try {
        await axios.delete(`/api/houses/${row.id}`)
        ElMessage.success('删除成功')
        fetchHouses()
      } catch (error) {
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

const handleClearAll = () => {
    ElMessageBox.confirm(
    '确定要清空所有房屋信息吗? 此操作不可恢复!',
    '严重警告',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'error',
    }
  )
    .then(async () => {
      try {
        await axios.delete('/api/houses')
        ElMessage.success('已清空所有数据')
        fetchHouses()
      } catch (error) {
        ElMessage.error('清空失败')
      }
    })
    .catch(() => {})
}

onMounted(() => {
  fetchHouses()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>
