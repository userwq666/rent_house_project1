<template>
  <div class="publish">
    <div class="header glass">
      <div>
        <div class="title">发布房源</div>
        <div class="sub">填写基础信息，几步即可上线</div>
      </div>
      <el-button text @click="$router.push('/home')">返回首页</el-button>
    </div>

    <div class="form-shell glass">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="房源标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入房源标题" />
        </el-form-item>

        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>

        <el-form-item label="所属区域" prop="district">
          <el-input v-model="form.district" placeholder="如：朝阳区 / 浦东新区" />
        </el-form-item>

        <el-form-item label="户型" prop="houseType">
          <el-select v-model="form.houseType" placeholder="请选择户型" style="width: 100%">
            <el-option label="一居室" value="1居" />
            <el-option label="一室一厅" value="1室1厅" />
            <el-option label="两室一厅" value="2室1厅" />
            <el-option label="三室一厅" value="3室1厅" />
            <el-option label="三室两厅" value="3室2厅" />
            <el-option label="四室两厅" value="4室2厅" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="面积(㎡)" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼层" prop="floor">
              <el-input-number v-model="form.floor" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="月租(元)" prop="rentPrice">
              <el-input-number v-model="form.rentPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="押金(元)" prop="deposit">
              <el-input-number v-model="form.deposit" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="房源描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请描述房源的特点、周边环境、交通等"
          />
        </el-form-item>

        <el-form-item label="配套设施" prop="facilities">
          <el-input
            v-model="form.facilities"
            placeholder="如：空调, 冰箱, 洗衣机, 热水器, 宽带"
          />
        </el-form-item>

        <el-form-item label="房源图片" prop="images">
          <div class="upload-box">
            <input
              ref="fileInput"
              type="file"
              accept="image/png,image/jpeg,image/jpg,image/webp"
              multiple
              @change="handleFileChange"
              class="hidden-input"
            />
            <el-button type="primary" plain @click="triggerSelect">选择图片</el-button>
            <span class="hint">最多4张，单张不超过1MB，支持 png/jpg/webp</span>
          </div>
          <div class="preview-list">
            <div v-for="(img, index) in imageList" :key="index" class="preview-item">
              <img :src="img" alt="house" />
              <el-button type="text" @click="removeImage(index)">移除</el-button>
            </div>
          </div>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" class="glow" @click="handleSubmit" :loading="loading">
            发布房源
          </el-button>
          <el-button text @click="$router.back()">取消</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createHouse } from '../api/house'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const fileInput = ref(null)
const imageList = ref([])

const form = reactive({
  title: '',
  address: '',
  district: '',
  houseType: '',
  area: null,
  floor: null,
  rentPrice: null,
  deposit: null,
  description: '',
  facilities: '',
  // images 保留字符串，提交时拼接 Base64 或未来替换为 URL
  images: ''
})

const rules = {
  title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  district: [{ required: true, message: '请输入所属区域', trigger: 'blur' }],
  houseType: [{ required: true, message: '请选择户型', trigger: 'change' }],
  rentPrice: [{ required: true, message: '请输入月租金', trigger: 'blur' }]
}

const triggerSelect = () => {
  fileInput.value?.click()
}

const handleFileChange = (e) => {
  const files = Array.from(e.target.files || [])
  const remain = 4 - imageList.value.length
  const picked = files.slice(0, remain)
  picked.forEach((file) => {
    if (file.size > 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过1MB，已跳过`)
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      imageList.value.push(reader.result)
    }
    reader.readAsDataURL(file)
  })
  e.target.value = ''
}

const removeImage = (index) => {
  imageList.value.splice(index, 1)
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    // 使用"|"分隔，避免 Base64 内部的逗号被截断
    form.images = imageList.value.join('|')
    
    // 确保数值类型正确
    const submitData = {
      ...form,
      area: Number(form.area) || null,
      floor: Number(form.floor) || null,
      rentPrice: Number(form.rentPrice) || null,
      deposit: Number(form.deposit) || null
    }
    
    await createHouse(submitData)
    ElMessage.success('发布成功')
    router.push('/my-houses')
  } catch (error) {
    if (error?.response) {
      ElMessage.error(error.response.data || '发布失败')
    } else {
      ElMessage.error('发布失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.publish {
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

.form-shell {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e4e7ed;
  min-width: 1000px;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.form-actions .el-button--primary {
  height: 44px;
  padding: 0 32px;
  font-size: 16px;
}

.upload-box {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 12px;
  border: 1px dashed var(--border);
}

.hidden-input {
  display: none;
}

.hint {
  color: var(--muted);
  font-size: 13px;
}

.preview-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.preview-item {
  width: 100%;
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
}

.preview-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.preview-item .el-button {
  width: 100%;
  border-radius: 0;
  padding: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
}

/* Form Styles */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text);
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: 8px;
  padding: 8px 12px;
}

:deep(.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px var(--text-light) inset;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}
</style>
