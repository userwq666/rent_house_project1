<template>
  <div class="message-action-card" :class="`message-${type}`">
    <div class="message-content">
      <h4 class="message-title">{{ title }}</h4>
      <p class="message-description">{{ content }}</p>

      <div v-if="type === 'TERMINATION_REQUEST'" class="message-type-tag">
        <el-tag type="warning">退租申请</el-tag>
      </div>
      <div v-else-if="type === 'TERMINATION_RESPONSE'" class="message-type-tag">
        <el-tag type="warning">退租回复</el-tag>
      </div>
      <div v-else-if="type === 'FORCE_TERMINATION_NOTICE'" class="message-type-tag">
        <el-tag type="danger">强制终止通知</el-tag>
      </div>

      <div class="message-meta">
        <span class="message-time">{{ formattedTime }}</span>
        <span v-if="contractId" class="contract-id">合同编号: {{ contractId }}</span>
      </div>
    </div>

    <!-- 需要操作 且 当前状态允许操作 -->
    <div
      v-if="requireAction && canAct"
      class="action-buttons"
    >
      <el-button
        type="success"
        size="small"
        @click="handleAction('ACCEPT')"
        :loading="loading === 'ACCEPT'"
      >
        同意
      </el-button>
      <el-button
        type="danger"
        size="small"
        @click="handleAction('REJECT')"
        :loading="loading === 'REJECT'"
      >
        拒绝
      </el-button>
    </div>

    <div v-else-if="status === 'ACCEPT' || status === 'REJECT'" class="action-result">
      <el-tag :type="status === 'ACCEPT' ? 'success' : 'danger'">
        {{ status === 'ACCEPT' ? '已同意' : '已拒绝' }}
      </el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { format } from 'date-fns'

const props = defineProps({
  id: { type: Number, required: true },
  title: { type: String, required: true },
  content: { type: String, required: true },
  type: {
    type: String,
    default: 'USER_CHAT'
    // 'TERMINATION_REQUEST', 'TERMINATION_RESPONSE', 'FORCE_TERMINATION_NOTICE', 'ADMIN_NOTIFICATION', 'USER_CHAT'
  },
  status: {
    type: String,
    default: 'UNREAD'
    // 'UNREAD', 'READ', 'PENDING', 'ACCEPT', 'REJECT'
  },
  createdAt: { type: String, required: true },
  contractId: { type: Number, default: null },
  requireAction: { type: Boolean, default: false }
})

const emit = defineEmits(['action'])
const loading = ref(null)

const formattedTime = computed(() => {
  const d = new Date(props.createdAt)
  return isNaN(d.getTime()) ? '-' : format(d, 'yyyy-MM-dd HH:mm')
})

const canAct = computed(() =>
  props.status === 'PENDING' ||
  props.status === 'UNREAD' ||
  props.status === 'READ'
)

const handleAction = (action) => {
  if (loading.value) return
  loading.value = action

  try {
    emit('action', { messageId: props.id, action })
    // 子组件只负责提示；真正状态更新由父组件改 props.status
    ElMessage.success(action === 'ACCEPT' ? '已同意' : '已拒绝')
  } catch (error) {
    ElMessage.error('操作失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value = null
  }
}
</script>

<style scoped>
.message-action-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background-color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.3s ease;
}

.message-action-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.message-TERMINATION_REQUEST { border-left: 4px solid #409eff; }
.message-TERMINATION_RESPONSE { border-left: 4px solid #67c23a; }
.message-FORCE_TERMINATION_NOTICE { border-left: 4px solid #e6a23c; }
.message-ADMIN_NOTIFICATION { border-left: 4px solid #909399; }

.message-content { margin-bottom: 12px; }
.message-type-tag { margin-top: 8px; }

.message-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.message-description {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--text-light);
  line-height: 1.5;
}

.message-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-light);
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

</style>